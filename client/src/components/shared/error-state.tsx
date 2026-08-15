"use client";

import { AlertCircle, RefreshCw, SearchX, ShieldAlert, WifiOff } from "lucide-react";
import { Button } from "@/components/ui/button";
import { getErrorMessage, getErrorStatus, isNetworkError } from "@/lib/api-error";

interface ErrorStateProps {
  title?: string;
  message?: string;
  onRetry?: () => void;
  /** Pass the caught error to derive an appropriate icon, title and message. */
  error?: unknown;
  className?: string;
}

/**
 * Failure state for a page or panel.
 *
 * <p>Presentation is derived from the HTTP status so the user gets a response that
 * matches the situation — an unreachable server, a permission problem and a missing
 * record should not all read as "something went wrong". Raw errors are never shown;
 * everything goes through {@link getErrorMessage}.</p>
 */
export function ErrorState({ title, message, onRetry, error, className }: ErrorStateProps) {
  const status = getErrorStatus(error);
  const derived = deriveAppearance(status, isNetworkError(error));
  const Icon = derived.Icon;

  const resolvedTitle = title ?? derived.title;
  const resolvedMessage = message ?? (error !== undefined ? getErrorMessage(error) : derived.message);

  return (
    <div
      role="alert"
      className={`flex flex-col items-center justify-center rounded-2xl border px-6 py-12 text-center ${derived.tone} ${className ?? ""}`}
    >
      <Icon className={`mb-4 h-10 w-10 ${derived.iconTone}`} aria-hidden="true" />
      <p className="text-base font-semibold text-slate-800 dark:text-slate-100">{resolvedTitle}</p>
      <p className="mt-1 max-w-md text-sm text-slate-600 dark:text-slate-300">{resolvedMessage}</p>
      {onRetry && (
        <Button variant="secondary" className="mt-5" onClick={onRetry}>
          <RefreshCw className="mr-2 h-4 w-4" />
          Try again
        </Button>
      )}
    </div>
  );
}

function deriveAppearance(status: number | undefined, offline: boolean) {
  if (offline) {
    return {
      Icon: WifiOff,
      title: "Can't reach the server",
      message: "Check your connection and try again.",
      tone: "border-slate-300/40 bg-slate-500/5",
      iconTone: "text-slate-500",
    };
  }

  switch (status) {
    case 403:
      return {
        Icon: ShieldAlert,
        title: "You don't have access to this",
        message: "Your account doesn't have permission to view this page.",
        tone: "border-amber-500/30 bg-amber-500/5",
        iconTone: "text-amber-500",
      };
    case 404:
      return {
        Icon: SearchX,
        title: "Not found",
        message: "This item may have been removed or never existed.",
        tone: "border-slate-300/40 bg-slate-500/5",
        iconTone: "text-slate-500",
      };
    case 503:
      return {
        Icon: AlertCircle,
        title: "Temporarily unavailable",
        message: "Search is offline right now. Everything else still works — try again shortly.",
        tone: "border-amber-500/30 bg-amber-500/5",
        iconTone: "text-amber-500",
      };
    default:
      return {
        Icon: AlertCircle,
        title: "Something went wrong",
        message: "Please try again.",
        tone: "border-red-500/25 bg-red-500/5",
        iconTone: "text-red-500",
      };
  }
}
