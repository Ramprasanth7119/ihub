"use client";

import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { Switch } from "@/components/ui/switch";
import { Skeleton } from "@/components/ui/skeleton";
import { ErrorState } from "@/components/shared/error-state";
import {
  Save,
  Bell,
  Shield,
  Database,
  Search as SearchIcon,
  RefreshCw,
  CheckCircle2,
  AlertTriangle,
} from "lucide-react";
import { toast } from "sonner";
import { adminService } from "@/services/admin.service";
import { getErrorMessage } from "@/lib/api-error";
import type { PlatformSettings } from "@/types";

/**
 * Platform settings.
 *
 * <p>These are persisted server-side and several change real behaviour: the bid
 * increment seeds new auctions, the auto-start/auto-end toggles gate the lifecycle
 * scheduler, and the alert toggles gate notification delivery. Each card notes what
 * it actually affects so an operator is not guessing.</p>
 */
export default function AdminSettingsPage() {
  const queryClient = useQueryClient();
  const [localDraft, setLocalDraft] = useState<PlatformSettings | null>(null);

  const {
    data: settings,
    isLoading,
    isError,
    error,
    refetch,
  } = useQuery({
    queryKey: ["admin", "settings"],
    queryFn: () => adminService.getSettings(),
  });

  const { data: metrics } = useQuery({
    queryKey: ["admin", "metrics"],
    queryFn: () => adminService.getMetrics(),
  });

  const { data: searchHealth } = useQuery({
    queryKey: ["admin", "search-health"],
    queryFn: () => adminService.getSearchHealth(),
    refetchInterval: 60_000,
  });

  // Edits are held locally so a half-finished form is not pushed on every keystroke.
  // `draft` stays null until the first edit and falls back to the fetched settings,
  // which avoids mirroring server state into local state inside an effect.
  const draft = localDraft ?? settings ?? null;

  const saveMutation = useMutation({
    mutationFn: (next: PlatformSettings) => adminService.updateSettings(next),
    onSuccess: (saved) => {
      queryClient.setQueryData(["admin", "settings"], saved);
      // Clearing the local copy makes the freshly saved server state authoritative.
      setLocalDraft(null);
      toast.success("Settings saved");
    },
    onError: (err) => toast.error(getErrorMessage(err, "Could not save settings")),
  });

  const reindexMutation = useMutation({
    mutationFn: () => adminService.reindexSearch(),
    onSuccess: (result) => {
      toast.success(`Search index rebuilt — ${result.indexed} ideas indexed`);
      queryClient.invalidateQueries({ queryKey: ["admin", "search-health"] });
    },
    onError: (err) => toast.error(getErrorMessage(err, "Reindex failed")),
  });

  const update = <K extends keyof PlatformSettings>(key: K, value: PlatformSettings[K]) => {
    setLocalDraft((prev) => {
      const base = prev ?? settings;
      return base ? { ...base, [key]: value } : prev;
    });
  };

  const handleSave = () => {
    if (draft) saveMutation.mutate(draft);
  };

  const isDirty = Boolean(
    localDraft && settings && JSON.stringify(localDraft) !== JSON.stringify(settings)
  );

  if (isLoading || !draft) {
    return (
      <div className="space-y-6">
        <Skeleton className="h-10 w-48" />
        <Skeleton className="h-28 w-full" />
        <div className="grid gap-6 md:grid-cols-2">
          <Skeleton className="h-72 w-full md:col-span-2" />
          <Skeleton className="h-80 w-full" />
          <Skeleton className="h-80 w-full" />
        </div>
      </div>
    );
  }

  if (isError) {
    return (
      <ErrorState
        title="Couldn't load settings"
        message={getErrorMessage(error)}
        onRetry={() => refetch()}
      />
    );
  }

  return (
    <div className="space-y-6 pb-24">
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div>
          <h1 className="text-3xl font-bold text-gray-900">Settings</h1>
          <p className="mt-2 text-gray-600">
            Platform-wide configuration. Changes take effect immediately across the service.
          </p>
        </div>
      </div>

      {metrics && (
        <Card>
          <CardHeader>
            <CardTitle>Live Platform Stats</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
              <Stat label="Users" value={metrics.totalUsers} />
              <Stat label="Ideas" value={metrics.totalIdeas} />
              <Stat label="Auctions" value={metrics.totalAuctions} />
              <Stat label="Bids" value={metrics.totalBids} />
            </div>
          </CardContent>
        </Card>
      )}

      <div className="grid gap-6 md:grid-cols-2">
        <Card className="md:col-span-2">
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <Shield className="h-5 w-5" />
              General
            </CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="grid gap-4 md:grid-cols-2">
              <div>
                <Label htmlFor="platform-name">Platform Name</Label>
                <Input
                  id="platform-name"
                  value={draft.platformName}
                  onChange={(e) => update("platformName", e.target.value)}
                />
              </div>
              <div>
                <Label htmlFor="support-email">Support Email</Label>
                <Input
                  id="support-email"
                  type="email"
                  value={draft.supportEmail}
                  onChange={(e) => update("supportEmail", e.target.value)}
                />
              </div>
            </div>
            <div>
              <Label htmlFor="platform-description">Platform Description</Label>
              <Textarea
                id="platform-description"
                value={draft.platformDescription}
                onChange={(e) => update("platformDescription", e.target.value)}
                rows={3}
              />
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <Bell className="h-5 w-5" />
              Notifications
            </CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            <ToggleRow
              label="Email Delivery"
              description="Drain the outbox and send queued emails. When off, messages stay queued and flush once re-enabled."
              checked={draft.emailNotifications}
              onChange={(v) => update("emailNotifications", v)}
            />
            <ToggleRow
              label="Idea Moderation Alerts"
              description="Notify creators when you approve, reject or suspend their idea."
              checked={draft.ideaApprovalAlerts}
              onChange={(v) => update("ideaApprovalAlerts", v)}
            />
            <ToggleRow
              label="Auction Start Alerts"
              description="Notify the creator when their auction opens."
              checked={draft.auctionStartAlerts}
              onChange={(v) => update("auctionStartAlerts", v)}
            />
            <ToggleRow
              label="Auction End Alerts"
              description="Notify the creator and all bidders when an auction closes."
              checked={draft.auctionEndAlerts}
              onChange={(v) => update("auctionEndAlerts", v)}
            />
            <p className="rounded-md bg-amber-50 p-3 text-xs text-amber-800">
              Outbid alerts are always delivered — they are essential to a fair auction and
              cannot be disabled.
            </p>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <Database className="h-5 w-5" />
              Auction Defaults
            </CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            <div>
              <Label htmlFor="default-min-bid">Default Minimum Bid</Label>
              <Input
                id="default-min-bid"
                type="number"
                min={0.01}
                step="0.01"
                value={draft.defaultMinBid}
                onChange={(e) => update("defaultMinBid", Number(e.target.value))}
              />
              <p className="mt-1 text-xs text-gray-500">Suggested starting price for new auctions.</p>
            </div>
            <div>
              <Label htmlFor="default-bid-increment">Default Bid Increment</Label>
              <Input
                id="default-bid-increment"
                type="number"
                min={0.01}
                step="0.01"
                value={draft.defaultBidIncrement}
                onChange={(e) => update("defaultBidIncrement", Number(e.target.value))}
              />
              <p className="mt-1 text-xs text-gray-500">
                How much each new bid must exceed the current highest by, when an auction
                does not specify its own.
              </p>
            </div>
            <div>
              <Label htmlFor="auction-duration">Default Duration (hours)</Label>
              <Input
                id="auction-duration"
                type="number"
                min={1}
                max={8760}
                value={draft.auctionDurationHours}
                onChange={(e) => update("auctionDurationHours", Number(e.target.value))}
              />
            </div>
            <ToggleRow
              label="Auto-start Auctions"
              description="Open scheduled auctions automatically at their start time."
              checked={draft.autoStartAuctions}
              onChange={(v) => update("autoStartAuctions", v)}
            />
            <ToggleRow
              label="Auto-end Auctions"
              description="Close auctions and select a winner automatically at their end time."
              checked={draft.autoEndAuctions}
              onChange={(v) => update("autoEndAuctions", v)}
            />
            {(!draft.autoStartAuctions || !draft.autoEndAuctions) && (
              <p className="rounded-md bg-amber-50 p-3 text-xs text-amber-800">
                With automation off, affected auctions must be started or ended manually from
                the Auctions page.
              </p>
            )}
          </CardContent>
        </Card>

        <Card className="md:col-span-2">
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <SearchIcon className="h-5 w-5" />
              Search Index
            </CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="flex flex-wrap items-center justify-between gap-4">
              <div className="flex items-center gap-2">
                {searchHealth?.available ? (
                  <>
                    <CheckCircle2 className="h-5 w-5 text-emerald-600" />
                    <span className="text-sm font-medium text-emerald-700">
                      Elasticsearch reachable
                    </span>
                  </>
                ) : (
                  <>
                    <AlertTriangle className="h-5 w-5 text-amber-600" />
                    <span className="text-sm font-medium text-amber-700">
                      Elasticsearch unreachable — discovery and search are degraded
                    </span>
                  </>
                )}
              </div>
              <Button
                variant="outline"
                onClick={() => reindexMutation.mutate()}
                disabled={reindexMutation.isPending}
              >
                <RefreshCw
                  className={`mr-2 h-4 w-4 ${reindexMutation.isPending ? "animate-spin" : ""}`}
                />
                {reindexMutation.isPending ? "Rebuilding…" : "Rebuild index"}
              </Button>
            </div>
            <p className="text-xs text-gray-500">
              Ideas are indexed as they are published. Index writes are best-effort so an
              Elasticsearch outage never blocks a database write — rebuild here to reconcile
              afterwards.
            </p>
          </CardContent>
        </Card>

        <Card className="md:col-span-2">
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <Shield className="h-5 w-5" />
              Sessions
            </CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            <ToggleRow
              label="Session Timeout"
              description="Sign users out after a period of inactivity."
              checked={draft.sessionTimeoutEnabled}
              onChange={(v) => update("sessionTimeoutEnabled", v)}
            />
            <div className="max-w-xs">
              <Label htmlFor="session-timeout">Timeout (minutes)</Label>
              <Input
                id="session-timeout"
                type="number"
                min={5}
                max={1440}
                value={draft.sessionTimeoutMinutes}
                onChange={(e) => update("sessionTimeoutMinutes", Number(e.target.value))}
                disabled={!draft.sessionTimeoutEnabled}
              />
            </div>
          </CardContent>
        </Card>
      </div>

      {/* One save bar for the whole page: the previous per-card buttons all wrote the
          same object, so "saving" one section silently persisted every other edit too. */}
      <div className="fixed inset-x-0 bottom-0 z-20 border-t border-gray-200 bg-white/95 backdrop-blur lg:pl-64">
        <div className="mx-auto flex max-w-6xl items-center justify-between gap-4 px-4 py-3 sm:px-6">
          <p className="text-sm text-gray-600">
            {isDirty ? "You have unsaved changes." : "All changes saved."}
          </p>
          <div className="flex gap-2">
            <Button
              variant="outline"
              onClick={() => setLocalDraft(null)}
              disabled={!isDirty || saveMutation.isPending}
            >
              Discard
            </Button>
            <Button onClick={handleSave} disabled={!isDirty || saveMutation.isPending}>
              <Save className="mr-2 h-4 w-4" />
              {saveMutation.isPending ? "Saving…" : "Save changes"}
            </Button>
          </div>
        </div>
      </div>
    </div>
  );
}

function Stat({ label, value }: { label: string; value: number }) {
  return (
    <div className="rounded-lg bg-gray-50 p-4">
      <p className="text-sm text-gray-500">{label}</p>
      <p className="text-2xl font-bold text-gray-900">{value.toLocaleString()}</p>
    </div>
  );
}

function ToggleRow({
  label,
  description,
  checked,
  onChange,
}: {
  label: string;
  description: string;
  checked: boolean;
  onChange: (value: boolean) => void;
}) {
  return (
    <div className="flex items-start justify-between gap-4">
      <div>
        <Label>{label}</Label>
        <p className="text-sm text-gray-500">{description}</p>
      </div>
      <Switch
        checked={checked}
        aria-label={label}
        onClick={() => onChange(!checked)}
        className="mt-1 shrink-0"
      />
    </div>
  );
}
