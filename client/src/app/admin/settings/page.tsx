"use client";

import { useEffect, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { Switch } from "@/components/ui/switch";
import { Save, Bell, Shield, Database } from "lucide-react";
import { toast } from "sonner";
import { adminService } from "@/services/admin.service";
import {
  DEFAULT_ADMIN_SETTINGS,
  loadAdminSettings,
  saveAdminSettings,
  type AdminSettings,
} from "@/lib/admin-settings";

export default function AdminSettingsPage() {
  const [settings, setSettings] = useState<AdminSettings>(DEFAULT_ADMIN_SETTINGS);
  const [loaded, setLoaded] = useState(false);

  const { data: metrics } = useQuery({
    queryKey: ["admin", "metrics"],
    queryFn: () => adminService.getMetrics(),
  });

  useEffect(() => {
    setSettings(loadAdminSettings());
    setLoaded(true);
  }, []);

  const update = <K extends keyof AdminSettings>(key: K, value: AdminSettings[K]) => {
    setSettings((prev) => ({ ...prev, [key]: value }));
  };

  const handleSave = (section: string) => {
    saveAdminSettings(settings);
    toast.success(`${section} settings saved`);
  };

  if (!loaded) return null;

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-3xl font-bold text-gray-900">Settings</h1>
        <p className="mt-2 text-gray-600">
          Configure admin preferences. Platform stats are loaded from the live API.
        </p>
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
              General Settings
            </CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="grid gap-4 md:grid-cols-2">
              <div>
                <Label htmlFor="platform-name">Platform Name</Label>
                <Input
                  id="platform-name"
                  value={settings.platformName}
                  onChange={(e) => update("platformName", e.target.value)}
                />
              </div>
              <div>
                <Label htmlFor="support-email">Support Email</Label>
                <Input
                  id="support-email"
                  value={settings.supportEmail}
                  onChange={(e) => update("supportEmail", e.target.value)}
                />
              </div>
            </div>
            <div>
              <Label htmlFor="platform-description">Platform Description</Label>
              <Textarea
                id="platform-description"
                value={settings.platformDescription}
                onChange={(e) => update("platformDescription", e.target.value)}
                rows={3}
              />
            </div>
            <Button onClick={() => handleSave("General")}>
              <Save className="mr-2 h-4 w-4" />
              Save Changes
            </Button>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <Bell className="h-5 w-5" />
              Notification Settings
            </CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            <ToggleRow
              label="Email Notifications"
              description="Send email notifications for important events"
              checked={settings.emailNotifications}
              onChange={(v) => update("emailNotifications", v)}
            />
            <ToggleRow
              label="Idea Approval Alerts"
              description="Notify creators when ideas are approved"
              checked={settings.ideaApprovalAlerts}
              onChange={(v) => update("ideaApprovalAlerts", v)}
            />
            <ToggleRow
              label="Auction Start Alerts"
              description="Notify investors when auctions start"
              checked={settings.auctionStartAlerts}
              onChange={(v) => update("auctionStartAlerts", v)}
            />
            <ToggleRow
              label="Auction End Alerts"
              description="Notify when auctions end"
              checked={settings.auctionEndAlerts}
              onChange={(v) => update("auctionEndAlerts", v)}
            />
            <Button onClick={() => handleSave("Notification")} className="w-full">
              <Save className="mr-2 h-4 w-4" />
              Save Changes
            </Button>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <Database className="h-5 w-5" />
              Auction Settings
            </CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            <div>
              <Label htmlFor="default-min-bid">Default Minimum Bid</Label>
              <Input
                id="default-min-bid"
                type="number"
                value={settings.defaultMinBid}
                onChange={(e) => update("defaultMinBid", Number(e.target.value))}
              />
            </div>
            <div>
              <Label htmlFor="default-bid-increment">Default Bid Increment</Label>
              <Input
                id="default-bid-increment"
                type="number"
                value={settings.defaultBidIncrement}
                onChange={(e) => update("defaultBidIncrement", Number(e.target.value))}
              />
            </div>
            <div>
              <Label htmlFor="auction-duration">Default Auction Duration (hours)</Label>
              <Input
                id="auction-duration"
                type="number"
                value={settings.auctionDurationHours}
                onChange={(e) => update("auctionDurationHours", Number(e.target.value))}
              />
            </div>
            <ToggleRow
              label="Auto-start Auctions"
              description="Automatically start auctions at scheduled time"
              checked={settings.autoStartAuctions}
              onChange={(v) => update("autoStartAuctions", v)}
            />
            <ToggleRow
              label="Auto-end Auctions"
              description="Automatically end auctions at scheduled time"
              checked={settings.autoEndAuctions}
              onChange={(v) => update("autoEndAuctions", v)}
            />
            <Button onClick={() => handleSave("Auction")} className="w-full">
              <Save className="mr-2 h-4 w-4" />
              Save Changes
            </Button>
          </CardContent>
        </Card>

        <Card className="md:col-span-2">
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <Shield className="h-5 w-5" />
              Session Settings
            </CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            <ToggleRow
              label="Session Timeout"
              description="Auto-logout after inactivity"
              checked={settings.sessionTimeoutEnabled}
              onChange={(v) => update("sessionTimeoutEnabled", v)}
            />
            <div>
              <Label htmlFor="session-timeout">Session Timeout (minutes)</Label>
              <Input
                id="session-timeout"
                type="number"
                value={settings.sessionTimeoutMinutes}
                onChange={(e) => update("sessionTimeoutMinutes", Number(e.target.value))}
              />
            </div>
            <Button onClick={() => handleSave("Session")}>
              <Save className="mr-2 h-4 w-4" />
              Save Changes
            </Button>
          </CardContent>
        </Card>
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
    <div className="flex items-center justify-between">
      <div>
        <Label>{label}</Label>
        <p className="text-sm text-gray-500">{description}</p>
      </div>
      <Switch checked={checked} onClick={() => onChange(!checked)} />
    </div>
  );
}
