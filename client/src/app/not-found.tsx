import Link from "next/link";
import { Compass, Home, Search } from "lucide-react";

/**
 * Custom 404. Without this, an unknown route rendered Next.js's unstyled default
 * page, which drops the visitor out of the product with no way back.
 */
export default function NotFound() {
  return (
    <main className="mesh-bg flex min-h-screen items-center justify-center px-6 py-24">
      <div className="w-full max-w-md text-center">
        <div className="mx-auto mb-6 flex h-16 w-16 items-center justify-center rounded-2xl bg-violet-500/10 text-violet-400">
          <Compass className="h-8 w-8" />
        </div>

        <p className="text-sm font-semibold uppercase tracking-widest text-violet-400">404</p>
        <h1 className="mt-2 text-3xl font-bold tracking-tight text-white">Page not found</h1>
        <p className="mt-3 text-slate-400">
          This page may have moved, or the idea or auction you were looking for is no longer
          available.
        </p>

        <div className="mt-8 flex flex-col gap-3 sm:flex-row sm:justify-center">
          <Link
            href="/"
            className="inline-flex items-center justify-center gap-2 rounded-xl bg-gradient-to-r from-violet-600 to-indigo-600 px-5 py-2.5 text-sm font-medium text-white shadow-lg shadow-violet-500/25 transition-colors hover:from-violet-500 hover:to-indigo-500"
          >
            <Home className="h-4 w-4" />
            Back to home
          </Link>
          <Link
            href="/search"
            className="inline-flex items-center justify-center gap-2 rounded-xl border border-white/15 px-5 py-2.5 text-sm font-medium text-white transition-colors hover:bg-white/5"
          >
            <Search className="h-4 w-4" />
            Discover ideas
          </Link>
        </div>
      </div>
    </main>
  );
}
