"use client";

import { motion } from "framer-motion";
import { Gavel, LineChart, Search, Shield, Zap, Users } from "lucide-react";

const features = [
  {
    icon: Search,
    title: "Smart Discovery",
    description: "Elasticsearch-powered search with instant filters, facets, and relevance ranking.",
  },
  {
    icon: Gavel,
    title: "Live Auctions",
    description: "Real-time bidding with countdown timers, leaderboards, and instant bid updates.",
  },
  {
    icon: LineChart,
    title: "Analytics Dashboard",
    description: "Track performance, category distribution, and investment metrics at a glance.",
  },
  {
    icon: Shield,
    title: "Secure & Trusted",
    description: "JWT authentication, role-based access, and transparent auction lifecycle.",
  },
  {
    icon: Zap,
    title: "Instant Notifications",
    description: "Get alerted on outbids, auction starts, endings, and winning announcements.",
  },
  {
    icon: Users,
    title: "Two-Sided Marketplace",
    description: "Purpose-built flows for creators and investors with tailored dashboards.",
  },
];

export function FeaturesSection() {
  return (
    <section id="features" className="py-24">
      <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
        <div className="mx-auto max-w-2xl text-center">
          <h2 className="text-3xl font-bold text-white sm:text-4xl">
            Built for the <span className="gradient-text">modern investor</span>
          </h2>
          <p className="mt-4 text-slate-400">
            Everything you need to discover, evaluate, and invest in breakthrough ideas.
          </p>
        </div>

        <div className="mt-16 grid gap-6 sm:grid-cols-2 lg:grid-cols-3">
          {features.map((feature, i) => (
            <motion.div
              key={feature.title}
              initial={{ opacity: 0, y: 20 }}
              whileInView={{ opacity: 1, y: 0 }}
              viewport={{ once: true }}
              transition={{ delay: i * 0.08 }}
              className="group rounded-2xl border border-white/10 bg-white/[0.02] p-6 transition-colors hover:border-violet-500/30 hover:bg-white/[0.04]"
            >
              <div className="mb-4 flex h-11 w-11 items-center justify-center rounded-xl bg-violet-500/15 text-violet-400 transition-colors group-hover:bg-violet-500/25">
                <feature.icon className="h-5 w-5" />
              </div>
              <h3 className="text-lg font-semibold text-white">{feature.title}</h3>
              <p className="mt-2 text-sm text-slate-400">{feature.description}</p>
            </motion.div>
          ))}
        </div>
      </div>
    </section>
  );
}
