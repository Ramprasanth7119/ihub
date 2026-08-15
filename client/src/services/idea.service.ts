import api from "@/lib/axios";
import type { Idea } from "@/types";

export interface IdeaFilters {
  status?: string;
  category?: string;
  minBudget?: number;
  maxBudget?: number;
  mine?: boolean;
  page?: number;
  size?: number;
}

export const ideaService = {
  /**
   * The response body is a plain array; the backend bounds the page and reports
   * the totals in `X-Total-Count` / `X-Page` / `X-Page-Size` headers.
   */
  getAll: (filters?: IdeaFilters) =>
    api.get<Idea[]>("/ideas", { params: filters }).then((r) => r.data),

  /** Same request, but also returning the pagination metadata from the headers. */
  getPage: (filters?: IdeaFilters) =>
    api.get<Idea[]>("/ideas", { params: filters }).then((r) => ({
      content: r.data,
      totalElements: Number(r.headers["x-total-count"] ?? r.data.length),
      page: Number(r.headers["x-page"] ?? 0),
      size: Number(r.headers["x-page-size"] ?? r.data.length),
      totalPages: Number(r.headers["x-total-pages"] ?? 1),
    })),

  getById: (id: number) => api.get<Idea>(`/ideas/${id}`).then((r) => r.data),

  create: (data: {
    title: string;
    description: string;
    category: string;
    basePrice: number;
    maxBudget?: number;
    tags?: string[];
  }) => api.post<Idea>("/ideas", data).then((r) => r.data),

  update: (
    id: number,
    data: {
      title?: string;
      description?: string;
      category?: string;
      basePrice?: number;
      maxBudget?: number;
      tags?: string[];
    }
  ) => api.put<Idea>(`/ideas/${id}`, data).then((r) => r.data),

  publish: (id: number) => api.post<Idea>(`/ideas/${id}/publish`).then((r) => r.data),

  delete: (id: number) => api.delete(`/ideas/${id}`),
};
