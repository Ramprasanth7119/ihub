"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { useMutation, useQuery } from "@tanstack/react-query";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { toast } from "sonner";
import { User, Lightbulb, Trophy, Gavel, KeyRound, Save, TrendingUp } from "lucide-react";
import { PageHeader } from "@/components/shared/page-header";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Skeleton } from "@/components/ui/skeleton";
import { useAuthStore } from "@/store/auth-store";
import { ideaService } from "@/services/idea.service";
import { bidService } from "@/services/bid.service";
import { userService } from "@/services/user.service";
import { getErrorMessage } from "@/lib/api-error";
import { formatCurrency, formatDate } from "@/lib/utils";

const profileSchema = z.object({
  name: z.string().min(2, "Name must be at least 2 characters").max(100),
});

const passwordSchema = z
  .object({
    currentPassword: z.string().min(1, "Enter your current password"),
    newPassword: z.string().min(8, "Password must be at least 8 characters").max(72),
    confirmPassword: z.string(),
  })
  .refine((data) => data.newPassword === data.confirmPassword, {
    message: "Passwords do not match",
    path: ["confirmPassword"],
  })
  .refine((data) => data.newPassword !== data.currentPassword, {
    message: "New password must differ from the current one",
    path: ["newPassword"],
  });

type ProfileForm = z.infer<typeof profileSchema>;
type PasswordForm = z.infer<typeof passwordSchema>;

export default function ProfilePage() {
  const { user, role, email } = useAuthStore();

  if (!user) {
    return <Skeleton className="mx-auto h-64 max-w-3xl" />;
  }

  return (
    <div className="mx-auto max-w-4xl">
      <PageHeader title="Profile" description="Your account details and activity." />

      <Card className="mb-6">
        <CardContent className="flex flex-wrap items-center gap-6 pt-6">
          <div className="flex h-16 w-16 items-center justify-center rounded-2xl bg-gradient-to-br from-violet-500 to-indigo-600">
            <User className="h-8 w-8 text-white" />
          </div>
          <div>
            <h2 className="text-xl font-bold text-white">{user.name}</h2>
            <p className="text-slate-400">{user.email ?? email}</p>
            <Badge className="mt-2">{role}</Badge>
          </div>
        </CardContent>
      </Card>

      <div className="mb-6 grid gap-6 lg:grid-cols-2">
        <AccountDetailsCard currentName={user.name} />
        <PasswordCard />
      </div>

      {role === "CREATOR" && <CreatorActivity />}
      {role === "INVESTOR" && <InvestorActivity />}
    </div>
  );
}

function AccountDetailsCard({ currentName }: { currentName: string }) {
  const setUser = useAuthStore((s) => s.setUser);

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isDirty },
  } = useForm<ProfileForm>({
    resolver: zodResolver(profileSchema),
    defaultValues: { name: currentName },
  });

  useEffect(() => {
    reset({ name: currentName });
  }, [currentName, reset]);

  const mutation = useMutation({
    mutationFn: (data: ProfileForm) => userService.updateProfile(data),
    onSuccess: (updated) => {
      setUser(updated);
      reset({ name: updated.name });
      toast.success("Profile updated");
    },
    onError: (error) => toast.error(getErrorMessage(error, "Could not update your profile")),
  });

  return (
    <Card>
      <CardHeader>
        <CardTitle className="flex items-center gap-2">
          <User className="h-5 w-5 text-violet-400" />
          Account details
        </CardTitle>
      </CardHeader>
      <CardContent>
        <form onSubmit={handleSubmit((data) => mutation.mutate(data))} className="space-y-4">
          <div>
            <Label htmlFor="name">Display name</Label>
            <Input id="name" className="mt-1.5" {...register("name")} />
            {errors.name && (
              <p className="mt-1 text-xs text-red-400" role="alert">
                {errors.name.message}
              </p>
            )}
          </div>
          {/* Email and role are deliberately not editable: email is the login
              identity and role governs authorization, so both are changed by an
              administrator rather than self-assigned. */}
          <p className="text-xs text-slate-500">
            To change your email address or role, contact an administrator.
          </p>
          <Button type="submit" disabled={!isDirty || mutation.isPending}>
            <Save className="mr-2 h-4 w-4" />
            {mutation.isPending ? "Saving…" : "Save changes"}
          </Button>
        </form>
      </CardContent>
    </Card>
  );
}

function PasswordCard() {
  const [justChanged, setJustChanged] = useState(false);

  const {
    register,
    handleSubmit,
    reset,
    setError,
    formState: { errors },
  } = useForm<PasswordForm>({ resolver: zodResolver(passwordSchema) });

  const mutation = useMutation({
    mutationFn: (data: PasswordForm) =>
      userService.changePassword({
        currentPassword: data.currentPassword,
        newPassword: data.newPassword,
      }),
    onSuccess: () => {
      reset();
      setJustChanged(true);
      toast.success("Password changed");
    },
    onError: (error) => {
      const message = getErrorMessage(error, "Could not change your password");
      // A 422 here means the current password was wrong, so the message belongs
      // on that field rather than floating at the bottom of the form.
      setError("currentPassword", { message });
    },
  });

  return (
    <Card>
      <CardHeader>
        <CardTitle className="flex items-center gap-2">
          <KeyRound className="h-5 w-5 text-violet-400" />
          Password
        </CardTitle>
      </CardHeader>
      <CardContent>
        <form
          onSubmit={handleSubmit((data) => {
            setJustChanged(false);
            mutation.mutate(data);
          })}
          className="space-y-4"
        >
          <div>
            <Label htmlFor="current-password">Current password</Label>
            <Input
              id="current-password"
              type="password"
              autoComplete="current-password"
              className="mt-1.5"
              {...register("currentPassword")}
            />
            {errors.currentPassword && (
              <p className="mt-1 text-xs text-red-400" role="alert">
                {errors.currentPassword.message}
              </p>
            )}
          </div>
          <div>
            <Label htmlFor="new-password">New password</Label>
            <Input
              id="new-password"
              type="password"
              autoComplete="new-password"
              className="mt-1.5"
              {...register("newPassword")}
            />
            {errors.newPassword && (
              <p className="mt-1 text-xs text-red-400" role="alert">
                {errors.newPassword.message}
              </p>
            )}
          </div>
          <div>
            <Label htmlFor="confirm-password">Confirm new password</Label>
            <Input
              id="confirm-password"
              type="password"
              autoComplete="new-password"
              className="mt-1.5"
              {...register("confirmPassword")}
            />
            {errors.confirmPassword && (
              <p className="mt-1 text-xs text-red-400" role="alert">
                {errors.confirmPassword.message}
              </p>
            )}
          </div>

          {justChanged && (
            <p className="rounded-lg border border-emerald-500/25 bg-emerald-500/10 p-3 text-sm text-emerald-300">
              Password updated. Use it the next time you sign in.
            </p>
          )}

          <Button type="submit" disabled={mutation.isPending}>
            {mutation.isPending ? "Updating…" : "Change password"}
          </Button>
        </form>
      </CardContent>
    </Card>
  );
}

function CreatorActivity() {
  const { data: ideas, isLoading } = useQuery({
    queryKey: ["ideas", "profile"],
    queryFn: () => ideaService.getAll({ mine: true, size: 50 }),
  });

  return (
    <Card>
      <CardHeader>
        <CardTitle className="flex items-center gap-2">
          <Lightbulb className="h-5 w-5 text-violet-400" />
          Your ideas
        </CardTitle>
      </CardHeader>
      <CardContent>
        {isLoading ? (
          <Skeleton className="h-32" />
        ) : !ideas || ideas.length === 0 ? (
          <p className="text-sm text-slate-500">
            You haven&apos;t submitted any ideas yet.{" "}
            <Link href="/ideas/new" className="text-violet-400 hover:underline">
              Submit your first one
            </Link>
            .
          </p>
        ) : (
          <div className="space-y-3">
            {ideas.map((idea) => (
              <Link
                key={idea.id}
                href={`/ideas/${idea.id}`}
                className="flex items-center justify-between rounded-xl bg-white/5 px-4 py-3 transition-colors hover:bg-white/10"
              >
                <div className="min-w-0">
                  <p className="truncate font-medium text-white">{idea.title}</p>
                  <p className="text-xs text-slate-500">
                    {idea.status} · {idea.category}
                  </p>
                </div>
                <span className="ml-4 shrink-0 text-sm text-violet-400">
                  {formatCurrency(idea.basePrice)}
                </span>
              </Link>
            ))}
          </div>
        )}
      </CardContent>
    </Card>
  );
}

/**
 * Investor activity, sourced from `/bids/my`.
 *
 * <p>This previously listed every closed auction on the platform and issued a
 * winner lookup for each one just to discover which the current user had won —
 * dozens of requests to render one card. The backend now returns that directly.</p>
 */
function InvestorActivity() {
  const { data: bids, isLoading } = useQuery({
    queryKey: ["bids", "mine", 0],
    queryFn: () => bidService.getMyBids({ size: 50 }),
  });

  const won = (bids ?? []).filter((bid) => bid.won);
  const leading = (bids ?? []).filter((bid) => bid.leading && bid.auctionStatus === "ACTIVE");

  return (
    <div className="grid gap-6 lg:grid-cols-2">
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <TrendingUp className="h-5 w-5 text-emerald-400" />
            Leading bids ({leading.length})
          </CardTitle>
        </CardHeader>
        <CardContent>
          {isLoading ? (
            <Skeleton className="h-24" />
          ) : leading.length === 0 ? (
            <p className="text-sm text-slate-500">
              You&apos;re not top bidder on any live auction.{" "}
              <Link href="/auctions" className="text-violet-400 hover:underline">
                Browse auctions
              </Link>
              .
            </p>
          ) : (
            <div className="space-y-3">
              {leading.slice(0, 5).map((bid) => (
                <Link
                  key={bid.bidId}
                  href={`/auctions/${bid.auctionId}`}
                  className="flex items-center justify-between rounded-xl bg-white/5 px-4 py-3 transition-colors hover:bg-white/10"
                >
                  <div className="min-w-0">
                    <p className="truncate font-medium text-white">{bid.ideaTitle}</p>
                    <p className="text-xs text-slate-500">Ends {formatDate(bid.endTime)}</p>
                  </div>
                  <span className="ml-4 shrink-0 font-semibold text-emerald-400">
                    {formatCurrency(bid.amount)}
                  </span>
                </Link>
              ))}
            </div>
          )}
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <Trophy className="h-5 w-5 text-amber-400" />
            Auctions won ({won.length})
          </CardTitle>
        </CardHeader>
        <CardContent>
          {isLoading ? (
            <Skeleton className="h-24" />
          ) : won.length === 0 ? (
            <p className="text-sm text-slate-500">
              You haven&apos;t won an auction yet. Keep bidding on live auctions.
            </p>
          ) : (
            <div className="space-y-3">
              {won.slice(0, 5).map((bid) => (
                <Link
                  key={bid.bidId}
                  href={`/auctions/${bid.auctionId}`}
                  className="flex items-center justify-between rounded-xl bg-white/5 px-4 py-3 transition-colors hover:bg-white/10"
                >
                  <div className="min-w-0">
                    <p className="truncate font-medium text-white">{bid.ideaTitle}</p>
                    <p className="text-xs text-slate-500">Won {formatDate(bid.endTime)}</p>
                  </div>
                  <span className="ml-4 shrink-0 font-semibold text-amber-400">
                    {formatCurrency(bid.amount)}
                  </span>
                </Link>
              ))}
            </div>
          )}
          {won.length > 5 && (
            <Link
              href="/my-bids"
              className="mt-4 inline-flex items-center gap-1 text-sm text-violet-400 hover:underline"
            >
              <Gavel className="h-3.5 w-3.5" />
              See all bids
            </Link>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
