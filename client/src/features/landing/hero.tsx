"use client";

import Link from "next/link";
import { useQuery } from "@tanstack/react-query";
import { motion } from "framer-motion";
import { ArrowRight, Play, Sparkles } from "lucide-react";
import { Button } from "@/components/ui/button";
import { auctionService } from "@/services/auction.service";
import { searchService } from "@/services/search.service";
function formatCount(value: number) {
  return value.toLocaleString();
}

export function Hero() {
  const { data: auctions } = useQuery({
    queryKey: ["public", "auctions"],
    queryFn: () => auctionService.getAll(),
    staleTime: 60_000,
  });

  const { data: liveIdeas } = useQuery({
    queryKey: ["public", "live-ideas"],
    queryFn: () => searchService.liveIdeas(0, 1),
    staleTime: 60_000,
  });

  const activeAuctions = auctions?.filter((a) => a.status === "ACTIVE").length ?? 0;
  const completedAuctions = auctions?.filter((a) => a.status === "CLOSED").length ?? 0;
  const liveIdeaCount = liveIdeas?.totalElements ?? 0;

  const stats = [
    { label: "Active Auctions", value: formatCount(activeAuctions) },
    { label: "Live Ideas", value: formatCount(liveIdeaCount) },
    { label: "Completed Auctions", value: formatCount(completedAuctions) },
  ];

  return (
    <section className="relative overflow-hidden pt-32 pb-20 sm:pt-40 sm:pb-28">
      <div className="absolute inset-0 -z-10">
        <div className="absolute left-1/2 top-0 h-[500px] w-[800px] -translate-x-1/2 rounded-full bg-violet-600/20 blur-[120px]" />
      </div>

      <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
        <motion.div
          initial={{ opacity: 0, y: 24 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.6 }}
          className="mx-auto max-w-4xl text-center"
        >
          <div className="mb-6 inline-flex items-center gap-2 rounded-full border border-violet-500/30 bg-violet-500/10 px-4 py-1.5 text-sm text-violet-300">
            <Sparkles className="h-4 w-4" />
            The future of idea investing
          </div>

          <h1 className="text-4xl font-bold tracking-tight text-white sm:text-6xl lg:text-7xl">
            Turn bold ideas into{" "}
            <span className="gradient-text">funded ventures</span>
          </h1>

          <p className="mx-auto mt-6 max-w-2xl text-lg text-slate-400 sm:text-xl">
            IHub connects visionary creators with strategic investors through
            live idea auctions. Discover, bid, and back the next breakthrough.
          </p>

          <div className="mt-10 flex flex-col items-center justify-center gap-4 sm:flex-row">
            <Button size="lg" asChild>
              <Link href="/register">
                Start Investing <ArrowRight className="h-4 w-4" />
              </Link>
            </Button>
            <Button size="lg" variant="secondary" asChild>
              <Link href="/register?role=creator">
                <Play className="h-4 w-4" /> Submit Your Idea
              </Link>
            </Button>
          </div>

          <motion.div
            initial={{ opacity: 0, y: 40 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.3, duration: 0.6 }}
            className="relative mx-auto mt-16 max-w-3xl"
          >
            <div className="glass rounded-2xl p-1 shadow-2xl shadow-violet-500/10">
              <div className="rounded-xl bg-slate-900/80 p-6">
                <div className="flex items-center justify-between border-b border-white/5 pb-4">
                  <div className="flex gap-2">
                    <div className="h-3 w-3 rounded-full bg-red-500/80" />
                    <div className="h-3 w-3 rounded-full bg-amber-500/80" />
                    <div className="h-3 w-3 rounded-full bg-emerald-500/80" />
                  </div>
                  <span className="text-xs text-slate-500">Live Platform Stats</span>
                </div>
                <div className="mt-4 grid gap-4 sm:grid-cols-3">
                  {stats.map((stat) => (
                    <div key={stat.label} className="rounded-xl bg-white/5 p-4 text-left">
                      <p className="text-xs text-slate-500">{stat.label}</p>
                      <p className="mt-1 text-2xl font-bold text-white">{stat.value}</p>
                    </div>
                  ))}
                </div>
              </div>
            </div>
          </motion.div>
        </motion.div>
      </div>
    </section>
  );
}
