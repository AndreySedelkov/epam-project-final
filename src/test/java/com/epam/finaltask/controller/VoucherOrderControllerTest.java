package com.epam.finaltask.controller;

import com.epam.finaltask.service.VoucherOrderService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.BDDMockito.then;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(VoucherOrderController.class)
@AutoConfigureMockMvc
class VoucherOrderControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    VoucherOrderService voucherOrderService;

    @Test
    @DisplayName("POST /vouchers/order → calls service and redirects to profile")
    @WithMockUser(username = "viktor")
    void shouldOrderVoucherAndRedirect() throws Exception {
        mockMvc.perform(post("/vouchers/order")
                        .with(csrf())
                        .param("voucherId", "abc123")
                )
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile"));

        then(voucherOrderService).should()
                .orderVoucher("abc123", "viktor");
    }
}