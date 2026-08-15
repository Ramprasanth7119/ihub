package com.ihub.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Platform-wide settings editable from the admin console.
 *
 * <p>Several of these change real behaviour rather than being cosmetic:
 * {@code defaultBidIncrement} seeds new auctions, {@code autoStartAuctions} /
 * {@code autoEndAuctions} gate the lifecycle scheduler, {@code emailNotifications}
 * gates the outbox drain, and the alert toggles gate individual notification types.</p>
 */
@Data
public class PlatformSettingsRequest {

    @NotBlank(message = "Platform name is required")
    @Size(max = 100, message = "Platform name must be at most 100 characters")
    private String platformName;

    @NotBlank(message = "Support email is required")
    @Email(message = "Support email must be a valid address")
    @Size(max = 150)
    private String supportEmail;

    @Size(max = 1000, message = "Description must be at most 1000 characters")
    private String platformDescription;

    @NotNull
    private Boolean emailNotifications;

    @NotNull
    private Boolean ideaApprovalAlerts;

    @NotNull
    private Boolean auctionStartAlerts;

    @NotNull
    private Boolean auctionEndAlerts;

    @NotNull
    @DecimalMin(value = "0.01", message = "Default minimum bid must be at least 0.01")
    private Double defaultMinBid;

    @NotNull
    @DecimalMin(value = "0.01", message = "Default bid increment must be at least 0.01")
    private Double defaultBidIncrement;

    @NotNull
    @Min(value = 1, message = "Auction duration must be at least 1 hour")
    @Max(value = 8760, message = "Auction duration must be at most one year")
    private Integer auctionDurationHours;

    @NotNull
    private Boolean autoStartAuctions;

    @NotNull
    private Boolean autoEndAuctions;

    @NotNull
    @Min(value = 5, message = "Session timeout must be at least 5 minutes")
    @Max(value = 1440, message = "Session timeout must be at most 24 hours")
    private Integer sessionTimeoutMinutes;

    @NotNull
    private Boolean sessionTimeoutEnabled;
}
