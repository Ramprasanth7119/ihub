"use client";

import { useEffect } from "react";
import { wsService } from "@/services/websocket.service";
import { useNotificationStore } from "@/store/notification-store";
import { useAuthStore } from "@/store/auth-store";

export function useNotificationsSocket() {
  const userId = useAuthStore((s) => s.user?.id);
  const { setUnreadCount, addLiveNotification } = useNotificationStore();

  useEffect(() => {
    if (!userId) return;

    let unsubNotifications: (() => void) | undefined;
    let unsubCount: (() => void) | undefined;

    wsService.connect(() => {
      unsubNotifications = wsService.subscribeUserNotifications(userId, addLiveNotification);
      unsubCount = wsService.subscribeUnreadCount(userId, setUnreadCount);
    });

    return () => {
      unsubNotifications?.();
      unsubCount?.();
    };
  }, [userId, setUnreadCount, addLiveNotification]);
}
