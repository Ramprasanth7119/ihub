"use client";

import { use } from "react";
import { useQuery, useMutation } from "@tanstack/react-query";
import { toast } from "sonner";
import Link from "next/link";
import { PageHeader } from "@/components/shared/page-header";
import { ErrorState } from "@/components/shared/error-state";
import { Skeleton } from "@/components/ui/skeleton";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { ideaService } from "@/services/idea.service";
import { auctionService } from "@/services/auction.service";
import { userService } from "@/services/user.service";
import { useAuthStore } from "@/store/auth-store";
import { IDEA_STATUS_LABELS } from "@/constants";
import { formatCurrency, getStatusColor } from "@/lib/utils";

export default function IdeaDetailPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = use(params);
  const ideaId = Number(id);
  const role = useAuthStore((s) => s.role);
  const userId = useAuthStore((s) => s.user?.id);

  const { data: idea, isLoading, isError, refetch } = useQuery({
    queryKey: ["ideas", ideaId],
    queryFn: () => ideaService.getById(ideaId),
  });

  const { data: creator } = useQuery({
    queryKey: ["users", idea?.creatorId],
    queryFn: () => userService.getById(idea!.creatorId),
    enabled: !!idea?.creatorId,
  });

  const { data: auctions } = useQuery({
    queryKey: ["auctions"],
    queryFn: () => auctionService.getAll(),
  });

  const ideaAuction = auctions?.find((a) => a.ideaId === ideaId);

  const publishMutation = useMutation({
    mutationFn: () => ideaService.publish(ideaId),
    onSuccess: () => {
      toast.success("Idea published!");
      refetch();
    },
    onError: () => toast.error("Failed to publish idea"),
  });

  if (isLoading) return <Skeleton className="h-96" />;
  if (isError || !idea) return <ErrorState onRetry={() => refetch()} />;

  const isOwner = userId === idea.creatorId;

  return (
    <div className="mx-auto max-w-4xl">
      <PageHeader
        title={idea.title}
        action={
          <div className="flex gap-2">
            {isOwner && idea.status === "DRAFT" && (
              <>
                <Button variant="secondary" asChild>
                  <Link href={`/ideas/${ideaId}/edit`}>Edit</Link>
                </Button>
                <Button onClick={() => publishMutation.mutate()} disabled={publishMutation.isPending}>
                  Publish
                </Button>
              </>
            )}
            {ideaAuction && (
              <Button variant="secondary" asChild>
                <Link href={`/auctions/${ideaAuction.id}`}>View Auction</Link>
              </Button>
            )}
          </div>
        }
      />

      <div className="flex flex-wrap items-center gap-2">
        <span className={`rounded-full border px-2.5 py-0.5 text-xs font-medium ${getStatusColor(idea.status)}`}>
          {idea.status}
        </span>
        <Badge variant="muted">{idea.category}</Badge>
        {idea.tags?.map((tag) => (
          <Badge key={tag} variant="muted">#{tag}</Badge>
        ))}
      </div>

      {IDEA_STATUS_LABELS[idea.status] && (
        <p className="mt-3 text-sm text-slate-400">{IDEA_STATUS_LABELS[idea.status]}</p>
      )}

      {isOwner && idea.status === "REJECTED" && (
        <p className="mt-2 text-sm text-red-400">
          Your idea was rejected. Edit and resubmit after addressing feedback.
        </p>
      )}

      <div className="mt-8 grid gap-6 lg:grid-cols-3">
        <div className="lg:col-span-2">
          <Card>
            <CardHeader>
              <CardTitle>Description</CardTitle>
            </CardHeader>
            <CardContent>
              <p className="leading-relaxed text-slate-300">{idea.description}</p>
            </CardContent>
          </Card>
        </div>

        <div className="space-y-4">
          <Card>
            <CardHeader>
              <CardTitle>Details</CardTitle>
            </CardHeader>
            <CardContent className="space-y-3 text-sm">
              <div className="flex justify-between">
                <span className="text-slate-500">Min Budget</span>
                <span className="text-white">{formatCurrency(idea.basePrice)}</span>
              </div>
              {idea.maxBudget && (
                <div className="flex justify-between">
                  <span className="text-slate-500">Max Budget</span>
                  <span className="text-white">{formatCurrency(idea.maxBudget)}</span>
                </div>
              )}
              <div className="flex justify-between">
                <span className="text-slate-500">Creator</span>
                <span className="text-white">{creator?.name ?? `#${idea.creatorId}`}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-slate-500">Auction</span>
                <span className="text-white">
                  {ideaAuction ? (
                    <Link href={`/auctions/${ideaAuction.id}`} className="text-violet-400 hover:underline">
                      {ideaAuction.status}
                    </Link>
                  ) : (
                    "None"
                  )}
                </span>
              </div>
            </CardContent>
          </Card>
        </div>
      </div>
    </div>
  );
}
