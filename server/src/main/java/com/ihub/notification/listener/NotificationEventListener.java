package com.ihub.notification.listener;

import com.ihub.notification.event.AuctionEndedEvent;
import com.ihub.notification.event.AuctionStartedEvent;
import com.ihub.notification.event.OutbidEvent;
import com.ihub.notification.event.WinnerAnnouncedEvent;
import com.ihub.service.NotificationService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class NotificationEventListener {

    private final NotificationService notificationService;

    public NotificationEventListener(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOutbid(OutbidEvent event) {
        notificationService.notifyOutbid(
                event.outbidUserId(),
                event.auctionId(),
                event.ideaTitle(),
                event.newHighestBid()
        );
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAuctionStarted(AuctionStartedEvent event) {
        notificationService.notifyAuctionStarted(
                event.creatorId(),
                event.auctionId(),
                event.ideaTitle()
        );
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAuctionEnded(AuctionEndedEvent event) {
        notificationService.notifyAuctionEnded(event.creatorId(), event.auctionId(), event.ideaTitle());
        for (Long bidderId : event.bidderIds()) {
            if (!bidderId.equals(event.creatorId())) {
                notificationService.notifyAuctionEnded(bidderId, event.auctionId(), event.ideaTitle());
            }
        }
    }

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
