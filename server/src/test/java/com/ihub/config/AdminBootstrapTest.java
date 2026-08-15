package com.ihub.config;

import com.ihub.dao.UserDao;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The initial-administrator seed.
 *
 * <p>Registration refuses the ADMIN role, so a fresh database needs this to be
 * administrable at all. The security-critical property is that it is inert once an
 * administrator exists — otherwise leaving the environment variables set would let
 * a restart silently reset the account's password.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AdminBootstrapTest {

    private static final String EMAIL = "founder@ihub.test";
    private static final String STRONG = "a-long-enough-password";

    @Mock
    private UserDao userDao;

    private final PasswordEncoder encoder = new BCryptPasswordEncoder();

    private AdminBootstrap bootstrap(String email, String password) {
        return new AdminBootstrap(userDao, encoder, email, password, "Founder");
    }

    @Test
    @DisplayName("seeds an administrator when the database has none")
    void seedsOnEmptyDatabase() {
        when(userDao.adminExists()).thenReturn(false);
        when(userDao.emailExists(EMAIL)).thenReturn(false);
        when(userDao.createUserWithHash(anyString(), anyString(), anyString(), anyString())).thenReturn(1L);

        bootstrap(EMAIL, STRONG).seedAdminIfMissing();

        verify(userDao).createUserWithHash(eq("Founder"), eq(EMAIL), anyString(), eq("ADMIN"));
    }

    @Test
    @DisplayName("does nothing when an administrator already exists, even with credentials still set")
    void inertOnceAdminExists() {
        when(userDao.adminExists()).thenReturn(true);

        bootstrap(EMAIL, "an-entirely-different-password").seedAdminIfMissing();

        // The security property: a restart cannot reset or re-grant an existing account.
        verify(userDao, never()).createUserWithHash(any(), any(), any(), any());
    }

    @Test
    @DisplayName("does nothing when no credentials are configured")
    void inertWithoutCredentials() {
        when(userDao.adminExists()).thenReturn(false);

        bootstrap("", "").seedAdminIfMissing();
        bootstrap(EMAIL, "").seedAdminIfMissing();
        bootstrap("", STRONG).seedAdminIfMissing();

        verify(userDao, never()).createUserWithHash(any(), any(), any(), any());
    }

    @Test
    @DisplayName("refuses a short password rather than seeding a weak administrator")
    void refusesShortPassword() {
        when(userDao.adminExists()).thenReturn(false);

        bootstrap(EMAIL, "short").seedAdminIfMissing();

        verify(userDao, never()).createUserWithHash(any(), any(), any(), any());
    }

    @Test
    @DisplayName("refuses to collide with an existing non-admin account")
    void refusesExistingEmail() {
        when(userDao.adminExists()).thenReturn(false);
        when(userDao.emailExists(EMAIL)).thenReturn(true);

        bootstrap(EMAIL, STRONG).seedAdminIfMissing();

        verify(userDao, never()).createUserWithHash(any(), any(), any(), any());
    }

    @Test
    @DisplayName("normalises the email and stores a hash, never the plaintext")
    void normalisesEmailAndHashesPassword() {
        when(userDao.adminExists()).thenReturn(false);
        when(userDao.emailExists(EMAIL)).thenReturn(false);
        when(userDao.createUserWithHash(anyString(), anyString(), anyString(), anyString()))
                .thenAnswer(inv -> {
                    String storedEmail = inv.getArgument(1);
                    String storedHash = inv.getArgument(2);
                    assertThat(storedEmail).isEqualTo(EMAIL);
                    assertThat(storedHash).isNotEqualTo(STRONG);
                    assertThat(encoder.matches(STRONG, storedHash)).isTrue();
                    return 1L;
                });

        bootstrap("  FOUNDER@IHub.Test  ", STRONG).seedAdminIfMissing();

        verify(userDao, times(1)).createUserWithHash(any(), any(), any(), any());
    }
}
