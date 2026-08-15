import * as React from "react";
import { cva, type VariantProps } from "class-variance-authority";
import { cn } from "@/lib/utils";

const badgeVariants = cva(
  "inline-flex items-center rounded-full border px-2.5 py-0.5 text-xs font-medium transition-colors",
  {
    variants: {
      variant: {
        default: "border-violet-500/30 bg-violet-500/15 text-violet-300",
        success: "border-emerald-500/30 bg-emerald-500/15 text-emerald-300",
        warning: "border-amber-500/30 bg-amber-500/15 text-amber-300",
        muted: "border-white/10 bg-white/5 text-slate-400",
        secondary:
          "border-slate-300/40 bg-slate-500/10 text-slate-500 [[data-surface=light]_&]:border-slate-200 [[data-surface=light]_&]:bg-slate-100 [[data-surface=light]_&]:text-slate-700",
        /**
         * Colour-free base for callers that supply their own status classes via
         * `className` — e.g. `getStatusColor` / `getAdminStatusColor`.
         */
        outline: "border-current/20 bg-transparent",
      },
    },
    defaultVariants: { variant: "default" },
  }
);

export interface BadgeProps
  extends React.HTMLAttributes<HTMLDivElement>,
    VariantProps<typeof badgeVariants> {}

function Badge({ className, variant, ...props }: BadgeProps) {
  return <div className={cn(badgeVariants({ variant }), className)} {...props} />;
}

export { Badge, badgeVariants };
