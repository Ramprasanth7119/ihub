package com.ihub.config;

import com.ihub.dao.UserDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

/**
 * Seeds the first administrator on an otherwise empty platform.
 *
 * <p>Self-service registration deliberately refuses the ADMIN role, so a freshly
 * migrated database has no administrator and no way to create one — the console
 * would be unreachable on a new deployment. This closes that gap without weakening
 * the registration rule.</p>
 *
 * <p>It is inert unless explicitly configured, and it never modifies an existing
 * account: if any administrator already exists it does nothing, so leaving the
 * variables set across restarts cannot reset a password or re-grant access.</p>
 */
@Component
public class AdminBootstrap {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

    /** Seeded accounts skip the interactive flow, so hold them to a longer password. */
    private static final int MIN_PASSWORD_LENGTH = 12;

    private final UserDao userDao;
    private final PasswordEncoder passwordEncoder;
    private final String email;
    private final String password;
    private final String name;

    public AdminBootstrap(
            UserDao userDao,
            PasswordEncoder passwordEncoder,
            @Value("${ihub.bootstrap.admin.email:}") String email,
            @Value("${ihub.bootstrap.admin.password:}") String password,
            @Value("${ihub.bootstrap.admin.name:Platform Administrator}") String name) {
        this.userDao = userDao;
        this.passwordEncoder = passwordEncoder;
        this.email = email;
        this.password = password;
        this.name = name;
    }

    /**
     * Entry point. Seeding is a convenience, never a startup requirement, so any
     * failure here is logged and swallowed — a broken seed must not crash-loop a
     * service whose API and database are otherwise healthy.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        try {
            seedAdminIfMissing();
        } catch (RuntimeException e) {
            log.error("Administrator bootstrap failed; the application is running but may have "
                    + "no administrator account", e);
        }
    }

    @Transactional
    public void seedAdminIfMissing() {
        if (userDao.adminExists()) {
            // Normal steady state — say nothing on every boot.
            return;
        }

        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            log.warn("No administrator account exists and no bootstrap credentials are configured. "
                    + "Set IHUB_ADMIN_EMAIL and IHUB_ADMIN_PASSWORD, restart once, then unset them. "
                    + "Without an administrator the admin console cannot be reached.");
            return;
        }

        if (password.length() < MIN_PASSWORD_LENGTH) {
            log.error("Refusing to seed the administrator: IHUB_ADMIN_PASSWORD must be at least {} "
                    + "characters. No account was created.", MIN_PASSWORD_LENGTH);
            return;
        }

        String normalisedEmail = email.trim().toLowerCase(Locale.ROOT);
        if (userDao.emailExists(normalisedEmail)) {
            log.error("Refusing to seed the administrator: {} already exists as a non-admin account. "
                    + "Choose a different IHUB_ADMIN_EMAIL.", normalisedEmail);
            return;
        }

        Long id = userDao.createUserWithHash(
                name, normalisedEmail, passwordEncoder.encode(password), "ADMIN");

        log.info("Seeded the initial administrator (id={}, email={}). "
                + "Sign in, change the password, then remove IHUB_ADMIN_PASSWORD from the environment.",
                id, normalisedEmail);
    }
}
