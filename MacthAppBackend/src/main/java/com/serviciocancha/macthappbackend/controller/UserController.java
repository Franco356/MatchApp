package com.serviciocancha.macthappbackend.controller;

import com.serviciocancha.macthappbackend.dto.ChangePasswordRequest;
import com.serviciocancha.macthappbackend.dto.UpdateProfileRequest;
import com.serviciocancha.macthappbackend.dto.UserResponse;
import com.serviciocancha.macthappbackend.exception.ApiException;
import com.serviciocancha.macthappbackend.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public UserResponse me(Authentication authentication) {
        return userService.getById(currentUserId(authentication));
    }

    @PatchMapping("/me")
    public UserResponse updateMe(
            Authentication authentication,
            @RequestBody UpdateProfileRequest request
    ) {
        return userService.updateProfile(currentUserId(authentication), request);
    }

    @PatchMapping("/me/password")
    public Map<String, String> changePassword(
            Authentication authentication,
            @RequestBody ChangePasswordRequest request
    ) {
        userService.changePassword(currentUserId(authentication), request);
        return Map.of("message", "Contraseña actualizada correctamente.");
    }

    private UUID currentUserId(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "No autorizado.");
        }

        try {
            return UUID.fromString(authentication.getName());
        } catch (IllegalArgumentException exception) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "No autorizado.");
        }
    }
}
