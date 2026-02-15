package com.epam.finaltask.restcontroller;

import com.epam.finaltask.dto.VoucherOrderDTO;
import com.epam.finaltask.model.User;
import com.epam.finaltask.model.Voucher;
import com.epam.finaltask.model.VoucherOrder;
import com.epam.finaltask.model.OrderStatus;
import com.epam.finaltask.service.VoucherOrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(VoucherOrderRestController.class)
@AutoConfigureMockMvc(addFilters = false)
class VoucherOrderRestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private VoucherOrderService voucherOrderService;

    @Test
    @DisplayName("PUT /api/voucher-orders/{voucherId}/order → 200 OK with order DTO")
    void shouldOrderVoucherAndReturnDto() throws Exception {
        String voucherId = UUID.randomUUID().toString();
        String userId = "viktorKovalenko";

        User user = new User();
        user.setUsername(userId);

        Voucher voucher = new Voucher();
        voucher.setId(UUID.fromString(voucherId));

        LocalDateTime date = LocalDateTime.of(2025, 6, 25, 12, 30, 0);

        VoucherOrder order = new VoucherOrder();
        order.setId(UUID.randomUUID());
        order.setUser(user);
        order.setVoucher(voucher);
        order.setOrderDate(date);
        order.setOrderStatus(OrderStatus.CREATED);
        order.setTotalPrice(150.0);

        given(voucherOrderService.orderVoucher(eq(voucherId), eq(userId)))
                .willReturn(order);

        mockMvc.perform(put("/api/voucher-orders/{voucherId}/order", voucherId)
                        .param("userId", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value("OK"))
                .andExpect(jsonPath("$.statusMessage").value("Voucher ordered successfully"))
                .andExpect(jsonPath("$.results.id").value(order.getId().toString()))
                .andExpect(jsonPath("$.results.userId").value(userId))
                .andExpect(jsonPath("$.results.voucherId").value(voucherId))
                .andExpect(jsonPath("$.results.orderDate").value("2025-06-25T12:30:00"))
                .andExpect(jsonPath("$.results.orderStatus").value("CREATED"))
                .andExpect(jsonPath("$.results.totalPrice").value(150.0));

        then(voucherOrderService).should().orderVoucher(voucherId, userId);
    }
}