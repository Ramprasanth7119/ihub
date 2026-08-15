"use client";

import { useEffect, useEffectEvent, useMemo, useState } from "react";
import { cn } from "@/lib/utils";

interface CountdownTimerProps {
  endTime: string;
  className?: string;
  onComplete?: () => void;
}

function getTimeLeft(end: Date) {
  const diff = end.getTime() - Date.now();
  if (diff <= 0) return { days: 0, hours: 0, minutes: 0, seconds: 0, expired: true };
  const days = Math.floor(diff / (1000 * 60 * 60 * 24));
  const hours = Math.floor((diff / (1000 * 60 * 60)) % 24);
  const minutes = Math.floor((diff / (1000 * 60)) % 60);
  const seconds = Math.floor((diff / 1000) % 60);
  return { days, hours, minutes, seconds, expired: false };
}

export function CountdownTimer({ endTime, className, onComplete }: CountdownTimerProps) {
  // Memoised so the effect below sees a stable value. Constructing the Date inline
  // produced a new object every render, tearing down and recreating the interval
  // on each tick.
  const end = useMemo(() => new Date(endTime), [endTime]);
  const [time, setTime] = useState(() => getTimeLeft(end));

  // `onComplete` is behaviour, not a dependency: callers pass inline arrows, and
  // treating it as reactive would restart the countdown on every parent render.
  const handleComplete = useEffectEvent(() => onComplete?.());

  useEffect(() => {
    const interval = setInterval(() => {
      const next = getTimeLeft(end);
      setTime(next);
      if (next.expired) {
        clearInterval(interval);
        handleComplete();
      }
    }, 1000);

    return () => clearInterval(interval);
  }, [end]);

  const displayed = time;

  if (displayed.expired) {
    return (
      <span className={cn("font-mono text-slate-500", className)}>Ended</span>
    );
  }

  const parts = [
    displayed.days > 0 && `${displayed.days}d`,
    `${String(displayed.hours).padStart(2, "0")}h`,
    `${String(displayed.minutes).padStart(2, "0")}m`,
    `${String(displayed.seconds).padStart(2, "0")}s`,
  ].filter(Boolean);

  return (
    <span
      className={cn("font-mono tabular-nums text-emerald-400", className)}
      // Announced politely so screen readers aren't interrupted every second.
      aria-live="off"
    >
      {parts.join(" ")}
    </span>
  );
}
