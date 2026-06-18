"use client";

import { useEffect } from "react";
import { AlertCircle } from "lucide-react";
import { Button } from "@/components/ui/button";

export default function DashboardError({
  error,
  reset,
}: {
  error: Error & { digest?: string };
  reset: () => void;
}) {
  useEffect(() => {
    console.error(error);
  }, [error]);

  return (
    <div className="flex min-h-[50vh] flex-col items-center justify-center px-4 text-center">
      <AlertCircle className="mb-4 h-12 w-12 text-red-400" />
      <h2 className="text-xl font-semibold text-white">Something went wrong</h2>
      <p className="mt-2 max-w-md text-sm text-slate-400">
        {error.message || "An unexpected error occurred while loading this page."}
      </p>
      <Button className="mt-6" onClick={reset}>
        Try again
      </Button>
    </div>
  );
}
