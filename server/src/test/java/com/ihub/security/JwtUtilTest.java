package com.ihub.security;

import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtUtilTest {

    private static final String SECRET = "a-test-signing-secret-that-is-comfortably-long-enough";

    private JwtUtil util(long accessMs) {
        return new JwtUtil(SECRET, accessMs, 604_800_000L);
    }

    @Test
    @DisplayName("a short signing secret is rejected at construction, not at first use")
    void rejectsShortSecret() {
        assertThatThrownBy(() -> new JwtUtil("too-short", 3600_000L, 604_800_000L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("at least 32");
    }

    @Test
    @DisplayName("subject and role round-trip through an access token")
    void roundTripsClaims() {
        JwtUtil util = util(3600_000L);
        String token = util.generateAccessToken("user@ihub.test", "INVESTOR");

        assertThat(util.extractEmail(token)).isEqualTo("user@ihub.test");
        assertThat(util.extractRole(token)).isEqualTo("INVESTOR");
        assertThat(util.isAccessToken(token)).isTrue();
        assertThat(util.isTokenExpired(token)).isFalse();
    }

    @Test
    @DisplayName("an expired token fails to parse rather than validating")
    void rejectsExpiredToken() {
        JwtUtil util = util(-1_000L);
        String expired = util.generateAccessToken("user@ihub.test", "CREATOR");

        assertThatThrownBy(() -> util.extractEmail(expired)).isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("a token signed with a different secret is rejected")
    void rejectsForeignSignature() {
        String foreign = new JwtUtil("an-entirely-different-secret-also-long-enough", 3600_000L, 1L)
                .generateAccessToken("attacker@ihub.test", "ADMIN");

        assertThatThrownBy(() -> util(3600_000L).extractEmail(foreign))
                .isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("refresh tokens are opaque, not JWTs, so they cannot authenticate an API call")
    void refreshTokensAreOpaque() {
        JwtUtil util = util(3600_000L);
        String refresh = util.generateRefreshToken();

        assertThat(refresh).doesNotContain(".eyJ");
        assertThatThrownBy(() -> util.isAccessToken(refresh))
                .isInstanceOfAny(JwtException.class, IllegalArgumentException.class);
    }
}
