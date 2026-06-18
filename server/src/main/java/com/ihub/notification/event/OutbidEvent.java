package com.ihub.notification.event;

public record OutbidEvent(
        Long outbidUserId,
        Long auctionId,
        String ideaTitle,
        Double newHighestBid
) {
}
