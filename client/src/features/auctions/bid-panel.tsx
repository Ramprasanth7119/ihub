"use client";

import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { motion } from "framer-motion";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { bidService } from "@/services/bid.service";
import { formatCurrency } from "@/lib/utils";
import type { HighestBid } from "@/types";

const schema = z.object({
  amount: z.number().min(0.01, "Bid must be greater than 0"),
});

type FormData = z.infer<typeof schema>;

interface BidPanelProps {
  auctionId: number;
  highestBid?: HighestBid;
  minIncrement?: number;
  status: string;
}

export function BidPanel({ auctionId, highestBid, minIncrement = 100, status }: BidPanelProps) {
  const queryClient = useQueryClient();
  const [lastBid, setLastBid] = useState<number | null>(null);

  const minBid = (highestBid?.amount ?? 0) + minIncrement;

  const { register, handleSubmit, reset, formState: { errors } } = useForm<FormData>({
    resolver: zodResolver(schema),
    defaultValues: { amount: minBid },
  });

  const mutation = useMutation({
    mutationFn: (data: FormData) => bidService.placeBid(auctionId, data.amount),
    onSuccess: (res) => {
      setLastBid(res.amount);
      toast.success(res.message || "Bid placed successfully!");
      reset({ amount: res.currentHighest + minIncrement });
      queryClient.invalidateQueries({ queryKey: ["bids", auctionId] });
    },
    onError: (err: unknown) => {
      const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message;
      toast.error(msg ?? "Failed to place bid");
    },
  });

  if (status !== "ACTIVE") {
    return (
      <div className="rounded-xl bg-white/5 p-4 text-center text-sm text-slate-400">
        Bidding is only available during active auctions.
      </div>
    );
  }

  return (
    <div>
      <div className="mb-4 rounded-xl bg-emerald-500/10 p-4">
        <p className="text-xs text-slate-400">Current Highest Bid</p>
        <motion.p
          key={highestBid?.amount}
          initial={{ scale: 1.05 }}
          animate={{ scale: 1 }}
          className="text-2xl font-bold text-emerald-400"
        >
          {highestBid ? formatCurrency(highestBid.amount) : "No bids yet"}
        </motion.p>
        {highestBid && (
          <p className="mt-1 text-xs text-slate-500">by {highestBid.investorName}</p>
        )}
        <p className="mt-2 text-xs text-slate-500">Minimum next bid: {formatCurrency(minBid)}</p>
      </div>

      <form onSubmit={handleSubmit((d) => mutation.mutate(d))} className="space-y-3">
        <div>
          <Label htmlFor="amount">Your Bid ($)</Label>
          <Input
            id="amount"
            type="number"
            step="0.01"
            className="mt-1.5"
            {...register("amount", { valueAsNumber: true })}
          />
          {errors.amount && <p className="mt-1 text-xs text-red-400">{errors.amount.message}</p>}
        </div>
        <Button type="submit" className="w-full" disabled={mutation.isPending}>
          {mutation.isPending ? "Placing bid..." : "Place Bid"}
        </Button>
      </form>

      {lastBid && (
        <motion.p
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          className="mt-3 text-center text-xs text-violet-400"
        >
          Your last bid: {formatCurrency(lastBid)}
        </motion.p>
      )}
    </div>
  );
}
