"use client";

import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { adminService } from "@/services/admin.service";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Badge } from "@/components/ui/badge";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { Search, Shield } from "lucide-react";
import { format } from "date-fns";

const ACTION_COLORS: Record<string, string> = {
  IDEA_STATUS_UPDATE: "bg-blue-100 text-blue-800",
  IDEA_DELETE: "bg-red-100 text-red-800",
  AUCTION_CREATE: "bg-green-100 text-green-800",
  AUCTION_UPDATE: "bg-yellow-100 text-yellow-800",
  AUCTION_CANCEL: "bg-red-100 text-red-800",
  AUCTION_START: "bg-green-100 text-green-800",
  AUCTION_END: "bg-purple-100 text-purple-800",
  USER_STATUS_UPDATE: "bg-orange-100 text-orange-800",
};

export default function AdminAuditLogsPage() {
  const [page, setPage] = useState(0);
  const [action, setAction] = useState<string>("");
  const [entityType, setEntityType] = useState<string>("");
  const [search, setSearch] = useState("");

  const { data: logs, isLoading } = useQuery({
    queryKey: ["admin", "audit-logs", page, action, entityType],
    queryFn: () =>
      adminService.getAuditLogs({
        page,
        size: 20,
        action: action || undefined,
        entityType: entityType || undefined,
      }),
  });

  const filteredLogs = logs?.content.filter(
    (log) =>
      log.adminName.toLowerCase().includes(search.toLowerCase()) ||
      log.action.toLowerCase().includes(search.toLowerCase()) ||
      log.details?.toLowerCase().includes(search.toLowerCase())
  ) || [];

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-3xl font-bold text-gray-900">Audit Logs</h1>
        <p className="mt-2 text-gray-600">Track all admin activities and changes</p>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Filters</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="flex gap-4">
            <div className="flex-1">
              <div className="relative">
                <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-gray-400" />
                <Input
                  placeholder="Search logs..."
                  value={search}
                  onChange={(e) => setSearch(e.target.value)}
                  className="pl-10"
                />
              </div>
            </div>
            <Select value={action} onValueChange={setAction}>
              <SelectTrigger className="w-[200px]">
                <SelectValue placeholder="Action" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="">All Actions</SelectItem>
                <SelectItem value="IDEA_STATUS_UPDATE">Idea Status Update</SelectItem>
                <SelectItem value="IDEA_DELETE">Idea Delete</SelectItem>
                <SelectItem value="AUCTION_CREATE">Auction Create</SelectItem>
                <SelectItem value="AUCTION_UPDATE">Auction Update</SelectItem>
                <SelectItem value="AUCTION_CANCEL">Auction Cancel</SelectItem>
                <SelectItem value="AUCTION_START">Auction Start</SelectItem>
                <SelectItem value="AUCTION_END">Auction End</SelectItem>
                <SelectItem value="USER_STATUS_UPDATE">User Status Update</SelectItem>
              </SelectContent>
            </Select>
            <Select value={entityType} onValueChange={setEntityType}>
              <SelectTrigger className="w-[180px]">
                <SelectValue placeholder="Entity Type" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="">All Entities</SelectItem>
                <SelectItem value="IDEA">Idea</SelectItem>
                <SelectItem value="AUCTION">Auction</SelectItem>
                <SelectItem value="USER">User</SelectItem>
              </SelectContent>
            </Select>
          </div>
        </CardContent>
      </Card>

      <Card>
        <CardContent className="p-0">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Admin</TableHead>
                <TableHead>Action</TableHead>
                <TableHead>Entity</TableHead>
                <TableHead>Details</TableHead>
                <TableHead>IP Address</TableHead>
                <TableHead>Time</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {isLoading ? (
                <TableRow>
                  <TableCell colSpan={6} className="text-center py-8">
                    Loading...
                  </TableCell>
                </TableRow>
              ) : filteredLogs.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={6} className="text-center py-8">
                    No audit logs found
                  </TableCell>
                </TableRow>
              ) : (
                filteredLogs.map((log) => (
                  <TableRow key={log.id}>
                    <TableCell>
                      <div className="flex items-center gap-2">
                        <Shield className="h-4 w-4 text-purple-600" />
                        <div>
                          <p className="font-medium">{log.adminName}</p>
                          <p className="text-sm text-gray-500">{log.adminEmail}</p>
                        </div>
                      </div>
                    </TableCell>
                    <TableCell>
                      <Badge className={ACTION_COLORS[log.action] || "bg-gray-100 text-gray-800"}>
                        {log.action.replace(/_/g, " ")}
                      </Badge>
                    </TableCell>
                    <TableCell>
                      <div>
                        <p className="font-medium">{log.entityType}</p>
                        <p className="text-sm text-gray-500">ID: {log.entityId || "—"}</p>
                      </div>
                    </TableCell>
                    <TableCell className="max-w-xs truncate">{log.details || "—"}</TableCell>
                    <TableCell className="text-sm text-gray-500">{log.ipAddress || "—"}</TableCell>
                    <TableCell>{format(new Date(log.createdAt), "MMM dd, yyyy HH:mm:ss")}</TableCell>
                  </TableRow>
                ))
              )}
            </TableBody>
          </Table>
        </CardContent>
      </Card>

      {logs && logs.totalElements > 20 && (
        <div className="flex items-center justify-between">
          <p className="text-sm text-gray-600">
            Showing {page * 20 + 1} to {Math.min((page + 1) * 20, logs.totalElements)} of {logs.totalElements} logs
          </p>
          <div className="flex gap-2">
            <Button
              variant="outline"
              onClick={() => setPage((p) => Math.max(0, p - 1))}
              disabled={page === 0}
            >
              Previous
            </Button>
            <Button
              variant="outline"
              onClick={() => setPage((p) => p + 1)}
              disabled={(page + 1) * 20 >= logs.totalElements}
            >
              Next
            </Button>
          </div>
        </div>
      )}
    </div>
  );
}
