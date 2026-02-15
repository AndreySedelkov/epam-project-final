package com.epam.finaltask.service;

import com.epam.finaltask.dto.TelegramAuthRequest;
import com.epam.finaltask.model.AuthProvider;
import com.epam.finaltask.model.Role;
import com.epam.finaltask.model.User;
import com.epam.finaltask.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TelegramAuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.telegram.bot-token:}")
    private String botToken;

    @Value("${app.telegram.auth-max-age-seconds:86400}")
    private long authMaxAgeSeconds;

    public User authenticate(TelegramAuthRequest request) {
        if (botToken == null || botToken.isBlank()) {
            throw new IllegalStateException("Telegram bot token is not configured");
        }
        if (request.getId() == null
                || request.getAuthDate() == null
                || request.getHash() == null
                || request.getHash().isBlank()) {
            throw new IllegalArgumentException("Invalid Telegram auth payload");
        }
        if (!isSignatureValid(request)) {
            throw new IllegalArgumentException("Invalid Telegram auth signature");
        }
        if (authMaxAgeSeconds > 0 && request.getAuthDate() != null) {
            long now = Instant.now().getEpochSecond();
            if (now - request.getAuthDate() > authMaxAgeSeconds) {
                throw new IllegalArgumentException("Telegram auth expired");
            }
        }

        String email = buildTelegramEmail(request.getId());
        User user = userRepository.findByEmail(email)
                .orElseGet(() -> createUser(request, email));

        if (!user.isAccountNonLocked() || user.isLocked() || !user.isActive()) {
            throw new IllegalArgumentException("User account is locked or inactive");
        }

        return user;
    }

    private boolean isSignatureValid(TelegramAuthRequest request) {
        String dataCheckString = buildDataCheckString(request);
        String expectedHash = hmacSha256Hex(sha256(botToken), dataCheckString);
        return expectedHash.equalsIgnoreCase(request.getHash());
    }

    private String buildDataCheckString(TelegramAuthRequest request) {
        Map<String, String> data = new TreeMap<>();
        data.put("id", String.valueOf(request.getId()));
        putIfNotBlank(data, "first_name", request.getFirstName());
        putIfNotBlank(data, "last_name", request.getLastName());
        putIfNotBlank(data, "username", request.getUsername());
        putIfNotBlank(data, "photo_url", request.getPhotoUrl());
        if (request.getAuthDate() != null) {
            data.put("auth_date", String.valueOf(request.getAuthDate()));
        }

        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, String> entry : data.entrySet()) {
            if (builder.length() > 0) {
                builder.append('\n');
            }
            builder.append(entry.getKey()).append('=').append(entry.getValue());
        }
        return builder.toString();
    }

    private void putIfNotBlank(Map<String, String> data, String key, String value) {
        if (value != null && !value.isBlank()) {
            data.put(key, value);
        }
    }

    private String buildTelegramEmail(Long id) {
        return "tg-" + id + "@telegram.local";
    }

    private User createUser(TelegramAuthRequest request, String email) {
        String baseUsername = request.getUsername() != null && !request.getUsername().isBlank()
                ? request.getUsername()
                : "tg-" + request.getId();
        String username = buildUniqueUsername(baseUsername);
        String firstName = request.getFirstName() != null && !request.getFirstName().isBlank()
                ? request.getFirstName()
                : "Telegram";
        String lastName = request.getLastName() != null && !request.getLastName().isBlank()
                ? request.getLastName()
                : "User";

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setName(firstName);
        user.setSurname(lastName);
        user.setRole(Role.USER);
        user.setActive(true);
        user.setLocked(false);
        user.setFailedAttempts(0);
        user.setBalance(BigDecimal.ZERO);
        user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
        user.setPhoneNumber(null);
        user.setAuthProvider(AuthProvider.TELEGRAM);
        return userRepository.save(user);
    }

    private String buildUniqueUsername(String baseUsername) {
        String sanitized = baseUsername.replaceAll("[^a-zA-Z0-9._-]", "");
        if (sanitized.isBlank()) {
            sanitized = "telegram";
        }
        String candidate = sanitized;
        if (userRepository.existsByUsername(candidate)) {
            candidate = sanitized + "-" + UUID.randomUUID().toString().substring(0, 8);
        }
        while (userRepository.existsByUsername(candidate)) {
            candidate = sanitized + "-" + UUID.randomUUID().toString().substring(0, 8);
        }
        return candidate;
    }

    private byte[] sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private String hmacSha256Hex(byte[] key, String message) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            byte[] result = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
            return toHex(result);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to calculate HMAC", e);
        }
    }

    private String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
