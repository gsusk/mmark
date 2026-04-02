package org.adso.minimarket.unit.service;

import org.adso.minimarket.config.UserPrincipal;
import org.adso.minimarket.dto.AuthResponse;
import org.adso.minimarket.dto.LoginRequest;
import org.adso.minimarket.dto.RegisterRequest;
import org.adso.minimarket.exception.TokenInvalidException;
import org.adso.minimarket.exception.WrongCredentialsException;
import org.adso.minimarket.models.user.User;
import org.adso.minimarket.service.AuthServiceImpl;
import org.adso.minimarket.service.CartService;
import org.adso.minimarket.service.JwtService;
import org.adso.minimarket.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @InjectMocks
    private AuthServiceImpl authService;

    @Mock
    private UserService userService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authManager;

    @Mock
    private CartService cartService;

    @Mock
    private JwtService jwtService;

    @Test
    @DisplayName("Registro con solicitud válida retorna par de tokens")
    void registro_conSolicitudValida_retornaParDeTokens() {
        var req = new RegisterRequest("Jorge", "Contreras", "jorge@test.com", "password123");
        User savedUser = new User("Jorge", "Contreras", "jorge@test.com", "encoded");
        ReflectionTestUtils.setField(savedUser, "id", 1L);

        when(passwordEncoder.encode(anyString())).thenReturn("encoded");
        when(userService.createUser(any(RegisterRequest.class))).thenReturn(savedUser);
        when(jwtService.generateAccessToken(any())).thenReturn("access-token");
        when(jwtService.generateRefreshToken(any())).thenReturn("refresh-token");

        AuthResponse result = authService.register(req, null);

        assertEquals("access-token", result.getAccessToken());
        verify(passwordEncoder).encode("password123");
        verify(userService).createUser(any(RegisterRequest.class));
        verify(jwtService).generateAccessToken(any());
        verify(jwtService).generateRefreshToken(any());
    }

    @Test
    @DisplayName("Registro con ID de invitado fusiona carritos")
    void registro_conIdInvitado_fusionaCarritos() {
        UUID guestId = UUID.randomUUID();
        var req = new RegisterRequest("Ana", "Lopez", "ana@test.com", "pass");
        User savedUser = new User("Ana", "Lopez", "ana@test.com", "encoded");
        ReflectionTestUtils.setField(savedUser, "id", 1L);

        when(passwordEncoder.encode(anyString())).thenReturn("encoded");
        when(userService.createUser(any(RegisterRequest.class))).thenReturn(savedUser);
        when(jwtService.generateAccessToken(any())).thenReturn("at");
        when(jwtService.generateRefreshToken(any())).thenReturn("rt");

        authService.register(req, guestId);

        verify(cartService).mergeCarts(1L, guestId);
    }

    @Test
    @DisplayName("Registro sin ID de invitado no fusiona carritos")
    void registro_sinIdInvitado_noFusionaCarritos() {
        var req = new RegisterRequest("Ana", "Lopez", "ana@test.com", "pass");
        User savedUser = new User("Ana", "Lopez", "ana@test.com", "encoded");

        when(passwordEncoder.encode(anyString())).thenReturn("encoded");
        when(userService.createUser(any(RegisterRequest.class))).thenReturn(savedUser);
        when(jwtService.generateAccessToken(any())).thenReturn("at");
        when(jwtService.generateRefreshToken(any())).thenReturn("rt");

        authService.register(req, null);

        verifyNoInteractions(cartService);
    }

    @Test
    @DisplayName("Login de usuario con credenciales válidas retorna par de tokens")
    void loginUsuario_conCredencialesValidas_retornaParDeTokens() {
        var req = new LoginRequest("test@test.com", "password");
        User user = new User("Test", "User", "test@test.com", "encoded");
        UserPrincipal principal = new UserPrincipal(user);

        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(principal);
        when(authManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(auth);
        when(jwtService.generateAccessToken(principal)).thenReturn("access");
        when(jwtService.generateRefreshToken(principal)).thenReturn("refresh");

        AuthResponse result = authService.loginUser(req, null);

        assertEquals("access", result.getAccessToken());
        verify(authManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    @DisplayName("Login de usuario con credenciales incorrectas lanza WrongCredentialsException")
    void loginUsuario_conCredencialesIncorrectas_lanzaWrongCredentialsException() {
        var req = new LoginRequest("bad@test.com", "wrongpass");

        when(authManager.authenticate(any())).thenThrow(new BadCredentialsException("bad"));

        assertThrows(WrongCredentialsException.class, () -> authService.loginUser(req, null));
        verifyNoInteractions(jwtService, cartService);
    }

    @Test
    @DisplayName("Login de usuario con ID de invitado fusiona carritos")
    void loginUsuario_conIdInvitado_fusionaCarritos() {
        UUID guestId = UUID.randomUUID();
        var req = new LoginRequest("test@test.com", "password");
        User user = new User("Test", "User", "test@test.com", "enc");
        ReflectionTestUtils.setField(user, "id", 1L);
        UserPrincipal principal = new UserPrincipal(user);

        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(principal);
        when(authManager.authenticate(any())).thenReturn(auth);
        when(jwtService.generateAccessToken(any())).thenReturn("at");
        when(jwtService.generateRefreshToken(any())).thenReturn("rt");

        authService.loginUser(req, guestId);

        verify(cartService).mergeCarts(user.getId(), guestId);
    }

    @Test
    @DisplayName("Refresh con token valido retorna nuevo token de acceso")
    void refresh_conTokenValido_retornaNuevoTokenDeAcceso() {
        String refreshToken = "valid-refresh-token";
        User user = new User("Test", "User", "test@test.com", "pass");

        when(jwtService.extractRefreshUsername(refreshToken)).thenReturn("test@test.com");
        when(userService.getUserInternalByEmail("test@test.com")).thenReturn(user);
        when(jwtService.isRefreshTokenValid(eq(refreshToken), any())).thenReturn(true);
        when(jwtService.generateAccessToken(any())).thenReturn("new-access-token");

        AuthResponse result = authService.refresh(refreshToken);

        assertEquals("new-access-token", result.getAccessToken());
        assertNull(result.getRefreshToken());
    }

    @Test
    @DisplayName("Refresh con email nulo lanza TokenInvalidException")
    void refresh_conEmailNulo_lanzaTokenInvalidException() {
        when(jwtService.extractRefreshUsername(anyString())).thenReturn(null);

        assertThrows(TokenInvalidException.class, () -> authService.refresh("bad-token"));
        verifyNoInteractions(userService);
    }

    @Test
    @DisplayName("Refresh con token inválido lanza TokenInvalidException")
    void refresh_conTokenInvalido_lanzaTokenInvalidException() {
        String refreshToken = "invalid-token";
        User user = new User("Test", "User", "test@test.com", "pass");

        when(jwtService.extractRefreshUsername(refreshToken)).thenReturn("test@test.com");
        when(userService.getUserInternalByEmail("test@test.com")).thenReturn(user);
        when(jwtService.isRefreshTokenValid(eq(refreshToken), any())).thenReturn(false);

        assertThrows(TokenInvalidException.class, () -> authService.refresh(refreshToken));
        verify(jwtService, never()).generateAccessToken(any());
    }
}
