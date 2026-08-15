package com.ihub.notification.listener;

import com.ihub.notification.event.AuctionCancelledEvent;
import com.ihub.notification.event.AuctionEndedEvent;
import com.ihub.notification.event.AuctionEndingSoonEvent;
import com.ihub.notification.event.AuctionStartedEvent;
import com.ihub.notification.event.OutbidEvent;
import com.ihub.notification.event.WinnerAnnouncedEvent;
import com.ihub.service.NotificationService;
import com.ihub.service.PlatformSettingsService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Turns domain events into notifications, after the transaction that raised them has
 * committed — so a rolled-back bid never produces an "outbid" alert.
 */
@Component
public class NotificationEventListener {

    private final NotificationService notificationService;
    private final PlatformSettingsService settingsService;

    public NotificationEventListener(
            NotificationService notificationService,
            PlatformSettingsService settingsService) {
        this.notificationService = notificationService;
        this.settingsService = settingsService;
    }

    /** Outbid alerts are core to the bidding experience and are never suppressed. */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOutbid(OutbidEvent event) {
        notificationService.notifyOutbid(
                event.outbidUserId(),
                event.auctionId(),
                event.ideaTitle(),
                event.newHighestBid()
        );
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAuctionStarted(AuctionStartedEvent event) {
        if (!settingsService.isAuctionStartAlertEnabled()) {
            return;
        }
        notificationService.notifyAuctionStarted(
                event.creatorId(),
                event.auctionId(),
                event.ideaTitle()
        );
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAuctionEnded(AuctionEndedEvent event) {
        if (!settingsService.isAuctionEndAlertEnabled()) {
            return;
        }
        notificationService.notifyAuctionEnded(event.creatorId(), event.auctionId(), event.ideaTitle());
        for (Long bidderId : event.bidderIds()) {
            if (!bidderId.equals(event.creatorId())) {
                notificationService.notifyAuctionEnded(bidderId, event.auctionId(), event.ideaTitle());
            }
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAuctionCancelled(AuctionCancelledEvent event) {
        notificationService.notifyAuctionCancelled(event.creatorId(), event.auctionId(), event.ideaTitle());
        for (Long bidderId : event.bidderIds()) {
            if (!bidderId.equals(event.creatorId())) {
                notificationService.notifyAuctionCancelled(bidderId, event.auctionId(), event.ideaTitle());
            }
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAuctionEndingSoon(AuctionEndingSoonEvent event) {
        notificationService.notifyAuctionEndingSoon(
                event.creatorId(), event.auctionId(), event.ideaTitle(), event.minutesRemaining());
        for (Long bidderId : event.bidderIds()) {
            if (!bidderId.equals(event.creatorId())) {
                notificationService.notifyAuctionEndingSoon(
                        bidderId, event.auctionId(), event.ideaTitle(), event.minutesRemaining());
            }
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onWinnerAnnounced(WinnerAnnouncedEvent event) {
        notificationService.notifyWinner(
                event.winnerId(),
                event.auctionId(),
                event.ideaTitle(),
                event.winningBid()
        );
        notificationService.notifyCreatorOfWinner(
                event.creatorId(),
                event.auctionId(),
                event.ideaTitle(),
                event.winningBid()
        );
    }
}
