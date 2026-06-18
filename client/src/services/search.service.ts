import api from "@/lib/axios";
import type { CategoryFacet, SearchResponse } from "@/types";

export interface SearchParams {
  q?: string;
  category?: string;
  tags?: string;
  minBudget?: number;
  maxBudget?: number;
  auctionStatus?: string;
  sort?: string;
  page?: number;
  size?: number;
}

export const searchService = {
  search: (params: SearchParams) =>
    api.get<SearchResponse>("/search", { params }).then((r) => r.data),

  liveIdeas: (page = 0, size = 20) =>
    api.get<SearchResponse>("/search/live", { params: { page, size } }).then((r) => r.data),

  byCategory: (category: string, page = 0, size = 20) =>
    api
      .get<SearchResponse>("/search/category", { params: { category, page, size } })
      .then((r) => r.data),

  categoryFacets: () =>
    api.get<CategoryFacet[]>("/search/facets/categories").then((r) => r.data),
};
