package com.asad.expensetracker.service;

import com.asad.expensetracker.dto.auth.AuthResponse;
import com.asad.expensetracker.dto.auth.LoginRequest;
import com.asad.expensetracker.dto.auth.RegisterRequest;
import com.asad.expensetracker.exception.BadRequestException;
import com.asad.expensetracker.exception.DuplicateResourceException;
import com.asad.expensetracker.exception.UnauthorizedException;
import com.asad.expensetracker.mapper.Mappers;
import com.asad.expensetracker.model.PasswordResetToken;
import com.asad.expensetracker.model.Role;
import com.asad.expensetracker.model.User;
import com.asad.expensetracker.repository.PasswordResetTokenRepository;
import com.asad.expensetracker.repository.UserRepository;
import com.asad.expensetracker.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final long RESET_TOKEN_TTL_MINUTES = 30;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final CategoryService categoryService;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final MailService mailService;

    @Value("${app.mail.frontend-reset-url}")
    private String frontendResetUrl;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase();
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new DuplicateResourceException("An account with this email already exists");
        }

        User user = User.builder()
                .name(request.name().trim())
                .email(email)
                .password(passwordEncoder.encode(request.password()))
                .role(Role.USER)
                .enabled(true)
                .build();

        user = userRepository.save(user);
        categoryService.seedDefaultCategories(user);

        log.info("Registered new user id={}", user.getId());
        return issueTokens(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        String email = request.email().trim().toLowerCase();

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, request.password()));
        } catch (Exception ex) {
            throw new BadCredentialsException("Invalid email or password");
        }

        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        return issueTokens(user);
    }

    @Transactional
    public AuthResponse refresh(String refreshToken) {
        if (!jwtService.isTokenValid(refreshToken, "refresh")) {
            throw new UnauthorizedException("Refresh token is invalid or expired");
        }

        Long userId = jwtService.extractUserId(refreshToken);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("Refresh token is invalid or expired"));

        String presentedHash = jwtService.hashToken(refreshToken);
        if (user.getRefreshTokenHash() == null
                || !user.getRefreshTokenHash().equals(presentedHash)
                || user.getRefreshTokenExpiry() == null
                || user.getRefreshTokenExpiry().isBefore(Instant.now())) {
            throw new UnauthorizedException("Refresh token has already been used or revoked");
        }

        // Rotate: the old refresh token becomes invalid the moment a new pair is issued.
        return issueTokens(user);
    }

    @Transactional
    public void logout(Long userId) {
        userRepository.findById(userId).ifPresent(user -> {
            user.setRefreshTokenHash(null);
            user.setRefreshTokenExpiry(null);
            userRepository.save(user);
        });
    }

    private AuthResponse issueTokens(User user) {
        String accessToken = jwtService.generateAccessToken(user.getId(), user.getEmail(), user.getRole().name());
        String refreshToken = jwtService.generateRefreshToken(user.getId(), user.getEmail());

        user.setRefreshTokenHash(jwtService.hashToken(refreshToken));
        user.setRefreshTokenExpiry(Instant.now().plusMillis(jwtService.getRefreshTokenTtlMs()));
        userRepository.save(user);

        return new AuthResponse(accessToken, refreshToken, Mappers.toUserResponse(user));
    }

    /**
     * Always succeeds from the caller's point of view (even for unknown emails) so this endpoint
     * can't be used to enumerate which addresses have accounts.
     */
    @Transactional
    public void forgotPassword(String email) {
        userRepository.findByEmailIgnoreCase(email.trim().toLowerCase()).ifPresent(user -> {
            String rawToken = generateSecureToken();
            PasswordResetToken resetToken = PasswordResetToken.builder()
                    .user(user)
                    .tokenHash(jwtService.hashToken(rawToken))
                    .expiresAt(Instant.now().plusSeconds(RESET_TOKEN_TTL_MINUTES * 60))
                    .used(false)
                    .build();
            passwordResetTokenRepository.save(resetToken);

            String link = frontendResetUrl + "?token=" + rawToken;
            mailService.sendPasswordResetEmail(user.getEmail(), link);
            log.info("Password reset requested for user id={}", user.getId());
        });
    }

    @Transactional
    public void resetPassword(String rawToken, String newPassword) {
        String tokenHash = jwtService.hashToken(rawToken);
        PasswordResetToken resetToken = passwordResetTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new BadRequestException("This reset link is invalid or has expired"));

        if (resetToken.isUsed() || resetToken.getExpiresAt().isBefore(Instant.now())) {
            throw new BadRequestException("This reset link is invalid or has expired");
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        // Revoke any existing session so old refresh tokens stop working once the password changes.
        user.setRefreshTokenHash(null);
        user.setRefreshTokenExpiry(null);
        userRepository.save(user);

        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);

        log.info("Password reset completed for user id={}", user.getId());
    }

    private String generateSecureToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
