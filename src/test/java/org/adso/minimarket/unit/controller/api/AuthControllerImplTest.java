package org.adso.minimarket.unit.controller.api;

import org.adso.minimarket.constant.AuthRoutes;
import org.adso.minimarket.controller.api.AuthControllerImpl;
import org.adso.minimarket.dto.AuthResponse;
import org.adso.minimarket.dto.LoginRequest;
import org.adso.minimarket.dto.RegisterRequest;
import org.adso.minimarket.exception.WrongCredentialsException;
import org.adso.minimarket.service.AppUserDetailsServiceImpl;
import org.adso.minimarket.service.AuthService;
import org.adso.minimarket.service.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@WebMvcTest(AuthControllerImpl.class)
@ExtendWith(SpringExtension.class)
class AuthControllerImplTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private AppUserDetailsServiceImpl userDetailsService;

    @Autowired
    private ObjectMapper objectMapper;

    // POST /auth/login

    @Test
    void login_withValidCredentials_returns200() throws Exception {
        LoginRequest request = new LoginRequest("test@gmail.com", "password123");
        AuthResponse response = new AuthResponse("access-token", "refresh-token");

        when(authService.loginUser(any(LoginRequest.class), any())).thenReturn(response);

        mockMvc.perform(post(AuthRoutes.LOGIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void login_withInvalidEmail_returns400() throws Exception {
        LoginRequest request = new LoginRequest("not-an-email", "");

        mockMvc.perform(post(AuthRoutes.LOGIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(authService);
    }

    @Test
    void login_withEmptyPassword_returns400() throws Exception {
        LoginRequest request = new LoginRequest("test@test.com", "");

        mockMvc.perform(post(AuthRoutes.LOGIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(authService);
    }

    @Test
    void login_whenWrongCredentials_returns401() throws Exception {
        LoginRequest request = new LoginRequest("bad@gmail.com", "wrongpass");

        when(authService.loginUser(any(LoginRequest.class), any()))
                .thenThrow(new WrongCredentialsException("unauthorized"));

        mockMvc.perform(post(AuthRoutes.LOGIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").exists());
    }

    // POST /auth/register

    @Test
    void register_withValidRequest_returns201() throws Exception {
        RegisterRequest request = new RegisterRequest("Test", "User", "test@gmail.com", "password123");
        AuthResponse response = new AuthResponse("access-token", "refresh-token");

        when(authService.register(any(RegisterRequest.class), any())).thenReturn(response);

        mockMvc.perform(post(AuthRoutes.REGISTER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    void register_withInvalidEmail_returns400() throws Exception {
        RegisterRequest request = new RegisterRequest("Test", "User", "not-an-email@@.com", "password123");

        mockMvc.perform(post(AuthRoutes.REGISTER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(authService);
    }

    @Test
    void register_withShortPassword_returns400() throws Exception {
        RegisterRequest request = new RegisterRequest("Test", "User", "test@gmail.com", "123");

        mockMvc.perform(post(AuthRoutes.REGISTER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(authService);
    }

    @Test
    void register_withBlankFirstName_returns400() throws Exception {
        RegisterRequest request = new RegisterRequest("", "User", "test@gmail.com", "password123");

        mockMvc.perform(post(AuthRoutes.REGISTER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(authService);
    }
}