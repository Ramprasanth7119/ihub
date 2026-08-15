import { AxiosError } from "axios";
import type { ApiError } from "@/types";

/**
 * Translates any thrown value into a message that is safe and useful to show a
 * user.
 *
 * <p>The backend returns a consistent `ErrorResponse` envelope, so its `message`
 * is preferred when present — it carries the specific business reason ("Bid must
 * be at least 155000.00"). Everything else falls back to a per-status sentence.
 * Raw exception text and stack traces are never surfaced.</p>
 */
export function getErrorMessage(error: unknown, fallback = "Something went wrong. Please try again."): string {
  if (!(error instanceof AxiosError)) {
    return error instanceof Error && error.message ? humanise(error.message, fallback) : fallback;
  }

  // No response at all: the request never reached the backend.
  if (!error.response) {
    if (error.code === "ECONNABORTED") {
      return "The request timed out. Please check your connection and try again.";
    }
    return "Cannot reach the server. Please check your connection and try again.";
  }

  const status = error.response.status;
  const data = error.response.data as ApiError | undefined;

  // Field-level validation errors are the most actionable thing we can show.
  if (status === 400 && data?.fieldErrors?.length) {
    return data.fieldErrors.map((fieldError) => fieldError.message).join(". ");
  }

  if (data?.message) {
    return data.message;
  }

  return statusMessage(status, fallback);
}

/** Default copy per status, used when the backend sent no message of its own. */
function statusMessage(status: number, fallback: string): string {
  switch (status) {
    case 400:
      return "That request wasn't valid. Please review the form and try again.";
    case 401:
      return "Your session has expired. Please sign in again.";
    case 403:
      return "You don't have permission to do that.";
    case 404:
      return "We couldn't find what you were looking for.";
    case 409:
      return "That conflicts with the current state — it may have changed since you loaded this page.";
    case 422:
      return "That value isn't allowed here. Please adjust it and try again.";
    case 429:
      return "Too many requests. Please wait a moment and try again.";
    case 503:
      return "This feature is temporarily unavailable. Please try again shortly.";
    default:
      return status >= 500
        ? "The server ran into a problem. We're on it — please try again shortly."
        : fallback;
  }
}

/** Strips anything that looks like internal detail out of a non-Axios error. */
function humanise(message: string, fallback: string): string {
  const looksInternal = /(\bat\s+\w+\.|Exception|stack|undefined is not|Cannot read)/i.test(message);
  return looksInternal ? fallback : message;
}

export function getErrorStatus(error: unknown): number | undefined {
  return error instanceof AxiosError ? error.response?.status : undefined;
}

/** True when the failure is Elasticsearch being unreachable rather than a bad query. */
export function isSearchUnavailable(error: unknown): boolean {
  return getErrorStatus(error) === 503;
}

/** True when the request never reached the backend at all. */
export function isNetworkError(error: unknown): boolean {
  return error instanceof AxiosError && !error.response;
}

export function isNotFound(error: unknown): boolean {
  return getErrorStatus(error) === 404;
}

export function isForbidden(error: unknown): boolean {
  return getErrorStatus(error) === 403;
}
