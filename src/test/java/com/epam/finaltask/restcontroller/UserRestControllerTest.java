package com.epam.finaltask.restcontroller;

import com.epam.finaltask.dto.SignUpRequestDTO;
import com.epam.finaltask.dto.UserDTO;
import com.epam.finaltask.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserRestController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserRestControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @MockBean
    private UserService userService;

    @Test
    @DisplayName("POST /api/users/register → 201 CREATED with user data")
    void shouldRegisterUser() throws Exception {
        SignUpRequestDTO signup = new SignUpRequestDTO();
        signup.setUsername("viktorKovalenko");
        signup.setFirstName("Viktor");
        signup.setLastName("Kovalenko");
        signup.setEmail("viktor@example.com");
        signup.setPhoneNumber("+380123456789");
        signup.setPassword("Pass123!");
        signup.setConfirmPassword("Pass123!");

        UserDTO created = new UserDTO();
        created.setUsername("viktorKovalenko");
        created.setName("Viktor");
        created.setSurname("Kovalenko");
        created.setEmail("viktor@example.com");
        created.setPhoneNumber("+380123456789");

        given(userService.register(any(SignUpRequestDTO.class)))
                .willReturn(created);

        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signup))
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.statusCode").value("OK"))
                .andExpect(jsonPath("$.statusMessage").value("User registration successful"))
                .andExpect(jsonPath("$.results.username").value("viktorKovalenko"))
                .andExpect(jsonPath("$.results.name").value("Viktor"))
                .andExpect(jsonPath("$.results.surname").value("Kovalenko"));

        then(userService).should().register(any(SignUpRequestDTO.class));
    }

    @Test
    @DisplayName("GET /api/users/profile → 200 OK with profile data")
    void shouldGetUserProfile() throws Exception {
        UserDTO profile = new UserDTO();
        profile.setUsername("viktorKovalenko");
        profile.setName("Viktor");
        profile.setSurname("Kovalenko");
        profile.setEmail("viktor@example.com");
        profile.setPhoneNumber("+380123456789");

        given(userService.getUserByUsername("viktorKovalenko"))
                .willReturn(profile);

        Authentication auth =
                new UsernamePasswordAuthenticationToken("viktorKovalenko", null);

        mockMvc.perform(get("/api/users/profile")
                        .principal(auth)
                        .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value("OK"))
                .andExpect(jsonPath("$.statusMessage").value("User profile loaded successfully"))
                .andExpect(jsonPath("$.results.username").value("viktorKovalenko"))
                .andExpect(jsonPath("$.results.email").value("viktor@example.com"));

        then(userService).should().getUserByUsername("viktorKovalenko");
    }

    @Test
    @DisplayName("PUT /api/users/profile → 200 OK with updated data")
    void shouldUpdateUserProfile() throws Exception {
        UserDTO updateReq = new UserDTO();
        updateReq.setUsername("viktorKovalenko");
        updateReq.setName("Viktor");
        updateReq.setSurname("Kovalenko");
        updateReq.setEmail("viktor_new@example.com");
        updateReq.setPhoneNumber("+380987654321");

        UserDTO updated = new UserDTO();
        updated.setUsername("viktorKovalenko");
        updated.setName("Viktor");
        updated.setSurname("Kovalenko");
        updated.setEmail("viktor_new@example.com");
        updated.setPhoneNumber("+380987654321");

        given(userService.updateUser("viktorKovalenko", updateReq))
                .willReturn(updated);

        Authentication auth =
                new UsernamePasswordAuthenticationToken("viktorKovalenko", null);

        mockMvc.perform(put("/api/users/profile")
                        .principal(auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value("OK"))
                .andExpect(jsonPath("$.statusMessage").value("User profile updated successfully"))
                .andExpect(jsonPath("$.results.email").value("viktor_new@example.com"))
                .andExpect(jsonPath("$.results.phoneNumber").value("+380987654321"));

        then(userService).should().updateUser("viktorKovalenko", updateReq);
    }
}
