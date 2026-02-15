package com.epam.finaltask.controller;

import com.epam.finaltask.dto.VoucherDTO;
import com.epam.finaltask.dto.VoucherFilterRequest;
import com.epam.finaltask.paged.PagedResponse;
import com.epam.finaltask.service.VoucherService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Locale;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final VoucherService voucherService;

    @GetMapping({"/", "/home"})
    public String homePage(
            @ModelAttribute VoucherFilterRequest filter,
            @RequestParam(value = "lang",  required = false) String langParam,
            @RequestParam(value = "page",  defaultValue = "0") int page,
            @RequestParam(value = "size",  defaultValue = "6") int size,
            Locale locale,
            Model model) {

        String lang = (langParam != null) ? langParam : locale.getLanguage();

        Pageable pageable = PageRequest.of(page, size);
        Page<VoucherDTO> pageResult =
                voucherService.findAllByFilter(filter, pageable, lang);

        PagedResponse<VoucherDTO> paged = new PagedResponse<>(pageResult);

        List<VoucherDTO> vouchers = pageResult.getContent();

        model.addAttribute("filter",         filter);
        model.addAttribute("lang",           lang);
        model.addAttribute("pagedVouchers",  paged);
        model.addAttribute("vouchers",       vouchers);

        return "home";
    }
}