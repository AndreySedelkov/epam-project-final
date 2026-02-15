package com.epam.finaltask.controller;

import com.epam.finaltask.dto.SignUpRequestDTO;
import com.epam.finaltask.exception.ResourceAlreadyExistsException;
import com.epam.finaltask.service.EmailVerificationService;
import com.epam.finaltask.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/auth")
@Slf4j
@RequiredArgsConstructor
public class RegistrationController {

    private final UserService userService;
    private final EmailVerificationService emailVerificationService;
    private final UserDetailsService userDetailsService;

    @GetMapping("/sign-up")
    public String signUpForm(Model model) {
        model.addAttribute("signUpRequest", new SignUpRequestDTO());
        return "auth/sign-up";
    }

    @PostMapping("/sign-up")
    public String register(
            @Valid @ModelAttribute("signUpRequest") SignUpRequestDTO dto,
            BindingResult bindingResult,
            Model model
    ) {
        if (bindingResult.hasErrors()) {
            return "auth/sign-up";
        }

        try {
            userService.register(dto);
        } catch (IllegalArgumentException ex) {
            bindingResult.rejectValue("confirmPassword", "password.mismatch");
            return "auth/sign-up";
        } catch (IllegalStateException ex) {
            model.addAttribute("error", ex.getMessage());
            return "auth/sign-up";
        } catch (ResourceAlreadyExistsException ex) {
            String msg = ex.getMessage().toLowerCase();
            if (msg.contains("username")) {
                bindingResult.rejectValue("username", "username.exists");
            } else if (msg.contains("email")) {
                bindingResult.rejectValue("email", "email.exists");
            } else if (msg.contains("phone")) {
                bindingResult.rejectValue("phoneNumber", "phone.exists");
            } else {
                model.addAttribute("error", ex.getMessage());
            }
            return "auth/sign-up";
        }

        return "redirect:/auth/sign-in";
    }

    @PostMapping("/send-verification-code")
    public String sendVerificationCode(@RequestParam("username") String username,
                                       RedirectAttributes ra) {
        try {
            emailVerificationService.resendVerificationCodeByUsername(username);
            ra.addAttribute("verifyRequired", "true");
            ra.addAttribute("codeSent", "true");
            ra.addAttribute("username", username);
            return "redirect:/auth/sign-in";
        } catch (IllegalArgumentException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
            ra.addAttribute("verifyRequired", "true");
            ra.addAttribute("verifySendError", "true");
            ra.addAttribute("username", username);
            return "redirect:/auth/sign-in";
        } catch (Exception ex) {
            String msg = ex.getMessage() != null && !ex.getMessage().isBlank()
                    ? ex.getMessage()
                    : "Failed to send verification code. Please try again.";
            ra.addFlashAttribute("error", msg);
            ra.addAttribute("verifyRequired", "true");
            ra.addAttribute("verifySendError", "true");
            ra.addAttribute("username", username);
            return "redirect:/auth/sign-in";
        }
    }

    @PostMapping("/verify-email-code")
    public String verifyEmailCode(@RequestParam("username") String username,
                                  @RequestParam("code") String code,
                                  HttpServletRequest request,
                                  RedirectAttributes ra) {
        try {
            emailVerificationService.confirmEmailByCode(username, code);
            authenticateVerifiedUser(username, request);
            userService.resetFailedAttempts(username);
            return "redirect:/";
        } catch (IllegalArgumentException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
            ra.addAttribute("verifyError", "true");
            ra.addAttribute("username", username);
            return "redirect:/auth/sign-in";
        } catch (Exception ex) {
            String msg = ex.getMessage() != null && !ex.getMessage().isBlank()
                    ? ex.getMessage()
                    : "Verification failed. Please try again.";
            ra.addFlashAttribute("error", msg);
            ra.addAttribute("verifyError", "true");
            ra.addAttribute("username", username);
            return "redirect:/auth/sign-in";
        }
    }

    private void authenticateVerifiedUser(String username, HttpServletRequest request) {
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
        var authentication = new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities()
        );
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        request.getSession(true).setAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                context
        );
    }
}
