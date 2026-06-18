"use client";

import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { Search as SearchIcon } from "lucide-react";
import { PageHeader } from "@/components/shared/page-header";
import { IdeaCard } from "@/components/shared/idea-card";
import { EmptyState } from "@/components/shared/empty-state";
import { Skeleton } from "@/components/ui/skeleton";
import { Input } from "@/components/ui/input";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Button } from "@/components/ui/button";
import { searchService } from "@/services/search.service";
import { useDebounce } from "@/hooks/use-debounce";
import { SORT_OPTIONS } from "@/constants";

export default function SearchPage() {
  const [query, setQuery] = useState("");
  const [category, setCategory] = useState("all");
  const [auctionStatus, setAuctionStatus] = useState("all");
  const [sort, setSort] = useState("relevance");
  const [page, setPage] = useState(0);
  const debouncedQuery = useDebounce(query, 400);

  const { data: facets } = useQuery({
    queryKey: ["search", "facets"],
    queryFn: () => searchService.categoryFacets(),
  });

  const { data, isLoading, isFetching } = useQuery({
    queryKey: ["search", debouncedQuery, category, auctionStatus, sort, page],
    queryFn: () =>
      searchService.search({
        q: debouncedQuery || undefined,
        category: category !== "all" ? category : undefined,
        auctionStatus: auctionStatus !== "all" ? auctionStatus : undefined,
        sort: sort !== "relevance" ? sort : undefined,
        page,
        size: 12,
      }),
  });

  const suggestions = debouncedQuery.length >= 2 ? (data?.content.slice(0, 3) ?? []) : [];

  return (
    <div>
      <PageHeader
        title="Discover Ideas"
        description="Search and filter ideas powered by Elasticsearch."
      />

      <div className="relative mb-6">
        <SearchIcon className="absolute left-4 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-500" />
        <Input
          placeholder="Search ideas, categories, tags..."
          value={query}
          onChange={(e) => { setQuery(e.target.value); setPage(0); }}
          className="pl-11"
        />
        {suggestions.length > 0 && query.length >= 2 && (
          <div className="absolute z-10 mt-1 w-full rounded-xl border border-white/10 bg-slate-900 p-2 shadow-xl">
            {suggestions.map((s) => (
              <button
                key={s.id}
                className="block w-full rounded-lg px-3 py-2 text-left text-sm text-slate-300 hover:bg-white/5"
                onClick={() => setQuery(s.title)}
              >
                {s.title}
                <span className="ml-2 text-xs text-slate-500">{s.category}</span>
              </button>
            ))}
          </div>
        )}
      </div>

      <div className="mb-6 flex flex-wrap gap-3">
        <Select value={category} onValueChange={(v) => { setCategory(v); setPage(0); }}>
          <SelectTrigger className="w-44">
            <SelectValue placeholder="Category" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="all">All Categories</SelectItem>
            {facets?.map((f) => (
              <SelectItem key={f.category} value={f.category}>
                {f.category} ({f.count})
              </SelectItem>
            ))}
          </SelectContent>
        </Select>

        <Select value={auctionStatus} onValueChange={(v) => { setAuctionStatus(v); setPage(0); }}>
          <SelectTrigger className="w-40">
            <SelectValue placeholder="Auction Status" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="all">All Auctions</SelectItem>
            <SelectItem value="ACTIVE">Active</SelectItem>
            <SelectItem value="UPCOMING">Upcoming</SelectItem>
            <SelectItem value="CLOSED">Closed</SelectItem>
          </SelectContent>
        </Select>

        <Select value={sort} onValueChange={setSort}>
          <SelectTrigger className="w-44">
            <SelectValue placeholder="Sort" />
          </SelectTrigger>
          <SelectContent>
            {SORT_OPTIONS.map((o) => (
              <SelectItem key={o.value} value={o.value}>{o.label}</SelectItem>
            ))}
          </SelectContent>
        </Select>
      </div>

      {isLoading ? (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {Array.from({ length: 6 }).map((_, i) => (
            <Skeleton key={i} className="h-48" />
          ))}
        </div>
      ) : data?.content.length === 0 ? (
        <EmptyState
          icon={SearchIcon}
          title="No results found"
          description="Try different keywords or adjust your filters."
        />
      ) : (
        <>
          <p className="mb-4 text-sm text-slate-500">
            {data?.totalElements ?? 0} results {isFetching && "· Updating..."}
          </p>
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {data?.content.map((idea, i) => (
              <IdeaCard key={idea.id} idea={idea} index={i} />
            ))}
          </div>
          {data && data.totalPages > 1 && (
            <div className="mt-8 flex justify-center gap-2">
              <Button variant="secondary" disabled={page === 0} onClick={() => setPage((p) => p - 1)}>
                Previous
              </Button>
              <span className="flex items-center px-4 text-sm text-slate-400">
                Page {page + 1} of {data.totalPages}
              </span>
              <Button variant="secondary" disabled={page >= data.totalPages - 1} onClick={() => setPage((p) => p + 1)}>
                Next
              </Button>
            </div>
          )}
        </>
      )}
    </div>
  );
}
