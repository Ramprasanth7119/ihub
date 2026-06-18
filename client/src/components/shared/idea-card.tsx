"use client";

import Link from "next/link";
import { motion } from "framer-motion";
import { ArrowUpRight, Tag } from "lucide-react";
import { Badge } from "@/components/ui/badge";
import { formatCurrency, getStatusColor } from "@/lib/utils";
import type { Idea, IdeaDocument } from "@/types";

interface IdeaCardProps {
  idea: Idea | IdeaDocument;
  href?: string;
  index?: number;
}

export function IdeaCard({ idea, href, index = 0 }: IdeaCardProps) {
  const minBudget = "basePrice" in idea ? idea.basePrice : idea.minBudget;
  const maxBudget = idea.maxBudget;
  const status = "status" in idea ? idea.status : idea.auctionStatus ?? idea.ideaStatus;
  const link = href ?? `/ideas/${idea.id}`;

  return (
    <motion.div
      initial={{ opacity: 0, y: 16 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ delay: index * 0.05 }}
    >
      <Link
        href={link}
        className="group block rounded-2xl border border-white/10 bg-white/[0.03] p-5 backdrop-blur-xl transition-all hover:border-violet-500/30 hover:bg-white/[0.05]"
      >
        <div className="flex items-start justify-between gap-3">
          <div className="min-w-0 flex-1">
            <div className="flex flex-wrap items-center gap-2">
              {status && (
                <span className={`rounded-full border px-2 py-0.5 text-xs font-medium ${getStatusColor(status)}`}>
                  {status}
                </span>
              )}
              {idea.category && (
                <Badge variant="muted">{idea.category}</Badge>
              )}
            </div>
            <h3 className="mt-3 truncate text-lg font-semibold text-white group-hover:text-violet-300">
              {idea.title}
            </h3>
            <p className="mt-2 line-clamp-2 text-sm text-slate-400">{idea.description}</p>
          </div>
          <ArrowUpRight className="h-5 w-5 shrink-0 text-slate-600 transition-colors group-hover:text-violet-400" />
        </div>
        <div className="mt-4 flex items-center justify-between border-t border-white/5 pt-4">
          <div className="flex items-center gap-1 text-sm text-slate-400">
            <Tag className="h-3.5 w-3.5" />
            {minBudget != null && formatCurrency(minBudget)}
            {maxBudget != null && ` – ${formatCurrency(maxBudget)}`}
          </div>
          {"tags" in idea && idea.tags && idea.tags.length > 0 && (
            <div className="flex gap-1">
              {idea.tags.slice(0, 2).map((tag) => (
                <span key={tag} className="text-xs text-slate-500">#{tag}</span>
              ))}
            </div>
          )}
        </div>
      </Link>
    </motion.div>
  );
}
