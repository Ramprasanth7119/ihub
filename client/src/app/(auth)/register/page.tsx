"use client";

import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { useForm, Controller } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { useMutation } from "@tanstack/react-query";
import { toast } from "sonner";
import { motion } from "framer-motion";
import { Lightbulb, TrendingUp } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { authService } from "@/services/auth.service";
import { useAuthStore } from "@/store/auth-store";
import { cn } from "@/lib/utils";
import { getDefaultDashboardPath } from "@/lib/middleware-auth";
import { Suspense } from "react";

const schema = z.object({
  name: z.string().min(2, "Name is required"),
  email: z.string().email("Invalid email"),
  password: z.string().min(8, "Password must be at least 8 characters"),
  role: z.enum(["CREATOR", "INVESTOR"]),
});

type FormData = z.infer<typeof schema>;

function RegisterForm() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const defaultRole = searchParams.get("role") === "creator" ? "CREATOR" : "INVESTOR";
  const { setTokens, setUser } = useAuthStore();

  const { register, handleSubmit, control, watch, formState: { errors } } = useForm<FormData>({
    resolver: zodResolver(schema),
    defaultValues: { role: defaultRole as "CREATOR" | "INVESTOR" },
  });

  const selectedRole = watch("role");

  const mutation = useMutation({
    mutationFn: async (data: FormData) => {
      const user = await authService.register(data);
      const auth = await authService.login(data.email, data.password);
      return { user, auth };
    },
    onSuccess: ({ user, auth }) => {
      setTokens(auth.accessToken, auth.refreshToken, auth.role);
      setUser(user);
      toast.success("Account created successfully!");
      router.push(getDefaultDashboardPath(auth.role as "CREATOR" | "INVESTOR" | "ADMIN"));
    },
    onError: (err: unknown) => {
      const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message;
      toast.error(msg ?? "Registration failed");
    },
  });

  return (
    <motion.div initial={{ opacity: 0, y: 16 }} animate={{ opacity: 1, y: 0 }} className="w-full max-w-md">
      <div className="mb-8 lg:hidden">
        <Link href="/" className="text-2xl font-bold text-white">IHub</Link>
      </div>
      <h1 className="text-2xl font-bold text-white">Create account</h1>
      <p className="mt-2 text-sm text-slate-400">
        Already have an account?{" "}
        <Link href="/login" className="text-violet-400 hover:underline">Sign in</Link>
      </p>

      <form onSubmit={handleSubmit((d) => mutation.mutate(d))} className="mt-8 space-y-5">
        <div>
          <Label>I am a...</Label>
          <Controller
            name="role"
            control={control}
            render={({ field }) => (
              <div className="mt-2 grid grid-cols-2 gap-3">
                {[
                  { value: "CREATOR" as const, label: "Creator", icon: Lightbulb, desc: "Submit ideas" },
                  { value: "INVESTOR" as const, label: "Investor", icon: TrendingUp, desc: "Bid on ideas" },
                ].map((opt) => (
                  <button
                    key={opt.value}
                    type="button"
                    onClick={() => field.onChange(opt.value)}
                    className={cn(
                      "rounded-xl border p-4 text-left transition-all",
                      selectedRole === opt.value
                        ? "border-violet-500/50 bg-violet-500/10"
                        : "border-white/10 bg-white/5 hover:border-white/20"
                    )}
                  >
                    <opt.icon className={cn("h-5 w-5", selectedRole === opt.value ? "text-violet-400" : "text-slate-500")} />
                    <p className="mt-2 font-medium text-white">{opt.label}</p>
                    <p className="text-xs text-slate-500">{opt.desc}</p>
                  </button>
                ))}
              </div>
            )}
          />
        </div>

        <div>
          <Label htmlFor="name">Full Name</Label>
          <Input id="name" placeholder="Jane Doe" className="mt-1.5" {...register("name")} />
          {errors.name && <p className="mt-1 text-xs text-red-400">{errors.name.message}</p>}
        </div>
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
          {mutation.isPending ? "Creating account..." : "Create account"}
        </Button>
      </form>
    </motion.div>
  );
}

export default function RegisterPage() {
  return (
    <Suspense>
      <RegisterForm />
    </Suspense>
  );
}
