"use client";

import { useState } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
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
import { Search, Ban, CheckCircle } from "lucide-react";
import { format } from "date-fns";
import { toast } from "sonner";

const ROLE_COLORS: Record<string, string> = {
  ADMIN: "bg-purple-100 text-purple-800",
  CREATOR: "bg-blue-100 text-blue-800",
  INVESTOR: "bg-green-100 text-green-800",
};

export default function AdminUsersPage() {
  const [page, setPage] = useState(0);
  const [role, setRole] = useState<string>("ALL");
const [active, setActive] = useState<string>("ALL");
  const [search, setSearch] = useState("");
  const queryClient = useQueryClient();

  const { data: users, isLoading } = useQuery({
    queryKey: ["admin", "users", page, role, active],
    queryFn: () =>
  adminService.getUsers({
    page,
    size: 20,
    role: role !== "ALL" ? role : undefined,
    active: active !== "ALL" ? active === "true" : undefined,
  }),
  });

  const updateStatusMutation = useMutation({
    mutationFn: ({ id, active }: { id: number; active: boolean }) => adminService.updateUserStatus(id, active),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["admin", "users"] });
      toast.success("User status updated successfully");
    },
    onError: () => {
      toast.error("Failed to update user status");
    },
  });

  const handleToggleStatus = (id: number, currentActive: boolean) => {
    if (confirm(`Are you sure you want to ${currentActive ? "disable" : "enable"} this user?`)) {
      updateStatusMutation.mutate({ id, active: !currentActive });
    }
  };

  const filteredUsers = users?.content.filter(
    (user) =>
      user.name.toLowerCase().includes(search.toLowerCase()) ||
      user.email.toLowerCase().includes(search.toLowerCase())
  ) || [];

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-3xl font-bold text-gray-900">Users</h1>
        <p className="mt-2 text-gray-600">Manage platform users and their access</p>
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
                  placeholder="Search users..."
                  value={search}
                  onChange={(e) => setSearch(e.target.value)}
                  className="pl-10"
                />
              </div>
            </div>
           
           <Select value={role} onValueChange={setRole}>
  <SelectTrigger className="w-[180px]">
    <SelectValue placeholder="Role" />
  </SelectTrigger>
  <SelectContent>
    <SelectItem value="ALL">All Roles</SelectItem>
    <SelectItem value="ADMIN">Admin</SelectItem>
    <SelectItem value="CREATOR">Creator</SelectItem>
    <SelectItem value="INVESTOR">Investor</SelectItem>
  </SelectContent>
</Select>

<Select value={active} onValueChange={setActive}>
  <SelectTrigger className="w-[180px]">
    <SelectValue placeholder="Status" />
  </SelectTrigger>
  <SelectContent>
    <SelectItem value="ALL">All Status</SelectItem>
    <SelectItem value="true">Active</SelectItem>
    <SelectItem value="false">Disabled</SelectItem>
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
                <TableHead>Name</TableHead>
                <TableHead>Email</TableHead>
                <TableHead>Role</TableHead>
                <TableHead>Status</TableHead>
                <TableHead>Verified</TableHead>
                <TableHead>Ideas</TableHead>
                <TableHead>Bids</TableHead>
                <TableHead>Joined</TableHead>
                <TableHead>Actions</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {isLoading ? (
                <TableRow>
                  <TableCell colSpan={9} className="text-center py-8">
                    Loading...
                  </TableCell>
                </TableRow>
              ) : filteredUsers.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={9} className="text-center py-8">
                    No users found
                  </TableCell>
                </TableRow>
              ) : (
                filteredUsers.map((user) => (
                  <TableRow key={user.id}>
                    <TableCell className="font-medium">{user.name}</TableCell>
                    <TableCell>{user.email}</TableCell>
                    <TableCell>
                      <Badge className={ROLE_COLORS[user.role] || "bg-gray-100 text-gray-800"}>
                        {user.role}
                      </Badge>
                    </TableCell>
                    <TableCell>
                      <Badge variant={user.active ? "success" : "warning"}>
                        {user.active ? "Active" : "Disabled"}
                      </Badge>
                    </TableCell>
                    <TableCell>
                      {user.verified ? (
                        <CheckCircle className="h-4 w-4 text-green-600" />
                      ) : (
                        <span className="text-gray-400">—</span>
                      )}
                    </TableCell>
                    <TableCell>{user.ideaCount}</TableCell>
                    <TableCell>{user.bidCount}</TableCell>
                    <TableCell>{format(new Date(user.createdAt), "MMM dd, yyyy")}</TableCell>
                    <TableCell>
                      <Button
                        variant="ghost"
                        size="icon"
                        onClick={() => handleToggleStatus(user.id, user.active)}
                      >
                        {user.active ? (
                          <Ban className="h-4 w-4 text-red-600" />
                        ) : (
                          <CheckCircle className="h-4 w-4 text-green-600" />
                        )}
                      </Button>
                    </TableCell>
                  </TableRow>
                ))
              )}
            </TableBody>
          </Table>
        </CardContent>
      </Card>

      {users && users.totalElements > 20 && (
        <div className="flex items-center justify-between">
          <p className="text-sm text-gray-600">
            Showing {page * 20 + 1} to {Math.min((page + 1) * 20, users.totalElements)} of {users.totalElements} users
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
              disabled={(page + 1) * 20 >= users.totalElements}
            >
              Next
            </Button>
          </div>
        </div>
      )}
    </div>
  );
}
