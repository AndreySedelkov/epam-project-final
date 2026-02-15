package com.epam.finaltask.restcontroller;

import com.epam.finaltask.dto.VoucherDTO;
import com.epam.finaltask.model.HotelType;
import com.epam.finaltask.model.TourType;
import com.epam.finaltask.service.VoucherService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(VoucherRestController.class)
@AutoConfigureMockMvc(addFilters = false)
class VoucherRestControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @MockBean
    private VoucherService voucherService;


    @Test
    @DisplayName("GET /api/vouchers → 200 OK with paged vouchers")
    void shouldGetAllVouchersWithPagination() throws Exception {
        VoucherDTO dto = new VoucherDTO();
        dto.setId(UUID.randomUUID().toString());

        Pageable anyPageable = Pageable.unpaged();
        Page<VoucherDTO> page = new PageImpl<>(List.of(dto), anyPageable, 1);

        given(voucherService.findAll(any(Pageable.class), eq("en"))).willReturn(page);

        mockMvc.perform(get("/api/vouchers")
                        .header("Accept-Language", "en")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())

                .andExpect(header().string("X-Total-Count", "1"))

                .andExpect(jsonPath("$.statusCode").value("OK"))
                .andExpect(jsonPath("$.statusMessage")
                        .value("Vouchers retrieved successfully"))

                .andExpect(jsonPath("$.results.content", hasSize(1)))
                .andExpect(jsonPath("$.results.content[0].id")
                        .value(dto.getId()));

        then(voucherService).should().findAll(any(Pageable.class), eq("en"));
    }

    @Test
    @DisplayName("GET /api/vouchers/user/{userId} → 200 OK with user's vouchers")
    void shouldGetVouchersByUser() throws Exception {
        VoucherDTO dto = new VoucherDTO(); dto.setId("v1");
        given(voucherService.findAllByUserId("user123"))
                .willReturn(List.of(dto));

        mockMvc.perform(get("/api/vouchers/user/{userId}", "user123")
                        .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value("OK"))
                .andExpect(jsonPath("$.statusMessage").value("Vouchers retrieved successfully"))
                .andExpect(jsonPath("$.results[0].id").value("v1"));

        then(voucherService).should().findAllByUserId("user123");
    }

    @Test
    @DisplayName("POST /api/vouchers → 201 CREATED with created voucher")
    void shouldCreateVoucher() throws Exception {
        VoucherDTO dto = new VoucherDTO();
        dto.setId("new-id");
        given(voucherService.create(any(VoucherDTO.class))).willReturn(dto);

        mockMvc.perform(post("/api/vouchers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto))
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.statusCode").value("OK"))
                .andExpect(jsonPath("$.statusMessage").value("Voucher is successfully created"))
                .andExpect(jsonPath("$.results.id").value("new-id"));

        then(voucherService).should().create(any(VoucherDTO.class));
    }

    @Test
    @DisplayName("PATCH /api/vouchers/{id} → 200 OK with updated voucher")
    void shouldUpdateVoucher() throws Exception {
        String id = "upd-id";
        VoucherDTO dto = new VoucherDTO(); dto.setId(id);
        given(voucherService.update(eq(id), any(VoucherDTO.class))).willReturn(dto);

        mockMvc.perform(patch("/api/vouchers/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value("OK"))
                .andExpect(jsonPath("$.statusMessage").value("Voucher is successfully updated"))
                .andExpect(jsonPath("$.results.id").value(id));

        then(voucherService).should().update(eq(id), any(VoucherDTO.class));
    }

    @Test
    @DisplayName("DELETE /api/vouchers/{id} → 200 OK with deletion message")
    void shouldDeleteVoucher() throws Exception {
        String id = "del-id";
        mockMvc.perform(delete("/api/vouchers/{id}", id)
                        .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value("OK"))
                .andExpect(jsonPath("$.statusMessage")
                        .value("Voucher with Id " + id + " has been deleted"))
                .andExpect(jsonPath("$.results").value(nullValue()));

        then(voucherService).should().delete(id);
    }

    @Test
    @DisplayName("PATCH /api/vouchers/{id}/status → 400 Bad Request if 'hot' missing")
    void shouldBadRequestWhenHotMissing() throws Exception {
        mockMvc.perform(patch("/api/vouchers/{id}/status", "x")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content("{}")
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode").value("ERROR"))
                .andExpect(jsonPath("$.statusMessage").value("Field 'hot' is required"))
                .andExpect(jsonPath("$.results").value(nullValue()));
    }

    @Test
    @DisplayName("PATCH /api/vouchers/{id}/status → 200 OK with hot status changed")
    void shouldChangeHotStatus() throws Exception {
        String id = "hot-id";
        VoucherDTO dto = new VoucherDTO(); dto.setId(id);
        given(voucherService.changeHotStatus(eq(id), eq(true), eq("en")))
                .willReturn(dto);

        mockMvc.perform(patch("/api/vouchers/{id}/status", id)
                        .header("Accept-Language", "en")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("hot", true)))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value("OK"))
                .andExpect(jsonPath("$.statusMessage")
                        .value("Voucher HOT status succesfully changed"))
                .andExpect(jsonPath("$.results.id").value(id));

        then(voucherService).should().changeHotStatus(id, true, "en");
    }

    @Test
    @DisplayName("PUT /api/vouchers/{id}/order → 200 OK with ordered voucher")
    void shouldOrderVoucher() throws Exception {
        String id = "ord-id", userId = "viktorKovalenko";
        VoucherDTO dto = new VoucherDTO(); dto.setId(id);
        given(voucherService.order(eq(id), eq(userId))).willReturn(dto);

        mockMvc.perform(put("/api/vouchers/{id}/order", id)
                        .param("userId", userId)
                        .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value("OK"))
                .andExpect(jsonPath("$.statusMessage").value("Voucher ordered successfully"))
                .andExpect(jsonPath("$.results.id").value(id));

        then(voucherService).should().order(id, userId);
    }

    @Test
    @DisplayName("GET /api/vouchers/tour-type → 200 OK filtered by tour type")
    void shouldGetByTourType() throws Exception {
        VoucherDTO dto = new VoucherDTO(); dto.setId("tt-id");
        given(voucherService.findAllByTourType(TourType.ADVENTURE))
                .willReturn(List.of(dto));

        mockMvc.perform(get("/api/vouchers/tour-type")
                        .param("tourType", "ADVENTURE")
                        .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value("OK"))
                .andExpect(jsonPath("$.statusMessage").value("Filtered by tour type"))
                .andExpect(jsonPath("$.results[0].id").value("tt-id"));

        then(voucherService).should().findAllByTourType(TourType.ADVENTURE);
    }

    @Test
    @DisplayName("GET /api/vouchers/transfer-type → 200 OK filtered by transfer type")
    void shouldGetByTransferType() throws Exception {
        VoucherDTO dto = new VoucherDTO(); dto.setId("tr-id");
        given(voucherService.findAllByTransferType("BUS"))
                .willReturn(List.of(dto));

        mockMvc.perform(get("/api/vouchers/transfer-type")
                        .param("transferType", "BUS")
                        .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value("OK"))
                .andExpect(jsonPath("$.statusMessage").value("Filtered by transfer type"))
                .andExpect(jsonPath("$.results[0].id").value("tr-id"));

        then(voucherService).should().findAllByTransferType("BUS");
    }

    @Test
    @DisplayName("GET /api/vouchers/hotel-type → 200 OK filtered by hotel type")
    void shouldGetByHotelType() throws Exception {
        VoucherDTO dto = new VoucherDTO(); dto.setId("ht-id");
        given(voucherService.findAllByHotelType(HotelType.FIVE_STARS))
                .willReturn(List.of(dto));

        mockMvc.perform(get("/api/vouchers/hotel-type")
                        .param("hotelType", "FIVE_STARS")
                        .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value("OK"))
                .andExpect(jsonPath("$.statusMessage").value("Filtered by hotel type"))
                .andExpect(jsonPath("$.results[0].id").value("ht-id"));

        then(voucherService).should().findAllByHotelType(HotelType.FIVE_STARS);
    }

    @Test
    @DisplayName("GET /api/vouchers/price → 200 OK filtered by price")
    void shouldGetByPrice() throws Exception {
        VoucherDTO dto = new VoucherDTO(); dto.setId("p-id");
        given(voucherService.findAllByPrice(99.99)).willReturn(List.of(dto));

        mockMvc.perform(get("/api/vouchers/price")
                        .param("price", "99.99")
                        .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value("OK"))
                .andExpect(jsonPath("$.statusMessage").value("Filtered by price"))
                .andExpect(jsonPath("$.results[0].id").value("p-id"));

        then(voucherService).should().findAllByPrice(99.99);
    }
}