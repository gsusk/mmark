package org.adso.minimarket.controller.api;

import jakarta.validation.Valid;
import org.adso.minimarket.config.UserPrincipal;
import org.adso.minimarket.dto.BasicUser;
import org.adso.minimarket.dto.DetailedUser;
import org.adso.minimarket.dto.UserUpdateRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

public interface UserController {
    ResponseEntity<BasicUser> getMe(UserPrincipal userPrincipal);

    ResponseEntity<DetailedUser> getProfile(UserPrincipal userPrincipal);

    ResponseEntity<DetailedUser> updateProfile(UserPrincipal userPrincipal, @Valid UserUpdateRequest userUpdateRequest);

    ResponseEntity<?> deleteUser(@AuthenticationPrincipal UserPrincipal userPrincipal);
}
