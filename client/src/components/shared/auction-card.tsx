"use client";

import Link from "next/link";
import { motion } from "framer-motion";
import { Gavel, Users } from "lucide-react";
import { CountdownTimer } from "./countdown-timer";
import { getStatusColor } from "@/lib/utils";
import type { Auction } from "@/types";

interface AuctionCardProps {
  auction: Auction;
  ideaTitle?: string;
  bidCount?: number;
  highestBid?: number;
  index?: number;
}

export function AuctionCard({
  auction,
  ideaTitle,
  bidCount = 0,
  highestBid,
  index = 0,
}: AuctionCardProps) {
  const targetTime = auction.status === "UPCOMING" ? auction.startTime : auction.endTime;
  const timerLabel = auction.status === "UPCOMING" ? "Starts in" : auction.status === "ACTIVE" ? "Ends in" : "Closed";

  return (
    <motion.div
      initial={{ opacity: 0, y: 16 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ delay: index * 0.05 }}
    >
      <Link
        href={`/auctions/${auction.id}`}
        className="group block rounded-2xl border border-white/10 bg-gradient-to-br from-white/[0.04] to-transparent p-5 backdrop-blur-xl transition-all hover:border-indigo-500/30"
      >
        <div className="flex items-center justify-between">
          <span className={`rounded-full border px-2.5 py-0.5 text-xs font-medium ${getStatusColor(auction.status)}`}>
            {auction.status}
          </span>
          <Gavel className="h-4 w-4 text-slate-500 group-hover:text-indigo-400" />
        </div>
        <h3 className="mt-3 text-lg font-semibold text-white group-hover:text-indigo-300">
          {ideaTitle ?? `Auction #${auction.id}`}
        </h3>
        <div className="mt-4 grid grid-cols-2 gap-3 text-sm">
          <div>
            <p className="text-slate-500">{timerLabel}</p>
            {auction.status !== "CLOSED" ? (
              <CountdownTimer endTime={targetTime} />
            ) : (
              <span className="text-slate-400">—</span>
            )}
          </div>
          <div>
            <p className="text-slate-500">Highest Bid</p>
            <p className="font-semibold text-white">
              {highestBid != null ? `$${highestBid.toLocaleString()}` : "—"}
            </p>
          </div>
        </div>
        <div className="mt-4 flex items-center gap-1.5 border-t border-white/5 pt-3 text-xs text-slate-500">
          <Users className="h-3.5 w-3.5" />
          {bidCount} bids placed
        </div>
      </Link>
    </motion.div>
  );
}
