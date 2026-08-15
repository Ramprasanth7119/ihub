package com.ihub.notification.event;

import java.util.List;

/**
 * Raised when an auction is cancelled before reaching a natural close. Delivered to
 * the idea's creator and to every investor who had already placed a bid.
 */
public record AuctionCancelledEvent(
        Long auctionId,
        Long ideaId,
        Long creatorId,
        String ideaTitle,
        List<Long> bidderIds
) {
}
