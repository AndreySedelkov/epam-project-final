package com.epam.finaltask.controller;

import com.epam.finaltask.dto.VoucherDTO;
import com.epam.finaltask.model.VoucherStatus;
import com.epam.finaltask.service.BalanceTopUpRequestService;
import com.epam.finaltask.service.VoucherOrderService;
import com.epam.finaltask.service.VoucherService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ManagerController.class)
@AutoConfigureMockMvc(addFilters = false)
class ManagerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private VoucherService voucherService;

    @MockBean
    private VoucherOrderService voucherOrderService;

    @MockBean
    private BalanceTopUpRequestService balanceTopUpRequestService;

    @Test
    @DisplayName("GET /manager → 200, view manager, model with vouchers and statuses")
    void shouldShowManagerPanel() throws Exception {
        VoucherDTO v1 = new VoucherDTO();
        v1.setId("a");
        VoucherDTO v2 = new VoucherDTO();
        v2.setId("b");
        given(voucherService.findAll("en"))
                .willReturn(List.of(v1, v2));
        given(voucherOrderService.findAllOrders()).willReturn(List.of());
        given(balanceTopUpRequestService.findAllRequests()).willReturn(List.of());

        mockMvc.perform(get("/manager").param("lang", "en"))
                .andExpect(status().isOk())
                .andExpect(view().name("manager"))
                .andExpect(model().attribute("vouchers", hasSize(2)))
                .andExpect(model().attribute("allStatuses", arrayContaining(VoucherStatus.values())))
                .andExpect(model().attribute("orders", hasSize(0)))
                .andExpect(model().attribute("topUpRequests", hasSize(0)));

        then(voucherService).should().findAll("en");
    }

    @Test
    @DisplayName("POST /manager/vouchers/{id}/hot → toggles hot and redirects")
    void shouldToggleHotAndRedirect() throws Exception {
        String id = UUID.randomUUID().toString();

        mockMvc.perform(post("/manager/vouchers/{id}/hot", id)
                        .param("hot", "true")
                        .param("lang", "en"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/manager?lang=en"));

        then(voucherService).should().changeHotStatus(id, true, "en");
    }

    @Test
    @DisplayName("POST /manager/vouchers/{id}/status → changes status and redirects")
    void shouldChangeStatusAndRedirect() throws Exception {
        String id = UUID.randomUUID().toString();

        mockMvc.perform(post("/manager/vouchers/{id}/status", id)
                        .param("status", VoucherStatus.PAID.name())
                        .param("lang", "en"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/manager?lang=en"));

        then(voucherService).should().changeStatus(id, VoucherStatus.PAID, "en");
    }
}
