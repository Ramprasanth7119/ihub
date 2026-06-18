package com.ihub.notification.event;

public record WinnerAnnouncedEvent(
        Long auctionId,
        Long ideaId,
        Long creatorId,
        Long winnerId,
        String ideaTitle,
        Double winningBid
) {
}
