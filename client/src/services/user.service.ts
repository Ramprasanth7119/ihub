import api from "@/lib/axios";
import type { User } from "@/types";

export const userService = {
  getMe: () => api.get<User>("/users/me").then((r) => r.data),

  getById: (id: number) => api.get<User>(`/users/${id}`).then((r) => r.data),
};
