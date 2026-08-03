package com.asad.expensetracker.service;

import com.asad.expensetracker.repository.EmailVerificationTokenRepository;
import com.asad.expensetracker.repository.PasswordResetTokenRepository;
import com.asad.expensetracker.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Expired/used/revoked tokens otherwise just accumulate in the database forever. Runs once a
 * day; timing isn't sensitive since these rows are already unusable — this is housekeeping,
 * not a security control.
 */
@Service
@RequiredArgsConstructor
public class TokenCleanupService {

    private static final Logger log = LoggerFactory.getLogger(TokenCleanupService.class);

    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    @Scheduled(cron = "0 30 3 * * *") // 3:30am server time, daily
    @Transactional
    public void purgeExpiredTokens() {
        Instant now = Instant.now();
        int refreshDeleted = refreshTokenRepository.deleteExpiredOrRevoked(now);
        int resetDeleted = passwordResetTokenRepository.deleteExpiredOrUsed(now);
        int verifyDeleted = emailVerificationTokenRepository.deleteExpiredOrUsed(now);

        if (refreshDeleted + resetDeleted + verifyDeleted > 0) {
            log.info("Token cleanup: removed {} refresh, {} password-reset, {} verification tokens",
                    refreshDeleted, resetDeleted, verifyDeleted);
        }
    }
}
