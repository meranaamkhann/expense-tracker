package com.asad.expensetracker.service;

import com.asad.expensetracker.dto.auth.AuthResponse;
import com.asad.expensetracker.dto.auth.LoginRequest;
import com.asad.expensetracker.dto.auth.RegisterRequest;
import com.asad.expensetracker.exception.AccountLockedException;
import com.asad.expensetracker.exception.BadRequestException;
import com.asad.expensetracker.exception.DuplicateResourceException;
import com.asad.expensetracker.exception.UnauthorizedException;
import com.asad.expensetracker.mapper.Mappers;
import com.asad.expensetracker.model.EmailVerificationToken;
import com.asad.expensetracker.model.PasswordResetToken;
import com.asad.expensetracker.model.RefreshToken;
import com.asad.expensetracker.model.Role;
import com.asad.expensetracker.model.User;
import com.asad.expensetracker.repository.EmailVerificationTokenRepository;
import com.asad.expensetracker.repository.PasswordResetTokenRepository;
import com.asad.expensetracker.repository.RefreshTokenRepository;
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
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final long RESET_TOKEN_TTL_MINUTES = 30;
    private static final long VERIFY_TOKEN_TTL_HOURS = 24;
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final long LOCKOUT_MINUTES = 15;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final CategoryService categoryService;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final MailService mailService;

    @Value("${app.mail.frontend-reset-url}")
    private String frontendResetUrl;

    @Value("${app.mail.frontend-verify-url}")
    private String frontendVerifyUrl;

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
                .emailVerified(false)
                .build();

        user = userRepository.save(user);
        categoryService.seedDefaultCategories(user);
        sendVerificationEmail(user);

        log.info("Registered new user id={}", user.getId());
        return issueTokens(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        String email = request.email().trim().toLowerCase();
        Optional<User> maybeUser = userRepository.findByEmailIgnoreCase(email);

        maybeUser.ifPresent(this::rejectIfLocked);

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, request.password()));
        } catch (Exception ex) {
            maybeUser.ifPresent(this::recordFailedAttempt);
            throw new BadCredentialsException("Invalid email or password");
        }

        User user = maybeUser.orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        if (user.getFailedLoginAttempts() > 0 || user.getLockedUntil() != null) {
            user.setFailedLoginAttempts(0);
            user.setLockedUntil(null);
            userRepository.save(user);
        }

        return issueTokens(user);
    }

    private void rejectIfLocked(User user) {
        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(Instant.now())) {
            long minutesLeft = Duration.between(Instant.now(), user.getLockedUntil()).toMinutes() + 1;
            throw new AccountLockedException(
                    "Too many failed login attempts. Try again in about " + minutesLeft + " minute(s).");
        }
    }

    private void recordFailedAttempt(User user) {
        int attempts = user.getFailedLoginAttempts() + 1;
        user.setFailedLoginAttempts(attempts);
        if (attempts >= MAX_FAILED_ATTEMPTS) {
            user.setLockedUntil(Instant.now().plus(LOCKOUT_MINUTES, ChronoUnit.MINUTES));
            log.warn("User id={} locked for {} minutes after {} failed login attempts", user.getId(), LOCKOUT_MINUTES, attempts);
        }
        userRepository.save(user);
    }

    /**
     * Rotates the presented refresh token: the old one is revoked and a new access/refresh pair
     * is issued, scoped to this same session. Other active sessions (other devices/browsers) are
     * untouched — that's the whole point of storing one row per session instead of one per user.
     */
    @Transactional
    public AuthResponse refresh(String rawRefreshToken) {
        if (!jwtService.isTokenValid(rawRefreshToken, "refresh")) {
            throw new UnauthorizedException("Refresh token is invalid or expired");
        }

        String tokenHash = jwtService.hashToken(rawRefreshToken);
        RefreshToken stored = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new UnauthorizedException("Refresh token has already been used or revoked"));

        if (stored.isRevoked() || stored.getExpiresAt().isBefore(Instant.now())) {
            throw new UnauthorizedException("Refresh token has already been used or revoked");
        }

        stored.setRevoked(true);
        refreshTokenRepository.save(stored);

        return issueTokens(stored.getUser());
    }

    /** Logs out one session if a refresh token is provided, otherwise every session for this user. */
    @Transactional
    public void logout(Long userId, String rawRefreshToken) {
        if (rawRefreshToken != null && !rawRefreshToken.isBlank()) {
            String tokenHash = jwtService.hashToken(rawRefreshToken);
            refreshTokenRepository.findByTokenHash(tokenHash).ifPresent(token -> {
                token.setRevoked(true);
                refreshTokenRepository.save(token);
            });
        } else {
            refreshTokenRepository.revokeAllForUser(userId);
        }
    }

    private AuthResponse issueTokens(User user) {
        String accessToken = jwtService.generateAccessToken(user.getId(), user.getEmail(), user.getRole().name());
        String refreshToken = jwtService.generateRefreshToken(user.getId(), user.getEmail());

        RefreshToken tokenRow = RefreshToken.builder()
                .user(user)
                .tokenHash(jwtService.hashToken(refreshToken))
                .expiresAt(Instant.now().plusMillis(jwtService.getRefreshTokenTtlMs()))
                .revoked(false)
                .build();
        refreshTokenRepository.save(tokenRow);

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
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        userRepository.save(user);

        // Revoke every active session so old refresh tokens stop working once the password changes.
        refreshTokenRepository.revokeAllForUser(user.getId());

        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);

        log.info("Password reset completed for user id={}", user.getId());
    }

    @Transactional
    public void verifyEmail(String rawToken) {
        String tokenHash = jwtService.hashToken(rawToken);
        EmailVerificationToken verificationToken = emailVerificationTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new BadRequestException("This verification link is invalid or has expired"));

        if (verificationToken.isUsed() || verificationToken.getExpiresAt().isBefore(Instant.now())) {
            throw new BadRequestException("This verification link is invalid or has expired");
        }

        User user = verificationToken.getUser();
        user.setEmailVerified(true);
        userRepository.save(user);

        verificationToken.setUsed(true);
        emailVerificationTokenRepository.save(verificationToken);

        log.info("Email verified for user id={}", user.getId());
    }

    /** No-op (but still returns success) for unknown emails or already-verified accounts, for the same reason as forgotPassword. */
    @Transactional
    public void resendVerification(String email) {
        userRepository.findByEmailIgnoreCase(email.trim().toLowerCase())
                .filter(user -> !user.isEmailVerified())
                .ifPresent(this::sendVerificationEmail);
    }

    private void sendVerificationEmail(User user) {
        String rawToken = generateSecureToken();
        EmailVerificationToken token = EmailVerificationToken.builder()
                .user(user)
                .tokenHash(jwtService.hashToken(rawToken))
                .expiresAt(Instant.now().plusSeconds(VERIFY_TOKEN_TTL_HOURS * 3600))
                .used(false)
                .build();
        emailVerificationTokenRepository.save(token);

        String link = frontendVerifyUrl + "?token=" + rawToken;
        mailService.sendVerificationEmail(user.getEmail(), link);
    }

    private String generateSecureToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
