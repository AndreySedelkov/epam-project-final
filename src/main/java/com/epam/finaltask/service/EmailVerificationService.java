package com.epam.finaltask.service;

import com.epam.finaltask.model.AuthProvider;
import com.epam.finaltask.model.EmailVerificationToken;
import com.epam.finaltask.model.User;
import com.epam.finaltask.repository.EmailVerificationTokenRepository;
import com.epam.finaltask.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private final EmailVerificationTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    @Value("${app.verificationToken.expirationMinutes:1440}")
    private long verificationTokenExpirationMinutes;

    @Transactional
    public void sendVerificationEmail(User user) {
        tokenRepository.deleteByUser(user);

        String code = String.format("%06d", ThreadLocalRandom.current().nextInt(0, 1_000_000));
        LocalDateTime expiry = LocalDateTime.now().plusMinutes(verificationTokenExpirationMinutes);
        tokenRepository.save(new EmailVerificationToken(user, code, expiry));

        String subject = "Confirm your account";
        String body = "Your email verification code: " + code
                + "\n\nEnter this code on the sign-in page."
                + "\n\nIf you did not create this account, ignore this message.";
        emailService.sendEmail(user.getEmail(), subject, body);
    }

    @Transactional
    public void sendVerificationCodeByUsername(String username) {
        User user = getLocalUnverifiedUser(username);
        var existingToken = tokenRepository.findByUser(user);
        if (existingToken.isPresent()) {
            if (LocalDateTime.now().isBefore(existingToken.get().getExpiryDate())) {
                return;
            }
            tokenRepository.delete(existingToken.get());
        }
        sendVerificationEmail(user);
    }

    @Transactional
    public void resendVerificationCodeByUsername(String username) {
        User user = getLocalUnverifiedUser(username);
        sendVerificationEmail(user);
    }

    @Transactional
    public void confirmEmailByCode(String username, String code) {
        User user = userRepository.findUserByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        EmailVerificationToken verificationToken = tokenRepository.findByUserAndToken(user, code)
                .orElseThrow(() -> new IllegalArgumentException("Invalid verification code"));

        if (LocalDateTime.now().isAfter(verificationToken.getExpiryDate())) {
            tokenRepository.delete(verificationToken);
            throw new IllegalArgumentException("Verification code has expired");
        }

        user.setActive(true);
        userRepository.save(user);
        tokenRepository.deleteByUser(user);
    }

    private User getLocalUnverifiedUser(String username) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username is required");
        }
        User user = userRepository.findUserByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        if (user.getAuthProvider() != AuthProvider.LOCAL) {
            throw new IllegalArgumentException("Verification is required only for local accounts");
        }
        if (user.isActive()) {
            throw new IllegalArgumentException("Email is already confirmed");
        }
        return user;
    }
}
