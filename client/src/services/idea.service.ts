import api from "@/lib/axios";
import type { Idea } from "@/types";

export interface IdeaFilters {
  status?: string;
  category?: string;
  minBudget?: number;
  maxBudget?: number;
  mine?: boolean;
}

export const ideaService = {
  getAll: (filters?: IdeaFilters) =>
    api.get<Idea[]>("/ideas", { params: filters }).then((r) => r.data),

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
