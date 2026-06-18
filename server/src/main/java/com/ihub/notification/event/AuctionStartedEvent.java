package com.ihub.notification.event;

public record AuctionStartedEvent(
        Long auctionId,
        Long ideaId,
        Long creatorId,
        String ideaTitle
) {
}
