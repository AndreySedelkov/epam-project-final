package com.epam.finaltask.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from:}")
    private String from;

    @Value("${spring.mail.username:}")
    private String username;

    @Override
    @Async
    public void sendEmail(String to, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        String fromAddress = resolveFrom();
        message.setFrom(fromAddress);
        message.setSubject(subject);
        message.setText(body);
        log.info("Sending reset email from={} to={} subject={}", fromAddress, to, subject);
        try {
            mailSender.send(message);
            log.info("Reset email sent to={}", to);
        } catch (Exception ex) {
            log.error("Failed to send reset email to={}", to, ex);
            throw ex;
        }
    }

    private String resolveFrom() {
        if (from != null && !from.isBlank()) {
            return from;
        }
        return username;
    }
}
