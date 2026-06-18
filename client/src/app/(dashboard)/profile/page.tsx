"use client";

import { useQuery, useQueries } from "@tanstack/react-query";
import { User, Lightbulb, Trophy, DollarSign } from "lucide-react";
import { PageHeader } from "@/components/shared/page-header";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";
import { useAuthStore } from "@/store/auth-store";
import { ideaService } from "@/services/idea.service";
import { auctionService } from "@/services/auction.service";
import { formatCurrency, formatDate } from "@/lib/utils";
import Link from "next/link";

export default function ProfilePage() {
  const { user, role, email } = useAuthStore();
  const userId = user?.id;

  const { data: ideas, isLoading: ideasLoading } = useQuery({
    queryKey: ["ideas", "profile"],
    queryFn: () => ideaService.getAll({ mine: true }),
    enabled: role === "CREATOR",
  });

  const { data: auctions } = useQuery({
    queryKey: ["auctions"],
    queryFn: () => auctionService.getAll(),
  });

  const closedAuctions = auctions?.filter((a) => a.status === "CLOSED") ?? [];

  const winnerQueries = useQueries({
    queries: closedAuctions.map((auction) => ({
      queryKey: ["auctions", auction.id, "winner"],
      queryFn: () => auctionService.getWinner(auction.id),
      retry: false,
      enabled: role === "INVESTOR" && !!userId,
    })),
  });

  const wonAuctions = closedAuctions
    .map((auction, i) => ({
      auction,
      winner: winnerQueries[i]?.data,
    }))
    .filter((entry) => entry.winner && userId && entry.winner.winnerId === userId);

  if (!user) {
    return <Skeleton className="mx-auto h-64 max-w-2xl" />;
  }

  return (
    <div className="mx-auto max-w-4xl">
      <PageHeader title="Profile" description="Your account and activity history." />

      <Card className="mb-6">
        <CardContent className="flex items-center gap-6 pt-6">
          <div className="flex h-16 w-16 items-center justify-center rounded-2xl bg-gradient-to-br from-violet-500 to-indigo-600">
            <User className="h-8 w-8 text-white" />
          </div>
          <div>
            <h2 className="text-xl font-bold text-white">{user.name}</h2>
            <p className="text-slate-400">{email}</p>
            <Badge className="mt-2">{role}</Badge>
          </div>
        </CardContent>
      </Card>

      {role === "CREATOR" && (
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <Lightbulb className="h-5 w-5 text-violet-400" />
              Submitted Ideas
            </CardTitle>
          </CardHeader>
          <CardContent>
            {ideasLoading ? (
              <Skeleton className="h-32" />
            ) : ideas?.length === 0 ? (
              <p className="text-sm text-slate-500">No ideas submitted yet.</p>
            ) : (
              <div className="space-y-3">
                {ideas?.map((idea) => (
                  <Link
                    key={idea.id}
                    href={`/ideas/${idea.id}`}
                    className="flex items-center justify-between rounded-xl bg-white/5 px-4 py-3 hover:bg-white/10"
                  >
                    <div>
                      <p className="font-medium text-white">{idea.title}</p>
                      <p className="text-xs text-slate-500">{idea.status} · {idea.category}</p>
                    </div>
                    <span className="text-sm text-violet-400">{formatCurrency(idea.basePrice)}</span>
                  </Link>
                ))}
              </div>
            )}
          </CardContent>
        </Card>
      )}

      {role === "INVESTOR" && (
        <div className="grid gap-6 lg:grid-cols-2">
          <Card>
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <DollarSign className="h-5 w-5 text-emerald-400" />
                Active Auctions
              </CardTitle>
            </CardHeader>
            <CardContent>
              {auctions?.filter((a) => a.status === "ACTIVE").length === 0 ? (
                <p className="text-sm text-slate-500">No active auctions.</p>
              ) : (
                <div className="space-y-3">
                  {auctions
                    ?.filter((a) => a.status === "ACTIVE")
                    .slice(0, 5)
                    .map((auction) => (
                      <Link
                        key={auction.id}
                        href={`/auctions/${auction.id}`}
                        className="block rounded-xl bg-white/5 px-4 py-3 hover:bg-white/10"
                      >
                        <p className="font-medium text-white">Auction #{auction.id}</p>
                        <p className="text-xs text-slate-500">
                          Ends {formatDate(auction.endTime)}
                        </p>
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
                Won Auctions ({wonAuctions.length})
              </CardTitle>
            </CardHeader>
            <CardContent>
              {winnerQueries.some((q) => q.isLoading) ? (
                <Skeleton className="h-24" />
              ) : wonAuctions.length === 0 ? (
                <p className="text-sm text-slate-500">
                  You haven&apos;t won any auctions yet. Keep bidding on live auctions!
                </p>
              ) : (
                <div className="space-y-3">
                  {wonAuctions.map(({ auction, winner }) => (
                    <Link
                      key={auction.id}
                      href={`/auctions/${auction.id}`}
                      className="flex items-center justify-between rounded-xl bg-white/5 px-4 py-3 hover:bg-white/10"
                    >
                      <div>
                        <p className="font-medium text-white">Auction #{auction.id}</p>
                        <p className="text-xs text-slate-500">
                          Won {winner ? formatDate(winner.closedAt) : ""}
                        </p>
                      </div>
                      <span className="font-semibold text-amber-400">
                        {winner ? formatCurrency(winner.winningBid) : ""}
                      </span>
                    </Link>
                  ))}
                </div>
              )}
            </CardContent>
          </Card>
        </div>
      )}
    </div>
  );
}
