package com.epam.finaltask.controller;

import com.epam.finaltask.service.VoucherOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
public class VoucherOrderController {

    private final VoucherOrderService voucherOrderService;

    @PostMapping("/vouchers/order")
    public String orderVoucher(@RequestParam("voucherId") String voucherId, Principal principal) {
        String username = principal.getName();

        voucherOrderService.orderVoucher(voucherId, username);

        return "redirect:/profile";
    }
}