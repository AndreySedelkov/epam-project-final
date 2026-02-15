package com.epam.finaltask.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

@Component
@RequiredArgsConstructor
@Slf4j
public class EmailVerificationSchemaMigration {

    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void migrate() {
        try {
            jdbcTemplate.execute("ALTER TABLE email_verification_tokens ADD COLUMN IF NOT EXISTS token VARCHAR(255)");
            jdbcTemplate.execute("ALTER TABLE email_verification_tokens ADD COLUMN IF NOT EXISTS code VARCHAR(255)");
            jdbcTemplate.update("UPDATE email_verification_tokens SET token = code WHERE token IS NULL AND code IS NOT NULL");
            jdbcTemplate.update("UPDATE email_verification_tokens SET code = token WHERE code IS NULL AND token IS NOT NULL");
        } catch (Exception ex) {
            log.debug("Email verification schema migration skipped: {}", ex.getMessage());
        }
    }
}
