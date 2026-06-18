"use client";

import { motion } from "framer-motion";
import { Quote } from "lucide-react";

const testimonials = [
  {
    quote: "IHub helped us find our lead investor in 48 hours. The live auction format created real urgency.",
    author: "Sarah Chen",
    role: "Creator, FinFlow AI",
  },
  {
    quote: "The search and filtering is incredible. I've discovered three portfolio companies through IHub this quarter.",
    author: "Marcus Webb",
    role: "Angel Investor",
  },
  {
    quote: "Real-time bidding with transparent leaderboards — finally an investment platform that feels modern.",
    author: "Elena Rodriguez",
    role: "VC Partner, Horizon Labs",
  },
];

export function Testimonials() {
  return (
    <section id="testimonials" className="border-y border-white/5 py-24">
      <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
        <h2 className="text-center text-3xl font-bold text-white">Trusted by innovators</h2>
        <div className="mt-16 grid gap-6 md:grid-cols-3">
          {testimonials.map((t, i) => (
            <motion.div
              key={t.author}
              initial={{ opacity: 0, y: 20 }}
              whileInView={{ opacity: 1, y: 0 }}
              viewport={{ once: true }}
              transition={{ delay: i * 0.1 }}
              className="rounded-2xl border border-white/10 bg-white/[0.02] p-6"
            >
              <Quote className="h-8 w-8 text-violet-500/40" />
              <p className="mt-4 text-sm leading-relaxed text-slate-300">&ldquo;{t.quote}&rdquo;</p>
              <div className="mt-6 border-t border-white/5 pt-4">
                <p className="font-medium text-white">{t.author}</p>
                <p className="text-xs text-slate-500">{t.role}</p>
              </div>
            </motion.div>
          ))}
        </div>
      </div>
    </section>
  );
}
