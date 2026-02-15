package com.epam.finaltask.controller;

import com.epam.finaltask.dto.LoginRequestDTO;
import com.epam.finaltask.dto.PasswordChangeDTO;
import com.epam.finaltask.dto.PasswordResetRequestDTO;
import com.epam.finaltask.dto.TelegramAuthRequest;
import com.epam.finaltask.service.EmailService;
import com.epam.finaltask.service.PasswordResetService;
import com.epam.finaltask.service.TelegramAuthService;
import com.epam.finaltask.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.MailException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.util.Locale;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class AuthenticationController {

    private final PasswordResetService passwordResetService;
    private final TelegramAuthService telegramAuthService;
    private final UserDetailsService userDetailsService;
    private final EmailService emailService;
    private final UserService userService;

    @Value("${app.url.resetPassword}")
    private String resetUrl;

    @Value("${app.telegram.bot-username:}")
    private String telegramBotUsername;

    @GetMapping("/auth/sign-in")
    public String loginPage(@RequestParam(value = "error", required = false) String error,
                            @RequestParam(value = "username", required = false) String username,
                            @RequestParam(value = "verifyRequired", required = false) String verifyRequired,
                            @RequestParam(value = "verifyError", required = false) String verifyError,
                            @RequestParam(value = "verifySendError", required = false) String verifySendError,
                            @RequestParam(value = "codeSent", required = false) String codeSent,
                            Model model) {
        model.addAttribute("loginRequest", new LoginRequestDTO());
        model.addAttribute("verificationUsername", username);
        boolean verificationFlow = verifyRequired != null
                || verifyError != null
                || verifySendError != null
                || codeSent != null;
        if (verificationFlow) {
            try {
                var user = userService.getUserByUsername(username);
                if (!user.isActive()
                        && "LOCAL".equalsIgnoreCase(user.getAuthProvider())
                        && user.getEmail() != null
                        && !user.getEmail().isBlank()) {
                    model.addAttribute("verificationEmail", user.getEmail());
                }
            } catch (RuntimeException ignored) {
                // Verification UI should stay usable even if user details are unavailable.
            }
        }
        if (telegramBotUsername != null && !telegramBotUsername.isBlank()) {
            model.addAttribute("telegramBotUsername", telegramBotUsername);
        }
        if (error != null) {
            model.addAttribute("loginError", true);
        }
        return "auth/sign-in";
    }

    @GetMapping("/auth/forgot-password")
    public String forgotPasswordForm(Model model) {
        model.addAttribute("request", new PasswordResetRequestDTO());
        return "auth/forgot-password";
    }

    @PostMapping("/auth/forgot-password")
    public String processForgotPassword(
            @ModelAttribute("request") @Valid PasswordResetRequestDTO request,
            BindingResult br,
            RedirectAttributes ra) {

        if (br.hasErrors()) {
            return "auth/forgot-password";
        }
        try {
            // створюємо та зберігаємо токен
            String token = passwordResetService.createPasswordResetToken(request.getEmail());
            // будуємо лінк та зберігаємо у flash
            String link = resetUrl + token;
            String subject = "Password reset request";
            String body = "To reset your password, open this link:\n" + link
                    + "\n\nIf you did not request this, you can ignore this email.";
            emailService.sendEmail(request.getEmail(), subject, body);
            ra.addFlashAttribute("message", "Reset link sent to your email.");
            return "redirect:/auth/forgot-password";

        } catch (MailException ex) {
            br.reject("email", "Failed to send reset email. Please try again later.");
            return "auth/forgot-password";
        } catch (IllegalArgumentException ex) {
            br.rejectValue("email", "error.email", ex.getMessage());
            return "auth/forgot-password";
        }
    }

    @GetMapping("/auth/reset-password")
    public String resetPasswordForm(@RequestParam(value = "token", required = false) String token,
                                    Model model,
                                    RedirectAttributes ra) {
        if (token == null || token.isBlank()) {
            return "redirect:/auth/forgot-password";
        }
        try {
            passwordResetService.validatePasswordResetToken(token);
            PasswordChangeDTO dto = new PasswordChangeDTO();
            dto.setToken(token);
            model.addAttribute("dto", dto);
            return "auth/reset-password";
        } catch (IllegalArgumentException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
            return "redirect:/auth/sign-in";
        }
    }

    @PostMapping("/auth/reset-password")
    public String processResetPassword(
            @ModelAttribute("dto") @Valid PasswordChangeDTO dto,
            BindingResult br,
            RedirectAttributes ra) {

        if (br.hasErrors()) {
            return "auth/reset-password";
        }
        try {
            passwordResetService.changePassword(dto);
            ra.addFlashAttribute("message", "Password changed successfully");
            return "redirect:/auth/sign-in";
        } catch (IllegalArgumentException ex) {
            br.reject("error.token", ex.getMessage());
            return "auth/reset-password";
        }
    }

    @PostMapping("/auth/telegram")
    @ResponseBody
    public ResponseEntity<Map<String, String>> telegramLogin(@RequestBody TelegramAuthRequest request,
                                                             HttpServletRequest httpServletRequest,
                                                             Locale locale) {
        try {
            var user = telegramAuthService.authenticate(request);
            UserDetails userDetails = userDetailsService.loadUserByUsername(user.getUsername());
            var authentication = new UsernamePasswordAuthenticationToken(
                    userDetails,
                    null,
                    userDetails.getAuthorities()
            );
            var context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);
            httpServletRequest.getSession(true).setAttribute(
                    HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                    context
            );
            String redirect = "/?lang=" + locale.getLanguage();
            return ResponseEntity.ok(Map.of("redirect", redirect));
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }
}
