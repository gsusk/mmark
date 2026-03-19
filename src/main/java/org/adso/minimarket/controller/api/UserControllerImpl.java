package org.adso.minimarket.controller.api;

import jakarta.validation.Valid;
import org.adso.minimarket.config.UserPrincipal;
import org.adso.minimarket.dto.BasicUser;
import org.adso.minimarket.dto.DetailedUser;
import org.adso.minimarket.dto.UserUpdateRequest;
import org.adso.minimarket.mappers.UserMapper;
import org.adso.minimarket.service.UserService;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
public class UserControllerImpl implements UserController {

    private final UserService userService;
    private final UserMapper userMapper;

    public UserControllerImpl(UserService userService, UserMapper userMapper) {
        this.userService = userService;
        this.userMapper = userMapper;
    }

    @Override
    @GetMapping("/user/me")
    public ResponseEntity<BasicUser> getMe(@AuthenticationPrincipal(errorOnInvalidType = true) UserPrincipal userPrincipal) {
        return ResponseEntity.ok(userService.getBasicUserById(userPrincipal.getId()));
    }

    @Override
    @GetMapping("/user/profile")
    public ResponseEntity<DetailedUser> getProfile(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        return ResponseEntity.ok(userMapper.toDetailedUserDto(userPrincipal.getUser()));
    }

    @Override
    @PutMapping("/user/profile")
    public ResponseEntity<DetailedUser> updateProfile(@AuthenticationPrincipal UserPrincipal userPrincipal,
                                                      @RequestBody @Valid UserUpdateRequest userUpdateRequest) {
        return ResponseEntity.ok(userService.updateUserProfile(userUpdateRequest, userPrincipal.getId()));
    }


    @Override
    @DeleteMapping("/user")
    public ResponseEntity<?> deleteUser(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        userService.deleteUser(userPrincipal.getId());
        ResponseCookie.from("X-REFRESH-TOKEN").build();
        ResponseCookie.from("CGUESTID").build();
        return ResponseEntity.ok().build();
    }
}