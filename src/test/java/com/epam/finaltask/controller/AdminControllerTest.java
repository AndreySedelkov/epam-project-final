package com.epam.finaltask.controller;

import com.epam.finaltask.dto.VoucherDTO;
import com.epam.finaltask.model.*;
import com.epam.finaltask.service.AdminService;
import com.epam.finaltask.service.BalanceTopUpRequestService;
import com.epam.finaltask.service.UserService;
import com.epam.finaltask.service.VoucherOrderService;
import com.epam.finaltask.service.VoucherService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminController.class)
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private VoucherService voucherService;

    @MockBean
    private AdminService adminService;

    @MockBean
    private UserService userService;

    @MockBean
    private VoucherOrderService voucherOrderService;

    @MockBean
    private BalanceTopUpRequestService balanceTopUpRequestService;

    @Test
    @DisplayName("GET /admin → status 200, модель заповнена всіма атрибутами")
    @WithMockUser(roles = "ADMIN")
    void shouldShowAdminPanel() throws Exception {
        // given
        VoucherDTO v1 = new VoucherDTO();
        v1.setId("1");
        VoucherDTO v2 = new VoucherDTO();
        v2.setId("2");
        given(voucherService.findAll("ru")).willReturn(List.of(v1, v2));
        given(adminService.findAllUsers()).willReturn(List.of());
        given(voucherOrderService.findAllOrders()).willReturn(List.of());
        given(balanceTopUpRequestService.findAllRequests()).willReturn(List.of());

        // when / then
        mockMvc.perform(get("/admin").param("lang", "ru"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin"))
                .andExpect(model().attribute("vouchers", hasSize(2)))
                .andExpect(model().attribute("allStatuses", arrayContaining(VoucherStatus.values())))
                .andExpect(model().attribute("allTourTypes", arrayContaining(TourType.values())))
                .andExpect(model().attribute("allTransferTypes", arrayContaining(TransferType.values())))
                .andExpect(model().attribute("allHotelTypes", arrayContaining(HotelType.values())))
                .andExpect(model().attribute("users", hasSize(0)))
                .andExpect(model().attribute("orders", hasSize(0)))
                .andExpect(model().attribute("topUpRequests", hasSize(0)))
                .andExpect(model().attribute("roles", arrayContaining(Role.values())));
    }

    @Test
    @DisplayName("POST /admin/users/{id}/block → блокує користувача і редірект з flash")
    @WithMockUser(roles = "ADMIN")
    void shouldBlockUserAndRedirect() throws Exception {
        UUID userId = UUID.randomUUID();
        mockMvc.perform(post("/admin/users/{id}/block", userId)
                        .with(csrf())
                        .param("lang", "en"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin?lang=en#users"))
                .andExpect(flash().attribute("success", "User blocked"));

        then(adminService).should().blockUser(userId);
    }

    @Test
    @DisplayName("POST /admin/users/{id}/unblock → розблоковує користувача і редірект з flash")
    @WithMockUser(roles = "ADMIN")
    void shouldUnblockUserAndRedirect() throws Exception {
        UUID userId = UUID.randomUUID();
        mockMvc.perform(post("/admin/users/{id}/unblock", userId)
                        .with(csrf())
                        .param("lang", "ru"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin?lang=ru#users"))
                .andExpect(flash().attribute("success", "User unblocked"));

        then(adminService).should().unblockUser(userId);
    }

    @Test
    @DisplayName("POST /admin/users/{id}/role → успішна зміна ролі")
    @WithMockUser(roles = "ADMIN")
    void shouldChangeUserRoleSuccess() throws Exception {
        UUID userId = UUID.randomUUID();
        mockMvc.perform(post("/admin/users/{id}/role", userId)
                        .with(csrf())
                        .param("role", "MANAGER")
                        .param("lang", "en"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin?lang=en#users"))
                .andExpect(flash().attribute("success", "Role updated"));

        then(userService).should().changeUserRole(userId, Role.MANAGER);
    }

    @Test
    @DisplayName("POST /admin/users/{id}/role → невідома роль викликає flash error")
    @WithMockUser(roles = "ADMIN")
    void shouldHandleUnknownRole() throws Exception {
        UUID userId = UUID.randomUUID();
        mockMvc.perform(post("/admin/users/{id}/role", userId)
                        .with(csrf())
                        .param("role", "NO_SUCH")
                        .param("lang", "en"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin?lang=en#users"))
                .andExpect(flash().attribute("error", "Unknown role: NO_SUCH"));

        then(userService).should(never()).changeUserRole(any(), any());
    }

    @Test
    @DisplayName("POST /admin/vouchers/{id}/hot → змінює hot і редірект")
    @WithMockUser(roles = "ADMIN")
    void shouldToggleVoucherHot() throws Exception {
        String vid = UUID.randomUUID().toString();
        mockMvc.perform(post("/admin/vouchers/{id}/hot", vid)
                        .with(csrf())
                        .param("hot", "true")
                        .param("lang", "ru"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin?lang=ru"));

        then(voucherService).should().changeHotStatus(vid, true, "ru");
    }

    @Test
    @DisplayName("POST /admin/vouchers/{id}/status → змінює статус і редірект")
    @WithMockUser(roles = "ADMIN")
    void shouldChangeVoucherStatus() throws Exception {
        String vid = UUID.randomUUID().toString();
        mockMvc.perform(post("/admin/vouchers/{id}/status", vid)
                        .with(csrf())
                        .param("status", VoucherStatus.PAID.name())
                        .param("lang", "en"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin?lang=en"));

        then(voucherService).should().changeStatus(vid, VoucherStatus.PAID, "en");
    }

    @Test
    @DisplayName("POST /admin/users/{id}/delete -> deletes user and redirects with success flash")
    @WithMockUser(username = "admin", roles = "ADMIN")
    void shouldDeleteUserAndRedirect() throws Exception {
        UUID userId = UUID.randomUUID();

        mockMvc.perform(post("/admin/users/{id}/delete", userId)
                        .with(csrf())
                        .param("lang", "en"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin?lang=en#users"))
                .andExpect(flash().attribute("success", "User deleted"));

        then(adminService).should().deleteUser(userId, "admin");
    }

    @Test
    @DisplayName("POST /admin/users/{id}/delete -> handles service error with flash error")
    @WithMockUser(username = "admin", roles = "ADMIN")
    void shouldHandleDeleteUserError() throws Exception {
        UUID userId = UUID.randomUUID();
        willThrow(new IllegalStateException("You cannot delete your own account"))
                .given(adminService).deleteUser(eq(userId), eq("admin"));

        mockMvc.perform(post("/admin/users/{id}/delete", userId)
                        .with(csrf())
                        .param("lang", "en"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin?lang=en#users"))
                .andExpect(flash().attribute("error", "You cannot delete your own account"));
    }
}
