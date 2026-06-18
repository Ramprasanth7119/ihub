import api from "@/lib/axios";
import type { Category } from "@/types";

export const categoryService = {
  getAll: () => api.get<Category[]>("/categories").then((r) => r.data),
};
