package org.adso.minimarket.service;

import org.adso.minimarket.config.UserPrincipal;
import org.adso.minimarket.dto.AuthResponse;
import org.adso.minimarket.dto.LoginRequest;
import org.adso.minimarket.dto.RegisterRequest;
import org.adso.minimarket.exception.TokenInvalidException;
import org.adso.minimarket.exception.WrongCredentialsException;
import org.adso.minimarket.models.user.User;
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

    // ── register ──────────────────────────────────────────────────────────────

    @Test
    void register_withValidRequest_returnsTokenPair() {
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
    void register_withGuestId_mergesCarts() {
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
    void register_withoutGuestId_doesNotMergeCarts() {
        var req = new RegisterRequest("Ana", "Lopez", "ana@test.com", "pass");
        User savedUser = new User("Ana", "Lopez", "ana@test.com", "encoded");

        when(passwordEncoder.encode(anyString())).thenReturn("encoded");
        when(userService.createUser(any(RegisterRequest.class))).thenReturn(savedUser);
        when(jwtService.generateAccessToken(any())).thenReturn("at");
        when(jwtService.generateRefreshToken(any())).thenReturn("rt");

        authService.register(req, null);

        verifyNoInteractions(cartService);
    }

    // ── loginUser ─────────────────────────────────────────────────────────────

    @Test
    void loginUser_withValidCredentials_returnsTokenPair() {
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
    void loginUser_withWrongCredentials_throwsWrongCredentialsException() {
        var req = new LoginRequest("bad@test.com", "wrongpass");

        when(authManager.authenticate(any())).thenThrow(new BadCredentialsException("bad"));

        assertThrows(WrongCredentialsException.class, () -> authService.loginUser(req, null));
        verifyNoInteractions(jwtService, cartService);
    }

    @Test
    void loginUser_withGuestId_mergesCarts() {
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

    // ── refresh ───────────────────────────────────────────────────────────────

    @Test
    void refresh_withValidToken_returnsNewAccessToken() {
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
    void refresh_withNullEmail_throwsTokenInvalidException() {
        when(jwtService.extractRefreshUsername(anyString())).thenReturn(null);

        assertThrows(TokenInvalidException.class, () -> authService.refresh("bad-token"));
        verifyNoInteractions(userService);
    }

    @Test
    void refresh_withInvalidToken_throwsTokenInvalidException() {
        String refreshToken = "invalid-token";
        User user = new User("Test", "User", "test@test.com", "pass");

        when(jwtService.extractRefreshUsername(refreshToken)).thenReturn("test@test.com");
        when(userService.getUserInternalByEmail("test@test.com")).thenReturn(user);
        when(jwtService.isRefreshTokenValid(eq(refreshToken), any())).thenReturn(false);

        assertThrows(TokenInvalidException.class, () -> authService.refresh(refreshToken));
        verify(jwtService, never()).generateAccessToken(any());
    }
}
