package com.ihub.service;

import com.ihub.dao.PlatformSettingsDao;
import com.ihub.dto.PlatformSettingsRequest;
import com.ihub.dto.PlatformSettingsResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Reads and writes platform settings, caching the assembled view in memory.
 *
 * <p>Settings are consulted on hot paths — every scheduler tick and every auction
 * creation — so the map is loaded once and refreshed on write rather than hitting
 * the database each time. Defaults come from {@code application.yaml}, so a fresh
 * install behaves sensibly before an admin ever opens the settings page.</p>
 */
@Service
public class PlatformSettingsService {

    private static final Logger log = LoggerFactory.getLogger(PlatformSettingsService.class);

    private static final String KEY_PLATFORM_NAME = "platformName";
    private static final String KEY_SUPPORT_EMAIL = "supportEmail";
    private static final String KEY_PLATFORM_DESCRIPTION = "platformDescription";
    private static final String KEY_EMAIL_NOTIFICATIONS = "emailNotifications";
    private static final String KEY_IDEA_APPROVAL_ALERTS = "ideaApprovalAlerts";
    private static final String KEY_AUCTION_START_ALERTS = "auctionStartAlerts";
    private static final String KEY_AUCTION_END_ALERTS = "auctionEndAlerts";
    private static final String KEY_DEFAULT_MIN_BID = "defaultMinBid";
    private static final String KEY_DEFAULT_BID_INCREMENT = "defaultBidIncrement";
    private static final String KEY_AUCTION_DURATION_HOURS = "auctionDurationHours";
    private static final String KEY_AUTO_START_AUCTIONS = "autoStartAuctions";
    private static final String KEY_AUTO_END_AUCTIONS = "autoEndAuctions";
    private static final String KEY_SESSION_TIMEOUT_MINUTES = "sessionTimeoutMinutes";
    private static final String KEY_SESSION_TIMEOUT_ENABLED = "sessionTimeoutEnabled";

    private final PlatformSettingsDao settingsDao;
    private final double configuredMinBidIncrement;
    private final AtomicReference<PlatformSettingsResponse> cache = new AtomicReference<>();

    public PlatformSettingsService(
            PlatformSettingsDao settingsDao,
            @Value("${auction.default-min-bid-increment:100}") double configuredMinBidIncrement) {
        this.settingsDao = settingsDao;
        this.configuredMinBidIncrement = configuredMinBidIncrement;
    }

    public PlatformSettingsResponse getSettings() {
        PlatformSettingsResponse cached = cache.get();
        if (cached != null) {
            return cached;
        }

        PlatformSettingsResponse loaded = load();
        cache.set(loaded);
        return loaded;
    }

    @Transactional
    public PlatformSettingsResponse updateSettings(PlatformSettingsRequest request, Long adminId) {
        Map<String, String> values = new LinkedHashMap<>();
        values.put(KEY_PLATFORM_NAME, request.getPlatformName());
        values.put(KEY_SUPPORT_EMAIL, request.getSupportEmail());
        values.put(KEY_PLATFORM_DESCRIPTION, request.getPlatformDescription());
        values.put(KEY_EMAIL_NOTIFICATIONS, String.valueOf(request.getEmailNotifications()));
        values.put(KEY_IDEA_APPROVAL_ALERTS, String.valueOf(request.getIdeaApprovalAlerts()));
        values.put(KEY_AUCTION_START_ALERTS, String.valueOf(request.getAuctionStartAlerts()));
        values.put(KEY_AUCTION_END_ALERTS, String.valueOf(request.getAuctionEndAlerts()));
        values.put(KEY_DEFAULT_MIN_BID, String.valueOf(request.getDefaultMinBid()));
        values.put(KEY_DEFAULT_BID_INCREMENT, String.valueOf(request.getDefaultBidIncrement()));
        values.put(KEY_AUCTION_DURATION_HOURS, String.valueOf(request.getAuctionDurationHours()));
        values.put(KEY_AUTO_START_AUCTIONS, String.valueOf(request.getAutoStartAuctions()));
        values.put(KEY_AUTO_END_AUCTIONS, String.valueOf(request.getAutoEndAuctions()));
        values.put(KEY_SESSION_TIMEOUT_MINUTES, String.valueOf(request.getSessionTimeoutMinutes()));
        values.put(KEY_SESSION_TIMEOUT_ENABLED, String.valueOf(request.getSessionTimeoutEnabled()));

        settingsDao.saveAll(values, adminId);
        cache.set(null);

        PlatformSettingsResponse refreshed = load();
        cache.set(refreshed);
        log.info("Platform settings updated by admin {}", adminId);
        return refreshed;
    }

    /** Increment applied to auctions created without an explicit one. */
    public double getDefaultBidIncrement() {
        return getSettings().getDefaultBidIncrement();
    }

    public boolean isAutoStartEnabled() {
        return getSettings().isAutoStartAuctions();
    }

    public boolean isAutoEndEnabled() {
        return getSettings().isAutoEndAuctions();
    }

    public boolean isEmailDeliveryEnabled() {
        return getSettings().isEmailNotifications();
    }

    public boolean isIdeaApprovalAlertEnabled() {
        return getSettings().isIdeaApprovalAlerts();
    }

    public boolean isAuctionStartAlertEnabled() {
        return getSettings().isAuctionStartAlerts();
    }

    public boolean isAuctionEndAlertEnabled() {
        return getSettings().isAuctionEndAlerts();
    }

    private PlatformSettingsResponse load() {
        Map<String, String> stored;
        try {
            stored = settingsDao.findAll();
        } catch (RuntimeException e) {
            // Never let a settings read take the platform down; fall back to defaults.
            log.error("Failed to load platform settings, using defaults", e);
            stored = Map.of();
        }

        PlatformSettingsResponse response = new PlatformSettingsResponse();
        response.setPlatformName(stringOr(stored, KEY_PLATFORM_NAME, "iHub"));
        response.setSupportEmail(stringOr(stored, KEY_SUPPORT_EMAIL, "support@ihub.com"));
        response.setPlatformDescription(stringOr(stored, KEY_PLATFORM_DESCRIPTION,
                "iHub is an idea auction platform where creators publish ideas and investors bid on them."));
        response.setEmailNotifications(boolOr(stored, KEY_EMAIL_NOTIFICATIONS, true));
        response.setIdeaApprovalAlerts(boolOr(stored, KEY_IDEA_APPROVAL_ALERTS, true));
        response.setAuctionStartAlerts(boolOr(stored, KEY_AUCTION_START_ALERTS, true));
        response.setAuctionEndAlerts(boolOr(stored, KEY_AUCTION_END_ALERTS, true));
        response.setDefaultMinBid(doubleOr(stored, KEY_DEFAULT_MIN_BID, configuredMinBidIncrement));
        response.setDefaultBidIncrement(doubleOr(stored, KEY_DEFAULT_BID_INCREMENT, configuredMinBidIncrement));
        response.setAuctionDurationHours(intOr(stored, KEY_AUCTION_DURATION_HOURS, 24));
        response.setAutoStartAuctions(boolOr(stored, KEY_AUTO_START_AUCTIONS, true));
        response.setAutoEndAuctions(boolOr(stored, KEY_AUTO_END_AUCTIONS, true));
        response.setSessionTimeoutMinutes(intOr(stored, KEY_SESSION_TIMEOUT_MINUTES, 30));
        response.setSessionTimeoutEnabled(boolOr(stored, KEY_SESSION_TIMEOUT_ENABLED, true));
        return response;
    }

    private String stringOr(Map<String, String> stored, String key, String fallback) {
        String value = stored.get(key);
        return value != null && !value.isBlank() ? value : fallback;
    }

    private boolean boolOr(Map<String, String> stored, String key, boolean fallback) {
        String value = stored.get(key);
        return value != null ? Boolean.parseBoolean(value) : fallback;
    }

    private double doubleOr(Map<String, String> stored, String key, double fallback) {
        try {
            String value = stored.get(key);
            return value != null ? Double.parseDouble(value) : fallback;
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private int intOr(Map<String, String> stored, String key, int fallback) {
        try {
            String value = stored.get(key);
            return value != null ? Integer.parseInt(value) : fallback;
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
