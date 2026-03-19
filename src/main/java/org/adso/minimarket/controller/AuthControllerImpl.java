package org.adso.minimarket.unit.api;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.adso.minimarket.constant.AuthRoutes;
import org.adso.minimarket.dto.AuthResponse;
import org.adso.minimarket.dto.LoginRequest;
import org.adso.minimarket.dto.RegisterRequest;
import org.adso.minimarket.service.AuthService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
public class AuthControllerImpl implements AuthController {

    private final AuthService authService;

    public AuthControllerImpl(AuthService authService) {
        this.authService = authService;
    }

    @Override
    @PostMapping(AuthRoutes.LOGIN)
    public ResponseEntity<AuthResponse> login(@RequestBody @Valid LoginRequest loginRequest,
                                              @CookieValue(name = "CGUESTID", required = false) UUID guestId,
                                              HttpServletResponse response) {
        AuthResponse auth = this.authService.loginUser(loginRequest, guestId);
        addRefreshTokenCookie(response, auth.getRefreshToken());
        clearGuestCookie(response);
        return ResponseEntity.ok(auth);
    }

    @Override
    @PostMapping(AuthRoutes.REGISTER)
    public ResponseEntity<AuthResponse> register(@RequestBody @Valid RegisterRequest registerRequest,
                                                 @CookieValue(name = "CGUESTID", required = false) UUID guestId,
                                                 HttpServletResponse response) {

        AuthResponse auth = this.authService.register(registerRequest, guestId);
        addRefreshTokenCookie(response, auth.getRefreshToken());
        clearGuestCookie(response);
        return new ResponseEntity<>(auth, HttpStatus.CREATED);
    }

    @Override
    @PostMapping(AuthRoutes.REFRESH_TOKEN)
    public ResponseEntity<AuthResponse> auth(
            @CookieValue(name = "X-REFRESH-TOKEN", required = false) String cookieRefreshToken
    ) {
        String token = cookieRefreshToken;

        if (token == null) {
            throw new org.adso.minimarket.exception.TokenInvalidException("Refresh token is missing");
        }

        return new ResponseEntity<>(this.authService.refresh(token), HttpStatus.OK);
    }

    private void clearGuestCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie
                .from("CGUESTID", "")
                .maxAge(0)
                .path("/")
                .httpOnly(true)
                .sameSite("Lax")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private void addRefreshTokenCookie(HttpServletResponse response, String token) {
        ResponseCookie cookie = ResponseCookie
                .from("X-REFRESH-TOKEN", token)
                .maxAge(3600)
                .path("/auth/refresh")
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

}
