package com.epam.finaltask.controller;

import com.epam.finaltask.dto.VoucherDTO;
import com.epam.finaltask.model.HotelType;
import com.epam.finaltask.model.TourType;
import com.epam.finaltask.model.TransferType;
import com.epam.finaltask.model.VoucherStatus;
import com.epam.finaltask.service.VoucherService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Controller
@RequestMapping("/admin/vouchers")
@Slf4j
@RequiredArgsConstructor
public class AdminVoucherController {

    private final VoucherService voucherService;

    @GetMapping("/create")
    public String createForm(Model model) {
        model.addAttribute("voucher", new VoucherDTO());
        populateEnums(model);
        return "admin/vouchers/edit";
    }

    @PostMapping("/create")
    public String create(
            @ModelAttribute("voucher") @Valid VoucherDTO voucher,
            BindingResult bindingResult,
            @RequestParam(name = "lang", defaultValue = "en") String lang,
            @AuthenticationPrincipal UserDetails currentUser,
            Model model
    ) {
        log.info("[ADMIN] Create request: lang='{}', DTO={}", lang, voucher);
        log.info("[ADMIN] CREATE form submitted, DTO.id='{}'", voucher.getId());
        if (bindingResult.hasErrors()) {
            log.warn("[ADMIN] Binding errors on create: {}", bindingResult.getAllErrors());
            populateEnums(model);
            return "admin/vouchers/edit";
        }
        voucher.setUserId(currentUser.getUsername());
        try {
            VoucherDTO created = voucherService.create(voucher);
            log.info("[ADMIN] Successfully created voucher: {}", created);
            return "redirect:/admin?lang=" + lang;
        } catch (Exception ex) {
            log.error("[ADMIN] Error during voucher creation: {}", ex.getMessage(), ex);
            model.addAttribute("errorMessage", "Error creating voucher: " + ex.getMessage());
            populateEnums(model);
            return "admin/vouchers/edit";
        }
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable String id, Model model) {
        VoucherDTO existing = voucherService.findById(id);
        model.addAttribute("voucher", existing);
        populateEnums(model);
        return "admin/vouchers/edit";
    }

    @PostMapping("/{id}/update")
    public String update(
            @PathVariable String id,
            @ModelAttribute("voucher") @Valid VoucherDTO voucher,
            BindingResult bindingResult,
            @RequestParam(name = "lang", defaultValue = "en") String lang,
            Model model
    ) {
        log.info("[ADMIN] Update request: id='{}', lang='{}', DTO={}", id, lang, voucher);

        if (bindingResult.hasErrors()) {
            log.warn("[ADMIN] Binding errors on update id='{}': {}", id, bindingResult.getAllErrors());
            populateEnums(model);
            return "admin/vouchers/edit";
        }

        try {
            VoucherDTO updated = voucherService.update(id, voucher);
            log.info("[ADMIN] Successfully updated voucher id='{}': {}", id, updated);
            return "redirect:/admin?lang=" + lang;
        } catch (Exception ex) {
            log.error("[ADMIN] Error during update id='{}': {}", id, ex.getMessage(), ex);
            model.addAttribute("errorMessage", "Error updating voucher: " + ex.getMessage());
            populateEnums(model);
            return "admin/vouchers/edit";
        }
    }

    @PostMapping("/{id}/delete")
    public String deleteVoucher(
            @PathVariable String id,
            @RequestParam(name = "lang", defaultValue = "en") String lang
    ) {
        log.info("[ADMIN] Delete request for voucher id='{}'", id);
        voucherService.delete(id);
        return "redirect:/admin?lang=" + lang;
    }

    private void populateEnums(Model model) {
        model.addAttribute("allStatuses", VoucherStatus.values());
        model.addAttribute("allTourTypes", TourType.values());
        model.addAttribute("allTransferTypes", TransferType.values());
        model.addAttribute("allHotelTypes", HotelType.values());
    }
}
