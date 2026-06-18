"use client";

import { motion, AnimatePresence } from "framer-motion";
import { Trophy } from "lucide-react";
import { formatCurrency } from "@/lib/utils";
import type { LeaderboardEntry } from "@/types";

interface LeaderboardProps {
  entries: LeaderboardEntry[];
  currentUserId?: number;
}

export function Leaderboard({ entries, currentUserId }: LeaderboardProps) {
  if (entries.length === 0) {
    return (
      <div className="rounded-xl border border-dashed border-white/10 py-8 text-center text-sm text-slate-500">
        No bids yet. Be the first to bid!
      </div>
    );
  }

  return (
    <div className="space-y-2">
      <AnimatePresence mode="popLayout">
        {entries.map((entry) => (
          <motion.div
            key={entry.investorId}
            layout
            initial={{ opacity: 0, x: -10 }}
            animate={{ opacity: 1, x: 0 }}
            exit={{ opacity: 0, x: 10 }}
            className={`flex items-center gap-3 rounded-xl px-4 py-3 ${
              entry.investorId === currentUserId
                ? "bg-violet-500/15 border border-violet-500/30"
                : "bg-white/5"
            }`}
          >
            <div className={`flex h-8 w-8 items-center justify-center rounded-lg text-sm font-bold ${
              entry.rank === 1 ? "bg-amber-500/20 text-amber-400" : "bg-white/10 text-slate-400"
            }`}>
              {entry.rank === 1 ? <Trophy className="h-4 w-4" /> : entry.rank}
            </div>
            <div className="min-w-0 flex-1">
              <p className="truncate font-medium text-white">{entry.investorName}</p>
            </div>
            <p className="font-semibold text-emerald-400">{formatCurrency(entry.highestBid)}</p>
          </motion.div>
        ))}
      </AnimatePresence>
    </div>
  );
}
