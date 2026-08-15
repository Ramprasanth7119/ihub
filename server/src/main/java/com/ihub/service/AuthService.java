package com.ihub.service;

import com.ihub.dao.AuthDao;
import com.ihub.dao.RefreshTokenDao;
import com.ihub.dto.AuthResponse;
import com.ihub.dto.LoginRequest;
import com.ihub.dto.RefreshTokenRequest;
import com.ihub.exception.ForbiddenException;
import com.ihub.exception.UnauthorizedException;
import com.ihub.security.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Map;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final AuthDao authDao;
    private final RefreshTokenDao refreshTokenDao;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    public AuthService(
            AuthDao authDao,
            RefreshTokenDao refreshTokenDao,
            JwtUtil jwtUtil,
            PasswordEncoder passwordEncoder) {
        this.authDao = authDao;
        this.refreshTokenDao = refreshTokenDao;
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        String email = request.getEmail() != null ? request.getEmail().trim().toLowerCase(Locale.ROOT) : "";
        Map<String, Object> user = authDao.getUserByEmail(email);

        // Both branches return the same message so the response cannot be used to
        // discover which email addresses are registered.
        if (user == null) {
            log.debug("Login attempt for unknown account");
            throw new UnauthorizedException("Invalid email or password");
        }

        String dbPassword = (String) user.get("password");
        if (!passwordEncoder.matches(request.getPassword(), dbPassword)) {
            log.debug("Login attempt with bad password for user {}", user.get("id"));
            throw new UnauthorizedException("Invalid email or password");
        }

        // MySQL reports BOOLEAN as TINYINT, which the driver may hand back as a
        // Boolean or a Number depending on the column definition — accept both
        // rather than silently letting suspended accounts through.
        if (!isActive(user.get("active"))) {
            throw new ForbiddenException("Your account has been suspended. Please contact support.");
        }

        return issueTokens(
                ((Number) user.get("id")).longValue(),
                (String) user.get("email"),
                (String) user.get("role")
        );
    }

    /**
     * Exchanges a refresh token for a fresh pair.
     *
     * <p>Tokens are rotated: the presented token is revoked before a new one is
     * issued, so a stolen token is single-use.</p>
     */
    @Transactional
    public AuthResponse refresh(RefreshTokenRequest request) {
        Map<String, Object> stored = refreshTokenDao.findValidToken(request.getRefreshToken());

        if (stored == null) {
            throw new UnauthorizedException("Invalid or expired refresh token");
        }

        refreshTokenDao.revoke(request.getRefreshToken());

        return issueTokens(
                ((Number) stored.get("user_id")).longValue(),
                (String) stored.get("email"),
                (String) stored.get("role")
        );
    }

    @Transactional
    public void logout(RefreshTokenRequest request) {
        // Idempotent by design: logging out twice, or with a token that was already
        // rotated away, is not an error.
        refreshTokenDao.revoke(request.getRefreshToken());
    }

    /** Purges tokens that expired or were revoked more than a week ago. */
    @Transactional
    public int purgeExpiredRefreshTokens() {
        return refreshTokenDao.deleteExpired();
    }

    private boolean isActive(Object value) {
        if (value == null) {
            return true;
        }
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value instanceof Number number) {
            return number.intValue() != 0;
        }
        return true;
    }

    private AuthResponse issueTokens(Long userId, String email, String role) {
        String accessToken = jwtUtil.generateAccessToken(email, role);
        String refreshToken = jwtUtil.generateRefreshToken();

        refreshTokenDao.save(
                userId,
                refreshToken,
                LocalDateTime.now().plusSeconds(jwtUtil.getRefreshExpirationMs() / 1000)
        );

        return new AuthResponse(
                accessToken,
                refreshToken,
                jwtUtil.getAccessExpirationMs() / 1000,
                role
        );
    }
}
