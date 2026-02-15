package com.epam.finaltask.controller;

import com.epam.finaltask.dto.VoucherDTO;
import com.epam.finaltask.dto.VoucherFilterRequest;
import com.epam.finaltask.service.VoucherService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(HomeController.class)
@AutoConfigureMockMvc(addFilters = false)
class HomeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private VoucherService voucherService;

    @Test
    @DisplayName("GET / should render home with default lang and empty filter")
    void shouldRenderHomeWithDefaultLang() throws Exception {
        given(voucherService.findAllByFilter(
                any(VoucherFilterRequest.class),
                any(Pageable.class),
                eq("en"))
        ).willReturn(new PageImpl<>(Collections.emptyList()));

        mockMvc.perform(get("/").locale(Locale.ENGLISH))
                .andExpect(status().isOk())
                .andExpect(view().name("home"))
                .andExpect(model().attributeExists("filter"))
                .andExpect(model().attribute("vouchers", hasSize(0)))
                .andExpect(model().attribute("lang", "en"));

        then(voucherService).should()
                .findAllByFilter(any(VoucherFilterRequest.class),
                        any(Pageable.class),
                        eq("en"));
    }

    @Test
    @DisplayName("GET /home with lang and filter parameters should render home view")
    void shouldRenderHomeWithLangAndFilter() throws Exception {
        VoucherDTO voucher = new VoucherDTO();
        voucher.setId(UUID.randomUUID().toString());

        given(voucherService.findAllByFilter(
                any(VoucherFilterRequest.class),
                any(Pageable.class),
                eq("en"))
        ).willReturn(new PageImpl<>(List.of(voucher)));

        mockMvc.perform(get("/home")
                        .param("lang", "en")
                        .param("priceMin", "50")
                        .param("tourType", "WINE")
                )
                .andExpect(status().isOk())
                .andExpect(view().name("home"))
                .andExpect(model().attributeExists("filter"))
                .andExpect(model().attribute("vouchers", hasSize(1)))
                .andExpect(model().attribute("lang", "en"));

        then(voucherService).should()
                .findAllByFilter(any(VoucherFilterRequest.class),
                        any(Pageable.class),
                        eq("en"));
    }
}