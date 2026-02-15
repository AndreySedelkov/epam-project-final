package com.epam.finaltask.controller;

import com.epam.finaltask.dto.UserDTO;
import com.epam.finaltask.model.OrderStatus;
import com.epam.finaltask.model.VoucherOrder;
import com.epam.finaltask.model.VoucherStatus;
import com.epam.finaltask.model.User;
import com.epam.finaltask.repository.UserRepository;
import com.epam.finaltask.repository.VoucherOrderRepository;
import com.epam.finaltask.repository.VoucherRepository;
import com.epam.finaltask.service.BalanceTopUpRequestService;
import com.epam.finaltask.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import com.epam.finaltask.model.BalanceTopUpRequest;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.never;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProfileController.class)
@AutoConfigureMockMvc
class ProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;
    @MockBean
    private VoucherOrderRepository voucherOrderRepository;
    @MockBean
    private UserRepository userRepository;
    @MockBean
    private VoucherRepository voucherRepository;
    @MockBean
    private BalanceTopUpRequestService balanceTopUpRequestService;
    @MockBean
    private UserDetailsService userDetailsService;

    @Test
    @DisplayName(
            "GET /profile → 200, model contains user, orders, lang")
    @WithMockUser(username = "viktor")
    void shouldShowProfilePage() throws Exception {
        UserDTO dto = new UserDTO();
        dto.setUsername("viktor");
        given(userService.getUserByUsername("viktor")).willReturn(dto);

        VoucherOrder o1 = new VoucherOrder();
        o1.setId(UUID.randomUUID());
        VoucherOrder o2 = new VoucherOrder();
        o2.setId(UUID.randomUUID());
        given(voucherOrderRepository.findByUserUsername("viktor"))
                .willReturn(List.of(o1, o2));

        mockMvc.perform(get("/profile").param("lang", "en"))
                .andExpect(status().isOk())
                .andExpect(view().name("profile"))
                .andExpect(model().attribute("user", dto))
                .andExpect(model().attribute("orders", hasSize(2)))
                .andExpect(model().attribute("lang", "en"));

        then(userService).should().getUserByUsername("viktor");
        then(voucherOrderRepository).should().findByUserUsername("viktor");
    }

    @Test
    @DisplayName("POST /profile/topup – invalid amount → flash error, without calling the service")
    @WithMockUser(username = "viktor")
    void shouldRejectInvalidTopUp() throws Exception {
        mockMvc.perform(post("/profile/topup")
                        .with(csrf())
                        .param("amount", "-5"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile"))
                .andExpect(flash().attribute("error",
                        "The amount must be between $0 and $10,000."));

        then(balanceTopUpRequestService).should(never())
                .createRequest(anyString(), anyDouble());
    }

    @Test
    @DisplayName("POST /profile/topup – valid amount → flash success, service call")
    @WithMockUser(username = "viktor")
    void shouldTopUpBalance() throws Exception {
        given(balanceTopUpRequestService.createRequest("viktor", 100.0))
                .willReturn(new BalanceTopUpRequest());

        mockMvc.perform(post("/profile/topup")
                        .with(csrf())
                        .param("amount", "100.0"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile"))
                .andExpect(flash().attribute("success",
                        "Top-up request submitted for approval."));

        then(balanceTopUpRequestService).should()
                .createRequest("viktor", 100.0);
    }

    @Test
    @DisplayName("POST /orders/pay - someone else's order → flash error")
    @WithMockUser(username = "viktor")
    void shouldPreventPayingOthersOrder() throws Exception {
        UUID oid = UUID.randomUUID();
        VoucherOrder order = new VoucherOrder();
        order.setId(oid);
        User other = new User();
        other.setUsername("alex");
        order.setUser(other);
        order.setOrderStatus(OrderStatus.CREATED);
        given(voucherOrderRepository.findById(oid))
                .willReturn(Optional.of(order));

        mockMvc.perform(post("/orders/pay")
                        .with(csrf())
                        .param("orderId", oid.toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile"))
                .andExpect(flash().attribute("error",
                        "You are not authorized to pay for this order."));

        then(voucherOrderRepository).should().findById(oid);
    }

    @Test
    @DisplayName("POST /orders/pay – successful payment → flash success, save")
    @WithMockUser(username = "viktor")
    void shouldPayOrderSuccessfully() throws Exception {
        UUID oid = UUID.randomUUID();
        VoucherOrder order = new VoucherOrder();
        order.setId(oid);
        User user = new User();
        user.setUsername("viktor");
        order.setUser(user);
        order.setOrderStatus(OrderStatus.APPROVED);
        order.setTotalPrice(200.0);
        given(voucherOrderRepository.findById(oid))
                .willReturn(Optional.of(order));
        User dbUser = new User();
        dbUser.setUsername("viktor");
        dbUser.setBalance(BigDecimal.valueOf(500));
        given(userRepository.findUserByUsername("viktor"))
                .willReturn(Optional.of(dbUser));

        mockMvc.perform(post("/orders/pay")
                        .with(csrf())
                        .param("orderId", oid.toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile"))
                .andExpect(flash().attribute("success",
                        "Payment was successful!"));

        then(voucherOrderRepository).should().save(order);
        then(userRepository).should().save(dbUser);
    }

    @Test
    @DisplayName("POST /orders/cancel – successful cancellation → flash success, save")
    @WithMockUser(username = "viktor")
    void shouldCancelOrder() throws Exception {
        UUID oid = UUID.randomUUID();
        VoucherOrder order = new VoucherOrder();
        order.setId(oid);
        User user = new User();
        user.setUsername("viktor");
        order.setUser(user);
        order.setOrderStatus(OrderStatus.APPROVED);
        var voucher = new com.epam.finaltask.model.Voucher();
        voucher.setId(UUID.randomUUID());
        voucher.setStatus(VoucherStatus.PAID);
        order.setVoucher(voucher);

        given(voucherOrderRepository.findById(oid))
                .willReturn(Optional.of(order));

        mockMvc.perform(post("/orders/cancel")
                        .with(csrf())
                        .param("orderId", oid.toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile"))
                .andExpect(flash().attribute("success",
                        "Order cancelled, voucher available for purchase again."));

        then(voucherOrderRepository).should().save(order);
        then(voucherRepository).should().save(voucher);
    }
}
