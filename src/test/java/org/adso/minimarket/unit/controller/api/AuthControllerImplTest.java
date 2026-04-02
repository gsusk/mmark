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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
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

    @Test
    @DisplayName("Login con credenciales validas retorna 200")
    void login_conCredencialesValidas_retorna200() throws Exception {
        LoginRequest request = new LoginRequest("test@gmail.com", "password123");
        AuthResponse response = new AuthResponse("access-token", "refresh-token");

        when(authService.loginUser(any(LoginRequest.class), any())).thenReturn(response);

        mockMvc.perform(post(AuthRoutes.LOGIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Login con email invalido retorna 400")
    void login_conEmailInvalido_retorna400() throws Exception {
        LoginRequest request = new LoginRequest("not-an-email", "");

        mockMvc.perform(post(AuthRoutes.LOGIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(authService);
    }

    @Test
    @DisplayName("Login con contraseña vacia retorna 400")
    void login_conContrasenaVacia_retorna400() throws Exception {
        LoginRequest request = new LoginRequest("test@test.com", "");

        mockMvc.perform(post(AuthRoutes.LOGIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(authService);
    }

    @Test
    @DisplayName("Login con credenciales incorrectas retorna 401")
    void login_conCredencialesIncorrectas_retorna401() throws Exception {
        LoginRequest request = new LoginRequest("bad@gmail.com", "wrongpass");

        when(authService.loginUser(any(LoginRequest.class), any()))
                .thenThrow(new WrongCredentialsException("unauthorized"));

        mockMvc.perform(post(AuthRoutes.LOGIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    @DisplayName("Registro con solicitud valida retorna 201")
    void registro_conSolicitudValida_retorna201() throws Exception {
        RegisterRequest request = new RegisterRequest("Test", "User", "test@gmail.com", "password123");
        AuthResponse response = new AuthResponse("access-token", "refresh-token");

        when(authService.register(any(RegisterRequest.class), any())).thenReturn(response);

        mockMvc.perform(post(AuthRoutes.REGISTER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("Registro con email invalido retorna 400")
    void registro_conEmailInvalido_retorna400() throws Exception {
        RegisterRequest request = new RegisterRequest("Test", "User", "not-an-email@@.com", "password123");

        mockMvc.perform(post(AuthRoutes.REGISTER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(authService);
    }

    @Test
    @DisplayName("Registro con contraseña corta retorna 400")
    void registro_conContrasenaCorta_retorna400() throws Exception {
        RegisterRequest request = new RegisterRequest("Test", "User", "test@gmail.com", "123");

        mockMvc.perform(post(AuthRoutes.REGISTER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(authService);
    }

    @Test
    @DisplayName("Registro con nombre en blanco retorna 400")
    void registro_conNombreEnBlanco_retorna400() throws Exception {
        RegisterRequest request = new RegisterRequest("", "User", "test@gmail.com", "password123");

        mockMvc.perform(post(AuthRoutes.REGISTER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(authService);
    }
}