"use client";

import { motion } from "framer-motion";

const steps = [
  { step: "01", title: "Submit or Discover", description: "Creators publish ideas. Investors browse and search the marketplace." },
  { step: "02", title: "Auction Goes Live", description: "Ideas enter timed auctions with transparent bidding rules and increments." },
  { step: "03", title: "Bid in Real-Time", description: "Place competitive bids, track the leaderboard, and get instant outbid alerts." },
  { step: "04", title: "Winner Announced", description: "When the clock stops, the highest bidder wins and both parties are notified." },
];

export function HowItWorks() {
  return (
    <section id="how-it-works" className="border-y border-white/5 py-24">
      <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
        <h2 className="text-center text-3xl font-bold text-white sm:text-4xl">How it works</h2>
        <div className="mt-16 grid gap-8 md:grid-cols-2 lg:grid-cols-4">
          {steps.map((item, i) => (
            <motion.div
              key={item.step}
              initial={{ opacity: 0, x: -20 }}
              whileInView={{ opacity: 1, x: 0 }}
              viewport={{ once: true }}
              transition={{ delay: i * 0.1 }}
              className="relative"
            >
              <span className="text-5xl font-bold text-white/5">{item.step}</span>
              <h3 className="mt-2 text-lg font-semibold text-white">{item.title}</h3>
              <p className="mt-2 text-sm text-slate-400">{item.description}</p>
            </motion.div>
          ))}
        </div>
      </div>
    </section>
  );
}
