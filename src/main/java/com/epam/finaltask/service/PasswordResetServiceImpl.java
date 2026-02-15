package com.epam.finaltask.service;

import com.epam.finaltask.dto.PasswordChangeDTO;
import com.epam.finaltask.model.PasswordResetToken;
import com.epam.finaltask.model.User;
import com.epam.finaltask.repository.PasswordResetTokenRepository;
import com.epam.finaltask.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PasswordResetServiceImpl implements PasswordResetService {

    private final UserRepository                   userRepo;
    private final PasswordResetTokenRepository     tokenRepo;
    private final PasswordEncoder                  passwordEncoder;

    @Value("${app.resetToken.expirationMinutes:30}")
    private long expirationMinutes;

    @Value("${app.url.resetPassword}")
    private String resetUrl;

    @Override
    @Transactional
    public String createPasswordResetToken(String email) {
        User user = userRepo.findByEmail(email)
                .orElseThrow(() ->
                        new IllegalArgumentException("No user with email " + email));

        tokenRepo.deleteByUser(user);

        String token = UUID.randomUUID().toString();
        LocalDateTime expiry = LocalDateTime.now().plusMinutes(expirationMinutes);
        tokenRepo.save(new PasswordResetToken(user, token, expiry));

        return token;
    }

    @Override
    @Transactional(readOnly = true)
    public User validatePasswordResetToken(String token) {
        PasswordResetToken prt = tokenRepo.findByToken(token)
                .orElseThrow(() ->
                        new IllegalArgumentException("Invalid token"));
        if (prt.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Token expired");
        }
        return prt.getUser();
    }

    @Override
    @Transactional
    public void changePassword(PasswordChangeDTO dto) {
        if (!dto.getNewPassword().equals(dto.getConfirmPassword())) {
            throw new IllegalArgumentException("Passwords do not match");
        }
        User user = validatePasswordResetToken(dto.getToken());
        user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        userRepo.save(user);
        tokenRepo.deleteByUser(user);
    }
}