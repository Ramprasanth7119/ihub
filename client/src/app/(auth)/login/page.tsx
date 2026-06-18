"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { useMutation } from "@tanstack/react-query";
import { toast } from "sonner";
import { motion } from "framer-motion";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { authService } from "@/services/auth.service";
import { userService } from "@/services/user.service";
import { useAuthStore } from "@/store/auth-store";
import { getDefaultDashboardPath } from "@/lib/middleware-auth";

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
    formState: { errors },
  } = useForm<FormData>({ resolver: zodResolver(schema) });

  const mutation = useMutation({
    mutationFn: (data: FormData) => authService.login(data.email, data.password),
    onSuccess: async (res) => {
      setTokens(res.accessToken, res.refreshToken, res.role);
      const user = await userService.getMe();
      setUser(user);
      toast.success("Welcome back!");
      router.push(getDefaultDashboardPath(res.role as "CREATOR" | "INVESTOR" | "ADMIN"));
    },
    onError: () => toast.error("Invalid email or password"),
  });

  return (
    <motion.div
      initial={{ opacity: 0, y: 16 }}
      animate={{ opacity: 1, y: 0 }}
      className="w-full max-w-md"
    >
      <div className="mb-8 lg:hidden">
        <Link href="/" className="text-2xl font-bold text-white">IHub</Link>
      </div>
      <h1 className="text-2xl font-bold text-white">Sign in</h1>
      <p className="mt-2 text-sm text-slate-400">
        Don&apos;t have an account?{" "}
        <Link href="/register" className="text-violet-400 hover:underline">
          Create one
        </Link>
      </p>

      <form onSubmit={handleSubmit((d) => mutation.mutate(d))} className="mt-8 space-y-5">
        <div>
          <Label htmlFor="email">Email</Label>
          <Input id="email" type="email" placeholder="you@company.com" className="mt-1.5" {...register("email")} />
          {errors.email && <p className="mt-1 text-xs text-red-400">{errors.email.message}</p>}
        </div>
        <div>
          <Label htmlFor="password">Password</Label>
          <Input id="password" type="password" placeholder="••••••••" className="mt-1.5" {...register("password")} />
          {errors.password && <p className="mt-1 text-xs text-red-400">{errors.password.message}</p>}
        </div>
        <Button type="submit" className="w-full" disabled={mutation.isPending}>
          {mutation.isPending ? "Signing in..." : "Sign in"}
        </Button>
      </form>
    </motion.div>
  );
}
