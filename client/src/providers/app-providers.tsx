"use client";

import { Toaster } from "sonner";
import { QueryProvider } from "./query-provider";

export function AppProviders({ children }: { children: React.ReactNode }) {
  return (
    <QueryProvider>
      {children}
      <Toaster
        position="top-right"
        toastOptions={{
          classNames: {
            toast: "bg-slate-900 border border-white/10 text-white",
          },
        }}
      />
    </QueryProvider>
  );
}
