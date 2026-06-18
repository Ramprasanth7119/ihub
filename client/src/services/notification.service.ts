import api from "@/lib/axios";
import type { NotificationPage } from "@/types";

export const notificationService = {
  getAll: (unreadOnly = false, page = 0, size = 20) =>
    api
      .get<NotificationPage>("/notifications", { params: { unreadOnly, page, size } })
      .then((r) => r.data),

  getUnreadCount: () =>
    api.get<{ count: number }>("/notifications/unread-count").then((r) => r.data.count),

  markAsRead: (id: number) => api.patch(`/notifications/${id}/read`),

  markAllAsRead: () => api.patch("/notifications/read-all"),
};
