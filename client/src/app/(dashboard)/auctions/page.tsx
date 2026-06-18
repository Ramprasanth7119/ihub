"use client";

import { useQuery } from "@tanstack/react-query";
import { Gavel } from "lucide-react";
import { PageHeader } from "@/components/shared/page-header";
import { AuctionCard } from "@/components/shared/auction-card";
import { EmptyState } from "@/components/shared/empty-state";
import { Skeleton } from "@/components/ui/skeleton";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { auctionService } from "@/services/auction.service";
import { ideaService } from "@/services/idea.service";
import { bidService } from "@/services/bid.service";

export default function AuctionsPage() {
  const { data: auctions, isLoading } = useQuery({
    queryKey: ["auctions"],
    queryFn: () => auctionService.getAll(),
  });

  const { data: ideas } = useQuery({
    queryKey: ["ideas"],
    queryFn: () => ideaService.getAll(),
  });

  const ideaMap = new Map(ideas?.map((i) => [i.id, i.title]));

  const renderList = (status?: string) => {
    const filtered = status
      ? auctions?.filter((a) => a.status === status)
      : auctions;

    if (isLoading) {
      return (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {Array.from({ length: 6 }).map((_, i) => (
            <Skeleton key={i} className="h-52" />
          ))}
        </div>
      );
    }

    if (!filtered?.length) {
      return (
        <EmptyState
          icon={Gavel}
          title={`No ${status?.toLowerCase() ?? ""} auctions`}
          description="Check back later for new auction opportunities."
        />
      );
    }

    return (
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        {filtered.map((auction, i) => (
          <AuctionCard
            key={auction.id}
            auction={auction}
            ideaTitle={ideaMap.get(auction.ideaId)}
            index={i}
          />
        ))}
      </div>
    );
  };

  return (
    <div>
      <PageHeader title="Auctions" description="Browse active, upcoming, and closed idea auctions." />

      <Tabs defaultValue="ACTIVE">
        <TabsList>
          <TabsTrigger value="ACTIVE">Active</TabsTrigger>
          <TabsTrigger value="UPCOMING">Upcoming</TabsTrigger>
          <TabsTrigger value="CLOSED">Closed</TabsTrigger>
          <TabsTrigger value="ALL">All</TabsTrigger>
        </TabsList>
        <TabsContent value="ACTIVE">{renderList("ACTIVE")}</TabsContent>
        <TabsContent value="UPCOMING">{renderList("UPCOMING")}</TabsContent>
        <TabsContent value="CLOSED">{renderList("CLOSED")}</TabsContent>
        <TabsContent value="ALL">{renderList()}</TabsContent>
      </Tabs>
    </div>
  );
}
