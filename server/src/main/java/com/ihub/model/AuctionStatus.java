package com.ihub.model;

/**
 * Auction lifecycle states.
 *
 * <p>The persisted vocabulary is {@code SCHEDULED → ACTIVE → CLOSED}, with
 * {@code CANCELLED} reachable from either non-terminal state. {@code UPCOMING} is
 * kept only as an inbound alias for {@code SCHEDULED}: earlier admin code accepted
 * it and clients may still send it, but it is never written to the database.</p>
 */
public enum AuctionStatus {

    SCHEDULED,
    ACTIVE,
    CLOSED,
    CANCELLED,
    /** Client-facing alias for {@link #SCHEDULED}; never persisted. */
    UPCOMING;

    /** Case-insensitive comparison against a raw status column value. */
    public boolean matches(String value) {
        if (value == null) {
            return false;
        }
        if (this == SCHEDULED && UPCOMING.name().equalsIgnoreCase(value)) {
            return true;
        }
        return name().equalsIgnoreCase(value);
    }

    /** Normalises a caller-supplied status, mapping the {@code UPCOMING} alias onto {@code SCHEDULED}. */
    public static AuctionStatus normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        for (AuctionStatus status : values()) {
            if (status.name().equalsIgnoreCase(value.trim())) {
                return status == UPCOMING ? SCHEDULED : status;
            }
        }
        return null;
    }
}
