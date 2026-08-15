package com.ihub.schedular;

import com.ihub.service.AuctionLifecycleService;
import com.ihub.service.AuthService;
import com.ihub.service.PlatformSettingsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class AuctionScheduler {

    private static final Logger log = LoggerFactory.getLogger(AuctionScheduler.class);

    private final AuctionLifecycleService lifecycleService;
    private final PlatformSettingsService settingsService;
    private final AuthService authService;

    public AuctionScheduler(
            AuctionLifecycleService lifecycleService,
            PlatformSettingsService settingsService,
            AuthService authService) {
        this.lifecycleService = lifecycleService;
        this.settingsService = settingsService;
        this.authService = authService;
    }

    /**
     * Polls every minute for auctions due to start or close.
     *
     * <p>Wrapped in a catch-all: an uncaught exception here would kill the scheduled
     * task for the remaining lifetime of the JVM, silently freezing every auction on
     * the platform. One bad tick should not stop the next one.</p>
     */
    @Scheduled(cron = "${auction.cron.lifecycle:0 */1 * * * *}")
    public void processAuctionLifecycle() {
        try {
            // Both toggles are administrator-controlled; when one is disabled that
            // transition is driven manually from the admin console instead.
            lifecycleService.processDueAuctions(
                    settingsService.isAutoStartEnabled(),
                    settingsService.isAutoEndEnabled());
        } catch (RuntimeException e) {
            log.error("Auction lifecycle sweep failed; will retry on the next tick", e);
        }
    }

    /** Nightly cleanup of refresh tokens that can no longer be exchanged. */
    @Scheduled(cron = "${auction.cron.token-cleanup:0 30 3 * * *}")
    public void purgeExpiredRefreshTokens() {
        try {
            int removed = authService.purgeExpiredRefreshTokens();
            if (removed > 0) {
                log.info("Purged {} expired or revoked refresh tokens", removed);
            }
        } catch (RuntimeException e) {
            log.error("Refresh token cleanup failed", e);
        }
    }
}
