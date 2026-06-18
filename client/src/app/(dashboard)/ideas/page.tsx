"use client";

import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { Lightbulb } from "lucide-react";
import { PageHeader } from "@/components/shared/page-header";
import { IdeaCard } from "@/components/shared/idea-card";
import { EmptyState } from "@/components/shared/empty-state";
import { ErrorState } from "@/components/shared/error-state";
import { Skeleton } from "@/components/ui/skeleton";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { ideaService } from "@/services/idea.service";
import { categoryService } from "@/services/category.service";
import { useAuthStore } from "@/store/auth-store";
import Link from "next/link";
import { useDebounce } from "@/hooks/use-debounce";

export default function IdeasPage() {
  const role = useAuthStore((s) => s.role);
  const [search, setSearch] = useState("");
  const [category, setCategory] = useState<string>("all");
  const [status, setStatus] = useState<string>("all");
  const debouncedSearch = useDebounce(search);

  const { data: categories } = useQuery({
    queryKey: ["categories"],
    queryFn: () => categoryService.getAll(),
  });

  const { data: ideas, isLoading, isError, refetch } = useQuery({
    queryKey: ["ideas", role === "CREATOR", category, status],
    queryFn: () =>
      ideaService.getAll({
        mine: role === "CREATOR",
        category: category !== "all" ? category : undefined,
        status: status !== "all" ? status : undefined,
      }),
  });

  const filtered = ideas?.filter(
    (idea) =>
      !debouncedSearch ||
      idea.title.toLowerCase().includes(debouncedSearch.toLowerCase()) ||
      idea.description.toLowerCase().includes(debouncedSearch.toLowerCase())
  );

  return (
    <div>
      <PageHeader
        title={role === "CREATOR" ? "My Ideas" : "Browse Ideas"}
        description={role === "CREATOR" ? "Manage and track your submitted ideas." : "Explore ideas available for investment."}
        action={
          role === "CREATOR" ? (
            <Button asChild>
              <Link href="/ideas/new">New Idea</Link>
            </Button>
          ) : (
            <Button variant="secondary" asChild>
              <Link href="/search">Advanced Search</Link>
            </Button>
          )
        }
      />

      <div className="mb-6 flex flex-col gap-3 sm:flex-row">
        <Input
          placeholder="Search ideas..."
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          className="sm:max-w-xs"
        />
        <Select value={category} onValueChange={setCategory}>
          <SelectTrigger className="sm:w-44">
            <SelectValue placeholder="Category" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="all">All Categories</SelectItem>
            {categories?.map((c) => (
              <SelectItem key={c.id} value={c.slug}>{c.name}</SelectItem>
            ))}
          </SelectContent>
        </Select>
        <Select value={status} onValueChange={setStatus}>
          <SelectTrigger className="sm:w-36">
            <SelectValue placeholder="Status" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="all">All Status</SelectItem>
            <SelectItem value="DRAFT">Draft</SelectItem>
            <SelectItem value="PUBLISHED">Published</SelectItem>
          </SelectContent>
        </Select>
      </div>

      {isLoading && (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {Array.from({ length: 6 }).map((_, i) => (
            <Skeleton key={i} className="h-48" />
          ))}
        </div>
      )}

      {isError && <ErrorState onRetry={() => refetch()} />}

      {!isLoading && !isError && filtered?.length === 0 && (
        <EmptyState
          icon={Lightbulb}
          title="No ideas found"
          description={role === "CREATOR" ? "Submit your first idea to start attracting investors." : "Try adjusting your filters or search query."}
          actionLabel={role === "CREATOR" ? "Submit Idea" : undefined}
          onAction={role === "CREATOR" ? () => window.location.assign("/ideas/new") : undefined}
        />
      )}

      {filtered && filtered.length > 0 && (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {filtered.map((idea, i) => (
            <IdeaCard key={idea.id} idea={idea} index={i} />
          ))}
        </div>
      )}
    </div>
  );
}
