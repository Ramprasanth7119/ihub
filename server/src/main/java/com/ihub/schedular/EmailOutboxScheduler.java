package com.ihub.schedular;

import com.ihub.dao.EmailOutboxDao;
import com.ihub.notification.email.EmailService;
import com.ihub.service.PlatformSettingsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class EmailOutboxScheduler {

    private static final Logger log = LoggerFactory.getLogger(EmailOutboxScheduler.class);

    private final EmailOutboxDao emailOutboxDao;
    private final EmailService emailService;
    private final PlatformSettingsService settingsService;
    private final int batchSize;
    private final int maxAttempts;

    public EmailOutboxScheduler(
            EmailOutboxDao emailOutboxDao,
            EmailService emailService,
            PlatformSettingsService settingsService,
            @Value("${ihub.notification.outbox.batch-size:20}") int batchSize,
            @Value("${ihub.notification.outbox.max-attempts:3}") int maxAttempts) {
        this.emailOutboxDao = emailOutboxDao;
        this.emailService = emailService;
        this.settingsService = settingsService;
        this.batchSize = batchSize;
        this.maxAttempts = maxAttempts;
    }

    /**
     * Drains queued emails. Messages stay queued while delivery is disabled rather
     * than being dropped, so re-enabling it flushes the backlog.
     */
    @Scheduled(cron = "${ihub.notification.outbox.cron:0 */2 * * * *}")
    public void processOutbox() {
        if (!settingsService.isEmailDeliveryEnabled()) {
            return;
        }

        try {
            List<Map<String, Object>> pending = emailOutboxDao.findPending(batchSize, maxAttempts);

            for (Map<String, Object> row : pending) {
                Long id = ((Number) row.get("id")).longValue();
                String email = (String) row.get("recipient_email");
                String subject = (String) row.get("subject");
                String body = (String) row.get("body");

                try {
                    emailService.send(email, subject, body);
                    emailOutboxDao.markSent(id);
                } catch (Exception ex) {
                    log.warn("Failed to send email id={}: {}", id, ex.getMessage());
                    emailOutboxDao.markFailed(id, ex.getMessage(), maxAttempts);
                }
            }
        } catch (RuntimeException e) {
            // Never let a failed tick cancel the scheduled task permanently.
            log.error("Email outbox sweep failed; will retry on the next tick", e);
        }
    }
}
