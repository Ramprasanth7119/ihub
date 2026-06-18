package com.ihub.notification.event;

import java.util.List;

public record AuctionEndedEvent(
        Long auctionId,
        Long ideaId,
        Long creatorId,
        String ideaTitle,
        List<Long> bidderIds
) {
}
