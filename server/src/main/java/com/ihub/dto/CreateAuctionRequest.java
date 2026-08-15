package com.ihub.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Admin-side auction creation payload.
 *
 * <p>{@code minBid} and {@code minBidIncrement} are deliberately distinct:
 * {@code minBid} is the starting/reserve price recorded in {@code auction_settings},
 * while {@code minBidIncrement} is how much each new bid must exceed the current
 * highest by. They were previously conflated, so a "minimum bid" of 50 000 became a
 * 50 000 required increment between bids.</p>
 *
 * <p>{@code startTime} is intentionally not constrained to the future — an admin
 * creating an auction that should open immediately sets it to now.</p>
 */
@Data
public class CreateAuctionRequest {

    @NotNull(message = "Idea ID is required")
    private Long ideaId;

    @NotNull(message = "Start time is required")
    private LocalDateTime startTime;

    @NotNull(message = "End time is required")
    @Future(message = "End time must be in the future")
    private LocalDateTime endTime;

    /** Starting / reserve price shown to investors. */
    @DecimalMin(value = "0.01", message = "Minimum bid must be at least 0.01")
    private Double minBid;

    /** Required gap between consecutive bids; falls back to the platform default when absent. */
    @DecimalMin(value = "0.01", message = "Minimum bid increment must be at least 0.01")
    private Double minBidIncrement;

    @DecimalMin(value = "0.01", message = "Reserve price must be at least 0.01")
    private Double reservePrice;

    private String description;
}
