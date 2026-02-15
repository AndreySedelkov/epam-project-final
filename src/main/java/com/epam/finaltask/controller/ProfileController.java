package com.epam.finaltask.controller;

import com.epam.finaltask.dto.ProfileUpdateRequest;
import com.epam.finaltask.dto.UserDTO;
import com.epam.finaltask.model.OrderStatus;
import com.epam.finaltask.model.VoucherOrder;
import com.epam.finaltask.model.VoucherStatus;
import com.epam.finaltask.repository.UserRepository;
import com.epam.finaltask.repository.VoucherOrderRepository;
import com.epam.finaltask.repository.VoucherRepository;
import com.epam.finaltask.service.BalanceTopUpRequestService;
import com.epam.finaltask.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.List;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class ProfileController {

    private final UserService userService;
    private final BalanceTopUpRequestService balanceTopUpRequestService;
    private final VoucherOrderRepository voucherOrderRepository;
    private final UserRepository userRepository;
    private final VoucherRepository voucherRepository;
    private final UserDetailsService userDetailsService;

    @GetMapping("/profile")
    public String profile(
            Principal principal,
            @RequestParam(value = "lang", required = false) String lang,
            Model model) {

        if (lang == null) {
            lang = LocaleContextHolder.getLocale().getLanguage();
        }

        UserDTO user = userService.getUserByUsername(principal.getName());
        List<VoucherOrder> orders =
                voucherOrderRepository.findByUserUsername(principal.getName());

        model.addAttribute("user", user);
        model.addAttribute("profileUpdate", toProfileUpdate(user));
        model.addAttribute("orders", orders);
        model.addAttribute("lang", lang);

        return "profile";
    }

    @PostMapping("/profile/update")
    public String updateProfile(Principal principal,
                                ProfileUpdateRequest request,
                                RedirectAttributes redirectAttributes,
                                HttpServletRequest httpServletRequest) {
        String currentUsername = principal.getName();
        try {
            UserDTO updated = userService.updateProfile(currentUsername, request);
            refreshAuthenticationIfNeeded(currentUsername, updated.getUsername(), httpServletRequest);
            redirectAttributes.addFlashAttribute("success", "Profile updated.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/profile";
    }


    @PostMapping("/profile/topup")
    public String topUpBalance(Principal principal,
                               @RequestParam("amount") double amount,
                               RedirectAttributes redirectAttributes) {

        if (amount < 0 || amount > 10000) {
            redirectAttributes.addFlashAttribute("error", "The amount must be between $0 and $10,000.");
            return "redirect:/profile";
        }

        try {
            balanceTopUpRequestService.createRequest(principal.getName(), amount);
            redirectAttributes.addFlashAttribute("success", "Top-up request submitted for approval.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "An error occurred while replenishing the balance.");
        }

        return "redirect:/profile";
    }

    @PostMapping("/orders/pay")
    @PreAuthorize("@securityExpressions.isOrderOwner(#orderId, authentication.name)")
    public String payOrder(@RequestParam("orderId") UUID orderId,
                           Principal principal,
                           RedirectAttributes redirectAttributes) {
        VoucherOrder order = voucherOrderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));

        String username = principal.getName();
        if (!order.getUser().getUsername().equals(username)) {
            redirectAttributes.addFlashAttribute("error", "You are not authorized to pay for this order.");
            return "redirect:/profile";
        }
        if (order.getOrderStatus().toString().toLowerCase().equals("confirmed")) {
            redirectAttributes.addFlashAttribute("info", "The order has already been paid for.");
            return "redirect:/profile";
        }
        if (order.getOrderStatus() != OrderStatus.APPROVED) {
            redirectAttributes.addFlashAttribute("error", "Order must be approved before payment.");
            return "redirect:/profile";
        }

        var user = userRepository.findUserByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        BigDecimal totalPrice = BigDecimal.valueOf(order.getTotalPrice());
        if (user.getBalance() == null || user.getBalance().compareTo(totalPrice) < 0) {
            redirectAttributes.addFlashAttribute("error", "Not enough funds, top up your balance.");
            return "redirect:/profile";
        }

        user.setBalance(user.getBalance().subtract(totalPrice));
        order.setOrderStatus(OrderStatus.CONFIRMED);
        voucherOrderRepository.save(order);
        userRepository.save(user);

        redirectAttributes.addFlashAttribute("success", "Payment was successful!");
        return "redirect:/profile";
    }

    @PostMapping("/orders/cancel")
    @PreAuthorize("@securityExpressions.isOrderOwner(#orderId, authentication.name)")
    public String cancelOrder(@RequestParam("orderId") UUID orderId,
                              Principal principal,
                              RedirectAttributes redirectAttributes) {
        VoucherOrder order = voucherOrderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));

        String username = principal.getName();
        if (!order.getUser().getUsername().equals(username)) {
            redirectAttributes.addFlashAttribute("error", "You do not have permission to cancel this order.");
            return "redirect:/profile";
        }
        if (order.getOrderStatus().toString().toLowerCase().equals("confirmed")) {
            redirectAttributes.addFlashAttribute("error", "The order has already been paid for, it cannot be canceled.");
            return "redirect:/profile";
        }

        order.setOrderStatus(OrderStatus.CANCELED);
        voucherOrderRepository.save(order);

        order.getVoucher().setStatus(VoucherStatus.REGISTERED);
        voucherRepository.save(order.getVoucher());

        redirectAttributes.addFlashAttribute("success", "Order cancelled, voucher available for purchase again.");
        return "redirect:/profile";
    }

    private ProfileUpdateRequest toProfileUpdate(UserDTO user) {
        ProfileUpdateRequest request = new ProfileUpdateRequest();
        request.setUsername(user.getUsername());
        request.setFirstName(user.getName());
        request.setLastName(user.getSurname());
        request.setPhoneNumber(user.getPhoneNumber());
        return request;
    }

    private void refreshAuthenticationIfNeeded(String previousUsername,
                                               String newUsername,
                                               HttpServletRequest request) {
        if (newUsername == null || newUsername.equals(previousUsername)) {
            return;
        }
        UserDetails userDetails = userDetailsService.loadUserByUsername(newUsername);
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
