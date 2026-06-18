package com.ihub.service;

import com.ihub.dto.NotificationResponse;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class NotificationBroadcastService {

    private final SimpMessagingTemplate messagingTemplate;

    public NotificationBroadcastService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void sendToUser(Long userId, NotificationResponse notification) {
        messagingTemplate.convertAndSend("/topic/user/" + userId + "/notifications", notification);
    }

    public void sendUnreadCount(Long userId, long unreadCount) {
        messagingTemplate.convertAndSend("/topic/user/" + userId + "/notifications/count", unreadCount);
    }
}
