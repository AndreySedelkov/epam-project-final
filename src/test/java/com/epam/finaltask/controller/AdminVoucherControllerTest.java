package com.epam.finaltask.controller;

import com.epam.finaltask.dto.VoucherDTO;
import com.epam.finaltask.model.*;
import com.epam.finaltask.service.VoucherService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminVoucherController.class)
class AdminVoucherControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private VoucherService voucherService;

    @Test
    @DisplayName("GET /admin/vouchers/create → status 200 and empty DTO with enum lists")
    @WithMockUser(roles = "ADMIN")
    void shouldShowCreateForm() throws Exception {
        mockMvc.perform(get("/admin/vouchers/create"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/vouchers/edit"))
                .andExpect(model().attributeExists("voucher"))
                .andExpect(model().attribute("voucher", hasProperty("id", nullValue())))
                .andExpect(model().attribute("allStatuses", arrayContaining(VoucherStatus.values())))
                .andExpect(model().attribute("allTourTypes", arrayContaining(TourType.values())))
                .andExpect(model().attribute("allTransferTypes", arrayContaining(TransferType.values())))
                .andExpect(model().attribute("allHotelTypes", arrayContaining(HotelType.values())));
    }

    @Test
    @DisplayName("POST /admin/vouchers/create → valid data, service call and redirect")
    @WithMockUser(username = "admin", roles = "ADMIN")
    void shouldCreateVoucherAndRedirect() throws Exception {
        given(voucherService.create(any(VoucherDTO.class)))
                .willAnswer(inv -> inv.getArgument(0, VoucherDTO.class));

        mockMvc.perform(post("/admin/vouchers/create")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("lang", "en")
                        .param("title", "New Voucher")
                        .param("description", "Description")
                        .param("price", "1000.0")
                        .param("arrivalDate", "2025-11-11")
                        .param("evictionDate", "2025-12-11")
                        .param("tourType", TourType.WINE.name())
                        .param("transferType", TransferType.SHIP.name())
                        .param("hotelType", HotelType.THREE_STARS.name())
                        .param("status", VoucherStatus.REGISTERED.name())
                )
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin?lang=en"));

        ArgumentCaptor<VoucherDTO> captor = ArgumentCaptor.forClass(VoucherDTO.class);
        then(voucherService).should().create(captor.capture());
        VoucherDTO passed = captor.getValue();
        assertThat(passed.getTitle()).isEqualTo("New Voucher");
        assertThat(passed.getPrice()).isEqualTo(1000.0);
        assertThat(passed.getTourType()).isEqualTo(TourType.WINE.name());
        assertThat(passed.getUserId()).isEqualTo("admin");
    }

    @Test
    @DisplayName("GET /admin/vouchers/{id}/edit → status 200, existing DTO and enum lists")
    @WithMockUser(roles = "ADMIN")
    void shouldShowEditForm() throws Exception {
        String id = UUID.randomUUID().toString();
        VoucherDTO dto = new VoucherDTO();
        dto.setId(id);
        dto.setTitle("Existing Voucher");
        given(voucherService.findById(id)).willReturn(dto);

        mockMvc.perform(get("/admin/vouchers/{id}/edit", id))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/vouchers/edit"))
                .andExpect(model().attribute("voucher", hasProperty("id", is(id))))
                .andExpect(model().attribute("allStatuses", arrayContaining(VoucherStatus.values())))
                .andExpect(model().attribute("allTourTypes", arrayContaining(TourType.values())))
                .andExpect(model().attribute("allTransferTypes", arrayContaining(TransferType.values())))
                .andExpect(model().attribute("allHotelTypes", arrayContaining(HotelType.values())));
    }

    @Test
    @DisplayName("POST /admin/vouchers/{id}/update → valid data, update and redirect")
    @WithMockUser(roles = "ADMIN")
    void shouldUpdateAndRedirect() throws Exception {
        String id = UUID.randomUUID().toString();
        given(voucherService.update(eq(id), any(VoucherDTO.class)))
                .willReturn(new VoucherDTO());

        mockMvc.perform(post("/admin/vouchers/{id}/update", id)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("lang", "ru")
                        .param("title", "Updated Voucher")
                        .param("description", "Desc")
                        .param("price", "200.0")
                        .param("arrivalDate", "2025-01-01")
                        .param("evictionDate", "2025-01-07")
                        .param("tourType", TourType.ADVENTURE.name())
                        .param("transferType", TransferType.BUS.name())
                        .param("hotelType", HotelType.FIVE_STARS.name())
                        .param("status", VoucherStatus.PAID.name())
                )
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin?lang=ru"));

        then(voucherService).should().update(eq(id), any(VoucherDTO.class));
    }

    @Test
    @DisplayName("POST /admin/vouchers/{id}/delete → delete and redirect")
    @WithMockUser(roles = "ADMIN")
    void shouldDeleteAndRedirect() throws Exception {
        String id = UUID.randomUUID().toString();

        mockMvc.perform(post("/admin/vouchers/{id}/delete", id)
                        .with(csrf())
                        .param("lang", "ru")
                )
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin?lang=ru"));

        then(voucherService).should().delete(id);
    }
}
