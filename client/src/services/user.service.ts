import api from "@/lib/axios";
import type { User } from "@/types";

export const userService = {
  getMe: () => api.get<User>("/users/me").then((r) => r.data),

  /**
   * Public profile of another user. `email` comes back `null` unless you are that
   * user or an administrator.
   */
  getById: (id: number) => api.get<User>(`/users/${id}`).then((r) => r.data),

  updateProfile: (data: { name: string }) =>
    api.put<User>("/users/me", data).then((r) => r.data),

  changePassword: (data: { currentPassword: string; newPassword: string }) =>
    api.put<void>("/users/me/password", data).then((r) => r.data),
};
