import { create } from "zustand";
import type { Notification } from "@/types";

interface NotificationState {
  unreadCount: number;
  liveNotifications: Notification[];
  setUnreadCount: (count: number) => void;
  addLiveNotification: (notification: Notification) => void;
  markReadLocally: (id: number) => void;
  clearLive: () => void;
}

export const useNotificationStore = create<NotificationState>((set) => ({
  unreadCount: 0,
  liveNotifications: [],

  setUnreadCount: (count) => set({ unreadCount: count }),

  addLiveNotification: (notification) =>
    set((s) => ({
      liveNotifications: [notification, ...s.liveNotifications].slice(0, 10),
      unreadCount: s.unreadCount + 1,
    })),

  markReadLocally: (id) =>
    set((s) => ({
      liveNotifications: s.liveNotifications.map((n) =>
        n.id === id ? { ...n, read: true } : n
      ),
      unreadCount: Math.max(0, s.unreadCount - 1),
    })),

  clearLive: () => set({ liveNotifications: [] }),
}));
