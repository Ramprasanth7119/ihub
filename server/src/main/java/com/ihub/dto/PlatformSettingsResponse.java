package com.ihub.dto;

import lombok.Data;

/** Current platform settings, mirroring {@link PlatformSettingsRequest}. */
@Data
public class PlatformSettingsResponse {

    private String platformName;
    private String supportEmail;
    private String platformDescription;
    private boolean emailNotifications;
    private boolean ideaApprovalAlerts;
    private boolean auctionStartAlerts;
    private boolean auctionEndAlerts;
    private double defaultMinBid;
    private double defaultBidIncrement;
    private int auctionDurationHours;
    private boolean autoStartAuctions;
    private boolean autoEndAuctions;
    private int sessionTimeoutMinutes;
    private boolean sessionTimeoutEnabled;
}
