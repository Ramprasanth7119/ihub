package com.ihub.service;

import com.ihub.dao.EmailOutboxDao;
import com.ihub.dao.NotificationDao;
import com.ihub.dao.UserDao;
import com.ihub.dto.NotificationPageResponse;
import com.ihub.dto.NotificationResponse;
import com.ihub.exception.NotFoundException;
import com.ihub.exception.UnauthorizedException;
import com.ihub.model.Notification;
import com.ihub.model.User;
import com.ihub.notification.NotificationType;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class NotificationService {

    private static final String REF_AUCTION = "AUCTION";

    private final NotificationDao notificationDao;
    private final EmailOutboxDao emailOutboxDao;
    private final UserDao userDao;
    private final NotificationBroadcastService broadcastService;

    public NotificationService(
            NotificationDao notificationDao,
            EmailOutboxDao emailOutboxDao,
            UserDao userDao,
            NotificationBroadcastService broadcastService) {
        this.notificationDao = notificationDao;
        this.emailOutboxDao = emailOutboxDao;
        this.userDao = userDao;
        this.broadcastService = broadcastService;
    }

    @Transactional
    public NotificationResponse notifyUser(
            Long userId,
            NotificationType type,
            String title,
            String message,
            String referenceType,
            Long referenceId) {

        Long id = notificationDao.insert(userId, type, title, message, referenceType, referenceId);
        Notification saved = notificationDao.findByIdAndUserId(id, userId).orElseThrow();

        NotificationResponse response = toResponse(saved);
        broadcastService.sendToUser(userId, response);
        broadcastService.sendUnreadCount(userId, notificationDao.countByUserId(userId, true));

        queueEmail(userId, title, message);
        return response;
    }

    public NotificationPageResponse getNotifications(boolean unreadOnly, int page, int size) {
        User user = getAuthenticatedUser();
        int offset = page * size;

        List<NotificationResponse> content = notificationDao
                .findByUserId(user.getId(), unreadOnly, size, offset)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        return new NotificationPageResponse(
                content,
                notificationDao.countByUserId(user.getId(), false),
                notificationDao.countByUserId(user.getId(), true),
                page,
                size
        );
    }

    public long getUnreadCount() {
        return notificationDao.countByUserId(getAuthenticatedUser().getId(), true);
    }

    @Transactional
    public void markAsRead(Long notificationId) {
        User user = getAuthenticatedUser();
        // Scoped by user id, so one user can never mark another user's notification read.
        int updated = notificationDao.markAsRead(notificationId, user.getId());
        if (updated == 0) {
            throw new NotFoundException("Notification not found");
        }
        broadcastService.sendUnreadCount(user.getId(), notificationDao.countByUserId(user.getId(), true));
    }

    @Transactional
    public void markAllAsRead() {
        User user = getAuthenticatedUser();
        notificationDao.markAllAsRead(user.getId());
        broadcastService.sendUnreadCount(user.getId(), 0);
    }

    public void notifyOutbid(Long userId, Long auctionId, String ideaTitle, Double newHighestBid) {
        notifyUser(
                userId,
                NotificationType.OUTBID,
                "You have been outbid",
                String.format("A new bid of %.2f was placed on \"%s\" (auction #%d). Place a higher bid to stay in the lead.",
                        newHighestBid, ideaTitle, auctionId),
                REF_AUCTION,
                auctionId
        );
    }

    public void notifyAuctionStarted(Long creatorId, Long auctionId, String ideaTitle) {
        notifyUser(
                creatorId,
                NotificationType.AUCTION_STARTED,
                "Your auction has started",
                String.format("The auction for \"%s\" (auction #%d) is now live and accepting bids.",
                        ideaTitle, auctionId),
                REF_AUCTION,
                auctionId
        );
    }

    public void notifyAuctionEnded(Long userId, Long auctionId, String ideaTitle) {
        notifyUser(
                userId,
                NotificationType.AUCTION_ENDED,
                "Auction ended",
                String.format("The auction for \"%s\" (auction #%d) has closed.",
                        ideaTitle, auctionId),
                REF_AUCTION,
                auctionId
        );
    }

    public void notifyAuctionEndingSoon(Long userId, Long auctionId, String ideaTitle, long minutesRemaining) {
        notifyUser(
                userId,
                NotificationType.AUCTION_ENDING_SOON,
                "Auction ending soon",
                String.format("The auction for \"%s\" (auction #%d) closes in about %d minute%s.",
                        ideaTitle, auctionId, minutesRemaining, minutesRemaining == 1 ? "" : "s"),
                REF_AUCTION,
                auctionId
        );
    }

    public void notifyAuctionCancelled(Long userId, Long auctionId, String ideaTitle) {
        notifyUser(
                userId,
                NotificationType.AUCTION_CANCELLED,
                "Auction cancelled",
                String.format("The auction for \"%s\" (auction #%d) has been cancelled. Any bids placed are void.",
                        ideaTitle, auctionId),
                REF_AUCTION,
                auctionId
        );
    }

    public void notifyIdeaStatusChanged(Long creatorId, Long ideaId, String ideaTitle, String status, String reason) {
        String detail = reason != null && !reason.isBlank() ? " Reason: " + reason : "";
        notifyUser(
                creatorId,
                NotificationType.IDEA_STATUS_CHANGED,
                "Your idea was " + status.toLowerCase(),
                String.format("The status of \"%s\" was changed to %s by a platform administrator.%s",
                        ideaTitle, status, detail),
                "IDEA",
                ideaId
        );
    }

    public void notifyWinner(Long winnerId, Long auctionId, String ideaTitle, Double winningBid) {
        notifyUser(
                winnerId,
                NotificationType.WINNER_ANNOUNCED,
                "Congratulations — you won!",
                String.format("You won the auction for \"%s\" with a bid of %.2f (auction #%d).",
                        ideaTitle, winningBid, auctionId),
                REF_AUCTION,
                auctionId
        );
    }

    public void notifyCreatorOfWinner(Long creatorId, Long auctionId, String ideaTitle, Double winningBid) {
        notifyUser(
                creatorId,
                NotificationType.WINNER_ANNOUNCED,
                "Winner selected for your auction",
                String.format("A winner was selected for \"%s\" with a winning bid of %.2f (auction #%d).",
                        ideaTitle, winningBid, auctionId),
                REF_AUCTION,
                auctionId
        );
    }

    private void queueEmail(Long userId, String subject, String body) {
        try {
            User user = userDao.getUserById(userId);
            if (user.getEmail() != null && !user.getEmail().isBlank()) {
                emailOutboxDao.enqueue(user.getEmail(), subject, body);
            }
        } catch (EmptyResultDataAccessException ignored) {
            // user removed
        }
    }

    private User getAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new UnauthorizedException("Authentication required");
        }
        User user = userDao.findByEmail(auth.getName());
        if (user == null) {
            throw new UnauthorizedException("Authenticated user no longer exists");
        }
        return user;
    }

    private NotificationResponse toResponse(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getType(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getReferenceType(),
                notification.getReferenceId(),
                notification.isReadFlag(),
                notification.getCreatedAt()
        );
    }
}
