import Link from "next/link";
import { Zap } from "lucide-react";

export default function AuthLayout({ children }: { children: React.ReactNode }) {
  return (
    <div className="flex min-h-screen">
      <div className="hidden w-1/2 flex-col justify-between bg-gradient-to-br from-violet-950 via-slate-950 to-indigo-950 p-12 lg:flex">
        <Link href="/" className="flex items-center gap-2">
          <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-gradient-to-br from-violet-500 to-indigo-600">
            <Zap className="h-5 w-5 text-white" />
          </div>
          <span className="text-xl font-bold text-white">IHub</span>
        </Link>
        <div>
          <h2 className="text-4xl font-bold leading-tight text-white">
            Where ideas meet <span className="text-violet-400">capital</span>
          </h2>
          <p className="mt-4 max-w-md text-slate-400">
            Join the marketplace redefining how startups and investors connect through live auctions.
          </p>
        </div>
        <p className="text-sm text-slate-600">Trusted by 2,000+ creators and investors</p>
      </div>
      <div className="flex flex-1 items-center justify-center p-6">{children}</div>
    </div>
  );
}
