"use client";

import { motion } from "framer-motion";
import { CheckCircle2 } from "lucide-react";

const creatorBenefits = [
  "Submit and manage ideas with full lifecycle control",
  "Schedule auctions with custom bid increments",
  "Track bids and see your winning investor",
  "Category analytics and performance insights",
];

const investorBenefits = [
  "Discover ideas with powerful Elasticsearch search",
  "Join live auctions with real-time leaderboards",
  "Place competitive bids with instant feedback",
  "Portfolio tracking and investment history",
];

export function Benefits() {
  return (
    <section className="py-24">
      <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
        <div className="grid gap-8 lg:grid-cols-2">
          <motion.div
            initial={{ opacity: 0, x: -20 }}
            whileInView={{ opacity: 1, x: 0 }}
            viewport={{ once: true }}
            className="rounded-2xl border border-violet-500/20 bg-gradient-to-br from-violet-500/10 to-transparent p-8"
          >
            <h3 className="text-2xl font-bold text-white">For Creators</h3>
            <p className="mt-2 text-slate-400">Turn your vision into a funded reality.</p>
            <ul className="mt-6 space-y-3">
              {creatorBenefits.map((b) => (
                <li key={b} className="flex items-start gap-3 text-sm text-slate-300">
                  <CheckCircle2 className="mt-0.5 h-4 w-4 shrink-0 text-violet-400" />
                  {b}
                </li>
              ))}
            </ul>
          </motion.div>

          <motion.div
            initial={{ opacity: 0, x: 20 }}
            whileInView={{ opacity: 1, x: 0 }}
            viewport={{ once: true }}
            className="rounded-2xl border border-indigo-500/20 bg-gradient-to-br from-indigo-500/10 to-transparent p-8"
          >
            <h3 className="text-2xl font-bold text-white">For Investors</h3>
            <p className="mt-2 text-slate-400">Find and back the next big thing.</p>
            <ul className="mt-6 space-y-3">
              {investorBenefits.map((b) => (
                <li key={b} className="flex items-start gap-3 text-sm text-slate-300">
                  <CheckCircle2 className="mt-0.5 h-4 w-4 shrink-0 text-indigo-400" />
                  {b}
                </li>
              ))}
            </ul>
          </motion.div>
        </div>
      </div>
    </section>
  );
}
