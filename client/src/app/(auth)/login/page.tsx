"use client";

import { Suspense } from "react";
import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { useMutation } from "@tanstack/react-query";
import { toast } from "sonner";
import { motion } from "framer-motion";
import { Clock } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { authService } from "@/services/auth.service";
import { userService } from "@/services/user.service";
import { useAuthStore } from "@/store/auth-store";
import { getDefaultDashboardPath } from "@/lib/middleware-auth";
import { getErrorMessage } from "@/lib/api-error";

const schema = z.object({
  email: z.string().email("Invalid email address"),
  password: z.string().min(8, "Password must be at least 8 characters"),
});

type FormData = z.infer<typeof schema>;

export default function LoginPage() {
  const router = useRouter();
  const { setTokens, setUser } = useAuthStore();

  const {
    register,
    handleSubmit,
    setError,
    formState: { errors },
  } = useForm<FormData>({ resolver: zodResolver(schema) });

  const mutation = useMutation({
    mutationFn: (data: FormData) => authService.login(data.email, data.password),
    onSuccess: async (res) => {
      setTokens(res.accessToken, res.refreshToken, res.role);

      // Non-fatal: useAuth refetches the profile once the dashboard mounts.
      try {
        setUser(await userService.getMe());
      } catch {
        /* ignore */
      }

      toast.success("Welcome back");
      router.push(resolveRedirect(res.role as "CREATOR" | "INVESTOR" | "ADMIN"));
    },
    onError: (error) => {
      // Shows the backend's actual reason — a suspended account returns 403 with
      // an explanation that "Invalid email or password" would have hidden.
      const message = getErrorMessage(error, "Invalid email or password");
      setError("root", { message });
      toast.error(message);
    },
  });

  return (
    <motion.div
      initial={{ opacity: 0, y: 16 }}
      animate={{ opacity: 1, y: 0 }}
      className="w-full max-w-md"
    >
      <div className="mb-8 lg:hidden">
        <Link href="/" className="text-2xl font-bold text-white">
          IHub
        </Link>
      </div>
      <h1 className="text-2xl font-bold text-white">Sign in</h1>
      <p className="mt-2 text-sm text-slate-400">
        Don&apos;t have an account?{" "}
        <Link href="/register" className="text-violet-400 hover:underline">
          Create one
        </Link>
      </p>

      {/* Only the query-string-dependent notice is suspended. Wrapping the whole
          page would make the server render a skeleton instead of the form. */}
      <Suspense fallback={null}>
        <SessionExpiredNotice />
      </Suspense>

      <form onSubmit={handleSubmit((d) => mutation.mutate(d))} className="mt-8 space-y-5" noValidate>
        <div>
          <Label htmlFor="email">Email</Label>
          <Input
            id="email"
            type="email"
            autoComplete="email"
            placeholder="you@company.com"
            className="mt-1.5"
            aria-invalid={Boolean(errors.email)}
            {...register("email")}
          />
          {errors.email && (
            <p className="mt-1 text-xs text-red-400" role="alert">
              {errors.email.message}
            </p>
          )}
        </div>
        <div>
          <Label htmlFor="password">Password</Label>
          <Input
            id="password"
            type="password"
            autoComplete="current-password"
            placeholder="••••••••"
            className="mt-1.5"
            aria-invalid={Boolean(errors.password)}
            {...register("password")}
          />
          {errors.password && (
            <p className="mt-1 text-xs text-red-400" role="alert">
              {errors.password.message}
            </p>
          )}
        </div>

        {errors.root && (
          <p
            role="alert"
            className="rounded-lg border border-red-500/25 bg-red-500/10 p-3 text-sm text-red-300"
          >
            {errors.root.message}
          </p>
        )}

        <Button type="submit" className="w-full" disabled={mutation.isPending}>
          {mutation.isPending ? "Signing in…" : "Sign in"}
        </Button>
      </form>
    </motion.div>
  );
}

function SessionExpiredNotice() {
  const searchParams = useSearchParams();
  if (searchParams.get("reason") !== "expired") return null;

  return (
    <div
      role="status"
      className="mt-6 flex items-start gap-3 rounded-xl border border-amber-500/25 bg-amber-500/10 p-3"
    >
      <Clock className="mt-0.5 h-4 w-4 shrink-0 text-amber-400" />
      <p className="text-sm text-amber-200">
        Your session expired. Sign in again to pick up where you left off.
      </p>
    </div>
  );
}

/**
 * Where to send the visitor after signing in.
 *
 * <p>Read from the live URL at submit time rather than through `useSearchParams`,
 * so the form itself never suspends. Only same-origin paths are honoured — an
 * absolute URL here would be an open redirect.</p>
 */
function resolveRedirect(role: "CREATOR" | "INVESTOR" | "ADMIN"): string {
  if (typeof window !== "undefined") {
    const target = new URLSearchParams(window.location.search).get("redirect");
    if (target && target.startsWith("/") && !target.startsWith("//")) {
      return target;
    }
  }
  return getDefaultDashboardPath(role);
}
