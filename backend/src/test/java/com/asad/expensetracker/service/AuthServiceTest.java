package com.asad.expensetracker.service;

import com.asad.expensetracker.dto.auth.LoginRequest;
import com.asad.expensetracker.dto.auth.RegisterRequest;
import com.asad.expensetracker.exception.AccountLockedException;
import com.asad.expensetracker.exception.DuplicateResourceException;
import com.asad.expensetracker.model.User;
import com.asad.expensetracker.repository.EmailVerificationTokenRepository;
import com.asad.expensetracker.repository.PasswordResetTokenRepository;
import com.asad.expensetracker.repository.RefreshTokenRepository;
import com.asad.expensetracker.repository.UserRepository;
import com.asad.expensetracker.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private CategoryService categoryService;
    @Mock private PasswordResetTokenRepository passwordResetTokenRepository;
    @Mock private EmailVerificationTokenRepository emailVerificationTokenRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private MailService mailService;

    @InjectMocks
    private AuthService authService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "frontendResetUrl", "http://localhost:3000/reset-password");
        ReflectionTestUtils.setField(authService, "frontendVerifyUrl", "http://localhost:3000/verify-email");
    }

    @Test
    void registerRejectsDuplicateEmail() {
        when(userRepository.existsByEmailIgnoreCase("taken@example.com")).thenReturn(true);

        RegisterRequest request = new RegisterRequest("Asad", "taken@example.com", "password123");

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(DuplicateResourceException.class);

        verify(userRepository, never()).save(any());
    }

    @Test
    void registerCreatesUserSeedsCategoriesSendsVerificationAndIssuesTokens() {
        when(userRepository.existsByEmailIgnoreCase("new@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            if (u.getId() == null) u.setId(1L);
            return u;
        });
        when(jwtService.generateAccessToken(anyLong(), anyString(), anyString())).thenReturn("access-token");
        when(jwtService.generateRefreshToken(anyLong(), anyString())).thenReturn("refresh-token");
        when(jwtService.hashToken(anyString())).thenReturn("hashed-value");
        when(jwtService.getRefreshTokenTtlMs()).thenReturn(604_800_000L);

        var response = authService.register(new RegisterRequest("New User", "new@example.com", "password123"));

        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isEqualTo("refresh-token");
        assertThat(response.user().email()).isEqualTo("new@example.com");
        assertThat(response.user().emailVerified()).isFalse();
        verify(categoryService).seedDefaultCategories(any(User.class));
        verify(emailVerificationTokenRepository).save(any());
        verify(refreshTokenRepository).save(any());
        verify(mailService).sendVerificationEmail(eq("new@example.com"), contains("http://localhost:3000/verify-email"));
    }

    @Test
    void forgotPasswordDoesNotErrorForUnknownEmail() {
        when(userRepository.findByEmailIgnoreCase("ghost@example.com")).thenReturn(Optional.empty());

        authService.forgotPassword("ghost@example.com");

        verify(passwordResetTokenRepository, never()).save(any());
        verify(mailService, never()).sendPasswordResetEmail(anyString(), anyString());
    }

    @Test
    void resendVerificationSkipsAlreadyVerifiedUsers() {
        User verified = User.builder().id(2L).name("Verified").email("verified@example.com").emailVerified(true).build();
        when(userRepository.findByEmailIgnoreCase("verified@example.com")).thenReturn(Optional.of(verified));

        authService.resendVerification("verified@example.com");

        verify(emailVerificationTokenRepository, never()).save(any());
        verify(mailService, never()).sendVerificationEmail(anyString(), anyString());
    }

    @Test
    void loginRejectsWhenAccountIsCurrentlyLocked() {
        User locked = User.builder().id(3L).email("locked@example.com")
                .lockedUntil(Instant.now().plus(10, ChronoUnit.MINUTES)).build();
        when(userRepository.findByEmailIgnoreCase("locked@example.com")).thenReturn(Optional.of(locked));

        assertThatThrownBy(() -> authService.login(new LoginRequest("locked@example.com", "whatever")))
                .isInstanceOf(AccountLockedException.class);

        verifyNoInteractions(authenticationManager);
    }

    @Test
    void loginLocksAccountAfterFiveFailedAttempts() {
        User user = User.builder().id(4L).email("target@example.com").failedLoginAttempts(4).build();
        when(userRepository.findByEmailIgnoreCase("target@example.com")).thenReturn(Optional.of(user));
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("bad"));

        assertThatThrownBy(() -> authService.login(new LoginRequest("target@example.com", "wrong")))
                .isInstanceOf(BadCredentialsException.class);

        assertThat(user.getFailedLoginAttempts()).isEqualTo(5);
        assertThat(user.getLockedUntil()).isNotNull().isAfter(Instant.now());
        verify(userRepository).save(user);
    }

    @Test
    void successfulLoginResetsFailedAttemptCounter() {
        User user = User.builder().id(5L).email("ok@example.com").failedLoginAttempts(2).build();
        when(userRepository.findByEmailIgnoreCase("ok@example.com")).thenReturn(Optional.of(user));
        when(jwtService.generateAccessToken(anyLong(), anyString(), anyString())).thenReturn("access-token");
        when(jwtService.generateRefreshToken(anyLong(), anyString())).thenReturn("refresh-token");
        when(jwtService.hashToken(anyString())).thenReturn("hashed-value");
        when(jwtService.getRefreshTokenTtlMs()).thenReturn(604_800_000L);

        authService.login(new LoginRequest("ok@example.com", "correct-password"));

        assertThat(user.getFailedLoginAttempts()).isZero();
        assertThat(user.getLockedUntil()).isNull();
    }
}
