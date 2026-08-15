package com.ihub.notification;

public enum NotificationType {

    OUTBID,
    AUCTION_STARTED,
    /** Sent once per auction shortly before its scheduled end time. */
    AUCTION_ENDING_SOON,
    AUCTION_ENDED,
    AUCTION_CANCELLED,
    WINNER_ANNOUNCED,
    /** Raised when an administrator approves, rejects or suspends an idea. */
    IDEA_STATUS_CHANGED
}
