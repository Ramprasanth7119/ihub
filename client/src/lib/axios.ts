import axios, { type AxiosError, type InternalAxiosRequestConfig } from "axios";
import { useAuthStore } from "@/store/auth-store";

/**
 * Requests go to Next.js at a relative `/api` path and are proxied to the Spring
 * Boot backend by the rewrites in `next.config.ts`. That keeps the browser
 * same-origin, so no CORS preflight is involved in normal operation.
 */
const api = axios.create({
  baseURL: "/api",
  headers: { "Content-Type": "application/json" },
});

/** Endpoints that must never trigger the refresh-and-retry flow. */
const AUTH_ENDPOINTS = ["/auth/login", "/auth/refresh", "/auth/logout"];

type RetriableRequest = InternalAxiosRequestConfig & { _retried?: boolean };

api.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const token = useAuthStore.getState().accessToken;
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

/** Shared across concurrent 401s so a burst of requests triggers one refresh. */
let refreshPromise: Promise<string | null> | null = null;

api.interceptors.response.use(
  (res) => res,
  async (error: AxiosError) => {
    const original = error.config as RetriableRequest | undefined;

    if (!original || error.response?.status !== 401) {
      return Promise.reject(error);
    }

    // A rejected login is a 401 about the submitted credentials, not an expired
    // session. Refreshing (and logging out on failure) would wipe an unrelated
    // signed-in session and hide the real "wrong password" message.
    const url = original.url ?? "";
    if (AUTH_ENDPOINTS.some((endpoint) => url.includes(endpoint))) {
      return Promise.reject(error);
    }

    // Only ever retry once: if the refreshed token is also rejected, the session
    // is genuinely dead and looping would hammer the API.
    if (original._retried) {
      useAuthStore.getState().logout();
      redirectToLogin();
      return Promise.reject(error);
    }

    const { refreshToken, setTokens, logout } = useAuthStore.getState();
    if (!refreshToken) {
      logout();
      redirectToLogin();
      return Promise.reject(error);
    }

    if (!refreshPromise) {
      refreshPromise = axios
        .post<{ accessToken: string; refreshToken: string; expiresIn: number; role: string }>(
          "/api/auth/refresh",
          { refreshToken }
        )
        .then((res) => {
          setTokens(res.data.accessToken, res.data.refreshToken, res.data.role);
          return res.data.accessToken;
        })
        .catch(() => {
          logout();
          redirectToLogin();
          return null;
        })
        .finally(() => {
          refreshPromise = null;
        });
    }

    const newToken = await refreshPromise;
    if (!newToken) return Promise.reject(error);

    original._retried = true;
    original.headers.Authorization = `Bearer ${newToken}`;
    return api(original);
  }
);

/**
 * Sends the visitor to the login screen, preserving where they were so they land
 * back there afterwards. Skipped when already on an auth page to avoid a loop.
 */
function redirectToLogin() {
  if (typeof window === "undefined") return;

  const { pathname, search } = window.location;
  if (pathname.startsWith("/login") || pathname.startsWith("/register")) return;

  const redirect = encodeURIComponent(`${pathname}${search}`);
  window.location.href = `/login?redirect=${redirect}&reason=expired`;
}

export default api;
