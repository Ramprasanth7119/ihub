"use client";

import { useEffect, useState } from "react";
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
  const end = new Date(endTime);
  const [time, setTime] = useState(() => getTimeLeft(end));

  useEffect(() => {
    const interval = setInterval(() => {
      const next = getTimeLeft(end);
      setTime(next);
      if (next.expired) {
        clearInterval(interval);
        onComplete?.();
      }
    }, 1000);
    return () => clearInterval(interval);
  }, [endTime, onComplete, end]);

  if (time.expired) {
    return <span className={cn("font-mono text-slate-500", className)}>Ended</span>;
  }

  const parts = [
    time.days > 0 && `${time.days}d`,
    `${String(time.hours).padStart(2, "0")}h`,
    `${String(time.minutes).padStart(2, "0")}m`,
    `${String(time.seconds).padStart(2, "0")}s`,
  ].filter(Boolean);

  return (
    <span className={cn("font-mono text-emerald-400 tabular-nums", className)}>
      {parts.join(" ")}
    </span>
  );
}
