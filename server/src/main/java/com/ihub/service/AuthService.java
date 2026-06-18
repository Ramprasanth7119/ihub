package com.ihub.service;

import com.ihub.dao.AuthDao;
import com.ihub.dao.RefreshTokenDao;
import com.ihub.dto.AuthResponse;
import com.ihub.dto.LoginRequest;
import com.ihub.dto.RefreshTokenRequest;
import com.ihub.exception.CustomException;
import com.ihub.security.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

@Service
public class AuthService {

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
        Map<String, Object> user = authDao.getUserByEmail(request.getEmail());

        if (user == null) {
            throw new CustomException("Invalid credentials");
        }

        String dbPassword = (String) user.get("password");

        if (!passwordEncoder.matches(request.getPassword(), dbPassword)) {
            throw new CustomException("Invalid credentials");
        }

        if (Boolean.FALSE.equals(user.get("active"))) {
            throw new CustomException("Account is suspended");
        }

        return issueTokens(
                ((Number) user.get("id")).longValue(),
                (String) user.get("email"),
                (String) user.get("role")
        );
    }

    @Transactional
    public AuthResponse refresh(RefreshTokenRequest request) {
        Map<String, Object> stored = refreshTokenDao.findValidToken(request.getRefreshToken());

        if (stored == null) {
            throw new CustomException("Invalid or expired refresh token");
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
        refreshTokenDao.revoke(request.getRefreshToken());
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
