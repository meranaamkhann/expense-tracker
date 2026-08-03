package com.asad.expensetracker.integration;

import com.asad.expensetracker.dto.auth.RegisterRequest;
import com.asad.expensetracker.exception.BadRequestException;
import com.asad.expensetracker.model.EmailVerificationToken;
import com.asad.expensetracker.model.User;
import com.asad.expensetracker.repository.EmailVerificationTokenRepository;
import com.asad.expensetracker.repository.UserRepository;
import com.asad.expensetracker.security.JwtService;
import com.asad.expensetracker.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Exercises the real service + database, simulating "the user clicked the link from their
 * email" by minting a token the same way AuthService does internally (the raw token itself
 * is never persisted anywhere retrievable — only its hash — which is the whole point).
 */
@SpringBootTest
class EmailVerificationIntegrationTest {

    @Autowired private AuthService authService;
    @Autowired private UserRepository userRepository;
    @Autowired private EmailVerificationTokenRepository tokenRepository;
    @Autowired private JwtService jwtService;

    @Test
    void newUserStartsUnverifiedAndCanBeVerifiedWithAValidToken() {
        String email = "verify-" + System.nanoTime() + "@example.com";
        var response = authService.register(new RegisterRequest("Verify Me", email, "SuperSecret123"));
        assertThat(response.user().emailVerified()).isFalse();

        User user = userRepository.findByEmailIgnoreCase(email).orElseThrow();
        String rawToken = "test-raw-token-" + System.nanoTime();
        tokenRepository.save(EmailVerificationToken.builder()
                .user(user)
                .tokenHash(jwtService.hashToken(rawToken))
                .expiresAt(Instant.now().plusSeconds(3600))
                .used(false)
                .build());

        authService.verifyEmail(rawToken);

        User verified = userRepository.findByEmailIgnoreCase(email).orElseThrow();
        assertThat(verified.isEmailVerified()).isTrue();
    }

    @Test
    void expiredTokenIsRejected() {
        String email = "expired-" + System.nanoTime() + "@example.com";
        authService.register(new RegisterRequest("Expired", email, "SuperSecret123"));
        User user = userRepository.findByEmailIgnoreCase(email).orElseThrow();

        String rawToken = "expired-token-" + System.nanoTime();
        tokenRepository.save(EmailVerificationToken.builder()
                .user(user)
                .tokenHash(jwtService.hashToken(rawToken))
                .expiresAt(Instant.now().minusSeconds(60)) // already expired
                .used(false)
                .build());

        assertThatThrownBy(() -> authService.verifyEmail(rawToken))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void usedTokenCannotBeReplayed() {
        String email = "replay-" + System.nanoTime() + "@example.com";
        authService.register(new RegisterRequest("Replay", email, "SuperSecret123"));
        User user = userRepository.findByEmailIgnoreCase(email).orElseThrow();

        String rawToken = "replay-token-" + System.nanoTime();
        tokenRepository.save(EmailVerificationToken.builder()
                .user(user)
                .tokenHash(jwtService.hashToken(rawToken))
                .expiresAt(Instant.now().plusSeconds(3600))
                .used(false)
                .build());

        authService.verifyEmail(rawToken); // first use succeeds
        assertThatThrownBy(() -> authService.verifyEmail(rawToken)) // second use must not
                .isInstanceOf(BadRequestException.class);
    }
}
