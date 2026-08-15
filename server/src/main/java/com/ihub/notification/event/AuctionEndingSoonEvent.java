package com.ihub.notification.event;

import java.util.List;

/**
 * Raised once per auction when it enters its final stretch, so participants get a
 * chance to place a closing bid. Emitted by the lifecycle scheduler.
 */
public record AuctionEndingSoonEvent(
        Long auctionId,
        Long ideaId,
        Long creatorId,
        String ideaTitle,
        List<Long> bidderIds,
        long minutesRemaining
) {
}
