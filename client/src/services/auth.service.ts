import api from "@/lib/axios";
import type { AuthResponse } from "@/types";

export const authService = {
  login: (email: string, password: string) =>
    api.post<AuthResponse>("/auth/login", { email, password }).then((r) => r.data),

  register: (data: { name: string; email: string; password: string; role: string }) =>
    api.post("/users", data).then((r) => r.data),

  refresh: (refreshToken: string) =>
    api.post<AuthResponse>("/auth/refresh", { refreshToken }).then((r) => r.data),

  logout: (refreshToken: string) =>
    api.post("/auth/logout", { refreshToken }),
};
