package org.adso.minimarket.controller.api;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.adso.minimarket.dto.AuthResponse;
import org.adso.minimarket.dto.LoginRequest;
import org.adso.minimarket.dto.RegisterRequest;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

public interface AuthController {
    ResponseEntity<AuthResponse> login(@Valid LoginRequest loginRequest,
                                       UUID guestId,
                                       HttpServletResponse response);

    ResponseEntity<AuthResponse> register(@Valid RegisterRequest registerRequest,
                                          UUID guestId,
                                          HttpServletResponse response);

    ResponseEntity<AuthResponse> auth(String cookieRefreshToken);
}
