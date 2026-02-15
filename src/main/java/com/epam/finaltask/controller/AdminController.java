package com.epam.finaltask.controller;

import com.epam.finaltask.dto.VoucherDTO;
import com.epam.finaltask.model.*;
import com.epam.finaltask.service.AdminService;
import com.epam.finaltask.service.BalanceTopUpRequestService;
import com.epam.finaltask.service.UserService;
import com.epam.finaltask.service.VoucherOrderService;
import com.epam.finaltask.service.VoucherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.security.Principal;
import java.util.UUID;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Slf4j
public class AdminController {

    private final VoucherService voucherService;
    private final AdminService   adminService;
    private final UserService    userService;
    private final VoucherOrderService voucherOrderService;
    private final BalanceTopUpRequestService balanceTopUpRequestService;

    @ModelAttribute("roles")
    public Role[] allRoles() {
        return Role.values();
    }

    @GetMapping
    public String panelAdmin(
            @RequestParam(value = "lang", defaultValue = "en") String lang,
            Model model) {

        List<VoucherDTO> vouchers = voucherService.findAll(lang);

        model.addAttribute("vouchers",      vouchers);
        model.addAttribute("lang",          lang);
        model.addAttribute("allStatuses",   VoucherStatus.values());
        model.addAttribute("allTourTypes",  TourType.values());
        model.addAttribute("allTransferTypes", TransferType.values());
        model.addAttribute("allHotelTypes",   HotelType.values());
        model.addAttribute("users",           adminService.findAllUsers());
        model.addAttribute("currentAdminUsername", principalUsername());
        model.addAttribute("orders",          voucherOrderService.findAllOrders());
        model.addAttribute("topUpRequests",   balanceTopUpRequestService.findAllRequests());

        return "admin";
    }

    private String principalUsername() {
        var auth = org.springframework.security.core.context.SecurityContextHolder
                .getContext()
                .getAuthentication();
        return auth != null ? auth.getName() : "";
    }

    @PostMapping("/users/{id}/block")
    public String block(@PathVariable UUID id,
                        @RequestParam(value = "lang", defaultValue = "en") String lang,
                        RedirectAttributes ra) {
        adminService.blockUser(id);
        ra.addFlashAttribute("success", "User blocked");
        return "redirect:/admin?lang=" + lang + "#users";
    }

    @PostMapping("/users/{id}/unblock")
    public String unblock(@PathVariable UUID id,
                          @RequestParam(value = "lang", defaultValue = "en") String lang,
                          RedirectAttributes ra) {
        adminService.unblockUser(id);
        ra.addFlashAttribute("success", "User unblocked");
        return "redirect:/admin?lang=" + lang + "#users";
    }

    @PostMapping("/users/{id}/role")
    public String changeRole(@PathVariable("id") UUID userId,
                             @RequestParam("role") String roleName,
                             @RequestParam(value = "lang", defaultValue = "en") String lang,
                             RedirectAttributes ra) {
        try {
            Role newRole = Role.valueOf(roleName);
            userService.changeUserRole(userId, newRole);
            ra.addFlashAttribute("success", "Role updated");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", "Unknown role: " + roleName);
        } catch (Exception ex) {
            log.error("Error changing role for user {}: ", userId, ex);
            ra.addFlashAttribute("error", "Cannot change role: " + ex.getMessage());
        }
        return "redirect:/admin?lang=" + lang + "#users";
    }

    @PostMapping("/users/{id}/delete")
    public String deleteUser(@PathVariable UUID id,
                             @RequestParam(value = "lang", defaultValue = "en") String lang,
                             Principal principal,
                             RedirectAttributes ra) {
        try {
            adminService.deleteUser(id, principal.getName());
            ra.addFlashAttribute("success", "User deleted");
        } catch (IllegalStateException | IllegalArgumentException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
        } catch (Exception ex) {
            log.error("Error deleting user {}: ", id, ex);
            ra.addFlashAttribute("error", "Cannot delete user: " + ex.getMessage());
        }
        return "redirect:/admin?lang=" + lang + "#users";
    }

    @PostMapping("/vouchers/{id}/hot")
    public String toggleHot(@PathVariable String id,
                            @RequestParam boolean hot,
                            @RequestParam(value = "lang", defaultValue = "en") String lang) {
        voucherService.changeHotStatus(id, hot, lang);
        return "redirect:/admin?lang=" + lang;
    }

    @PostMapping("/vouchers/{id}/status")
    public String changeStatus(@PathVariable String id,
                               @RequestParam("status") VoucherStatus status,
                               @RequestParam(value = "lang", defaultValue = "en") String lang) {
        voucherService.changeStatus(id, status, lang);
        return "redirect:/admin?lang=" + lang;
    }

    @PostMapping("/topups/{id}/approve")
    public String approveTopUp(@PathVariable UUID id,
                               @RequestParam(value = "lang", defaultValue = "en") String lang,
                               Principal principal,
                               RedirectAttributes ra) {
        balanceTopUpRequestService.approveRequest(id, principal.getName());
        ra.addFlashAttribute("success", "Top-up approved");
        return "redirect:/admin?lang=" + lang + "#topups";
    }

    @PostMapping("/topups/{id}/reject")
    public String rejectTopUp(@PathVariable UUID id,
                              @RequestParam(value = "lang", defaultValue = "en") String lang,
                              Principal principal,
                              RedirectAttributes ra) {
        balanceTopUpRequestService.rejectRequest(id, principal.getName());
        ra.addFlashAttribute("success", "Top-up rejected");
        return "redirect:/admin?lang=" + lang + "#topups";
    }

    @PostMapping("/orders/{id}/approve")
    public String approveOrder(@PathVariable UUID id,
                               @RequestParam(value = "lang", defaultValue = "en") String lang,
                               RedirectAttributes ra) {
        voucherOrderService.updateOrderStatus(id, OrderStatus.APPROVED);
        ra.addFlashAttribute("success", "Order approved");
        return "redirect:/admin?lang=" + lang + "#orders";
    }

    @PostMapping("/orders/{id}/reject")
    public String rejectOrder(@PathVariable UUID id,
                              @RequestParam(value = "lang", defaultValue = "en") String lang,
                              RedirectAttributes ra) {
        voucherOrderService.updateOrderStatus(id, OrderStatus.CANCELED);
        ra.addFlashAttribute("success", "Order rejected");
        return "redirect:/admin?lang=" + lang + "#orders";
    }
}
