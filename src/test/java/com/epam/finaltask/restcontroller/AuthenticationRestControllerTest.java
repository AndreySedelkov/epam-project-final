package com.epam.finaltask.restcontroller;

import com.epam.finaltask.dto.AuthRequestDTO;
import com.epam.finaltask.dto.AuthResponseDTO;
import com.epam.finaltask.dto.RefreshRequestDTO;
import com.epam.finaltask.service.AuthenticationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthenticationRestController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthenticationRestControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockBean AuthenticationService authenticationService;

    @Test
    @DisplayName("POST /api/auth/login → 200 with correct JSON fields")
    void loginShouldReturnAuthResponse() throws Exception {
        AuthRequestDTO req = new AuthRequestDTO();
        req.setUsername("viktorKovalenko");
        req.setPassword("secret");

        AuthResponseDTO resp = new AuthResponseDTO();
        resp.setAccessToken("access-token");
        resp.setRefreshToken("refresh-token");

        given(authenticationService.authenticate(any(AuthRequestDTO.class)))
                .willReturn(resp);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value("OK"))
                .andExpect(jsonPath("$.statusMessage").value("Authentication successful"))
                .andExpect(jsonPath("$.results.accessToken").value("access-token"))
                .andExpect(jsonPath("$.results.refreshToken").value("refresh-token"));

        then(authenticationService).should().authenticate(any(AuthRequestDTO.class));
    }

    @Test
    @DisplayName("POST /api/auth/refresh → 200 with new tokens")
    void refreshShouldReturnNewTokens() throws Exception {
        RefreshRequestDTO req = new RefreshRequestDTO();
        req.setRefreshToken("old-refresh");

        AuthResponseDTO resp = new AuthResponseDTO();
        resp.setAccessToken("new-access");
        resp.setRefreshToken("new-refresh");

        given(authenticationService.refreshToken(any(RefreshRequestDTO.class)))
                .willReturn(resp);

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value("OK"))
                .andExpect(jsonPath("$.statusMessage").value("Token refreshed successfully"))
                .andExpect(jsonPath("$.results.accessToken").value("new-access"))
                .andExpect(jsonPath("$.results.refreshToken").value("new-refresh"));

        then(authenticationService).should().refreshToken(any(RefreshRequestDTO.class));
    }

    @Test
    @DisplayName("POST /api/auth/logout → 200 and no results payload")
    void logoutShouldReturnNoData() throws Exception {
        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer dummy-token")
                        .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value("OK"))
                .andExpect(jsonPath("$.statusMessage").value("Logout successful"))
                .andExpect(jsonPath("$.results").isEmpty());

        then(authenticationService).should().logout("dummy-token");
    }
}