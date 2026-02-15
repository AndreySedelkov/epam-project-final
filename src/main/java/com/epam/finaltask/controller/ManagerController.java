package com.epam.finaltask.controller;

import com.epam.finaltask.dto.VoucherDTO;
import com.epam.finaltask.model.OrderStatus;
import com.epam.finaltask.model.VoucherStatus;
import com.epam.finaltask.service.BalanceTopUpRequestService;
import com.epam.finaltask.service.VoucherOrderService;
import com.epam.finaltask.service.VoucherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.security.Principal;
import java.util.UUID;

@Controller
@RequestMapping("/manager")
@PreAuthorize("hasRole('MANAGER')")
@RequiredArgsConstructor
@Slf4j
public class ManagerController {

    private final VoucherService voucherService;
    private final VoucherOrderService voucherOrderService;
    private final BalanceTopUpRequestService balanceTopUpRequestService;

    @GetMapping
    public String panel(
            @RequestParam(value = "lang", defaultValue = "en") String lang,
            Model model) {

        List<VoucherDTO> vouchers = voucherService.findAll(lang);

        model.addAttribute("vouchers",    vouchers);
        model.addAttribute("lang",        lang);
        model.addAttribute("allStatuses", VoucherStatus.values());
        model.addAttribute("orders",      voucherOrderService.findAllOrders());
        model.addAttribute("topUpRequests", balanceTopUpRequestService.findAllRequests());

        return "manager";
    }


    @PostMapping("/vouchers/{id}/hot")
    public String toggleHot(
            @PathVariable String id,
            @RequestParam boolean hot,
            @RequestParam(value = "lang", defaultValue = "en") String lang) {

        voucherService.changeHotStatus(id, hot, lang);
        return "redirect:/manager?lang=" + lang;
    }

    @PostMapping("/vouchers/{id}/status")
    public String changeStatus(
            @PathVariable String id,
            @RequestParam("status") VoucherStatus status,
            @RequestParam(value = "lang", defaultValue = "en") String lang) {

        voucherService.changeStatus(id, status, lang);
        return "redirect:/manager?lang=" + lang;
    }

    @PostMapping("/topups/{id}/approve")
    public String approveTopUp(@PathVariable UUID id,
                               @RequestParam(value = "lang", defaultValue = "en") String lang,
                               Principal principal) {
        balanceTopUpRequestService.approveRequest(id, principal.getName());
        return "redirect:/manager?lang=" + lang + "#topups";
    }

    @PostMapping("/topups/{id}/reject")
    public String rejectTopUp(@PathVariable UUID id,
                              @RequestParam(value = "lang", defaultValue = "en") String lang,
                              Principal principal) {
        balanceTopUpRequestService.rejectRequest(id, principal.getName());
        return "redirect:/manager?lang=" + lang + "#topups";
    }

    @PostMapping("/orders/{id}/approve")
    public String approveOrder(@PathVariable UUID id,
                               @RequestParam(value = "lang", defaultValue = "en") String lang) {
        voucherOrderService.updateOrderStatus(id, OrderStatus.APPROVED);
        return "redirect:/manager?lang=" + lang + "#orders";
    }

    @PostMapping("/orders/{id}/reject")
    public String rejectOrder(@PathVariable UUID id,
                              @RequestParam(value = "lang", defaultValue = "en") String lang) {
        voucherOrderService.updateOrderStatus(id, OrderStatus.CANCELED);
        return "redirect:/manager?lang=" + lang + "#orders";
    }
}
