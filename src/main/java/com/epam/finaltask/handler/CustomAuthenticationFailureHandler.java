package com.epam.finaltask.handler;

import com.epam.finaltask.service.EmailVerificationService;
import com.epam.finaltask.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
public class CustomAuthenticationFailureHandler
        implements AuthenticationFailureHandler {

    @Autowired
    private UserService userService;

    @Autowired
    private EmailVerificationService emailVerificationService;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception)
            throws IOException, ServletException {
        if (exception instanceof DisabledException) {
            String username = request.getParameter("username");
            String encoded = username == null ? "" : URLEncoder.encode(username, StandardCharsets.UTF_8);
            boolean codeSent = false;
            try {
                emailVerificationService.sendVerificationCodeByUsername(username);
                codeSent = true;
            } catch (Exception ignored) {
                codeSent = false;
            }

            String suffix = codeSent ? "&codeSent=true" : "&verifySendError=true";
            response.sendRedirect(request.getContextPath() + "/auth/sign-in?verifyRequired=true&username=" + encoded + suffix);
            return;
        }

        String username = request.getParameter("username");
        if (username != null) {
            userService.increaseFailedAttempts(username);
        }
        response.sendRedirect(request.getContextPath() + "/auth/sign-in?error");
    }
}
