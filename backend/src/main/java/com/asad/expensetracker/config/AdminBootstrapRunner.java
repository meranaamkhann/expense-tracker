package com.asad.expensetracker.config;

import com.asad.expensetracker.model.Role;
import com.asad.expensetracker.model.User;
import com.asad.expensetracker.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

/**
 * Promotes accounts to ROLE_ADMIN on startup based on the ADMIN_EMAILS env var
 * (comma-separated). This is intentionally the only way to create an admin — no self-service
 * "become admin" endpoint exists, since that would be a privilege-escalation hole.
 *
 * To make yourself an admin: register normally, then restart the backend with
 * ADMIN_EMAILS=you@example.com set. The account is promoted the next time it starts up.
 */
@Component
@RequiredArgsConstructor
public class AdminBootstrapRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrapRunner.class);

    private final UserRepository userRepository;

    @Value("${app.security.admin-emails:}")
    private String adminEmailsRaw;

    @Override
    @Transactional
    public void run(String... args) {
        List<String> emails = Arrays.stream(adminEmailsRaw.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .map(String::toLowerCase)
                .toList();

        for (String email : emails) {
            userRepository.findByEmailIgnoreCase(email).ifPresentOrElse(user -> {
                if (user.getRole() != Role.ADMIN) {
                    user.setRole(Role.ADMIN);
                    userRepository.save(user);
                    log.info("Promoted {} to ROLE_ADMIN", email);
                }
            }, () -> log.warn("ADMIN_EMAILS listed {} but no account with that email exists yet — " +
                    "register the account first, then restart the backend.", email));
        }
    }
}
