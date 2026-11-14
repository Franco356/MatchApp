package com.serviciocancha.macthappbackend.controller;

import com.serviciocancha.macthappbackend.dto.AuthResponse;
import com.serviciocancha.macthappbackend.dto.DniResponse;
import com.serviciocancha.macthappbackend.dto.ForgotPasswordRequest;
import com.serviciocancha.macthappbackend.dto.GoogleLoginRequest;
import com.serviciocancha.macthappbackend.dto.LoginRequest;
import com.serviciocancha.macthappbackend.dto.RegisterRequest;
import com.serviciocancha.macthappbackend.dto.ResetPasswordRequest;
import com.serviciocancha.macthappbackend.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.serviciocancha.macthappbackend.service.AuthService;
import com.serviciocancha.macthappbackend.service.PasswordResetService;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final PasswordResetService passwordResetService;

    public AuthController(AuthService authService, PasswordResetService passwordResetService) {
        this.authService = authService;
        this.passwordResetService = passwordResetService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/google")
    public ResponseEntity<AuthResponse> loginWithGoogle(@RequestBody GoogleLoginRequest request) {
        return ResponseEntity.ok(authService.loginWithGoogle(request.credential()));
    }

    @GetMapping("/dni/{dni}")
    public ResponseEntity<DniResponse> verifyDni(@PathVariable String dni) {
        return ResponseEntity.ok(authService.verifyDni(dni));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(
            @RequestBody ForgotPasswordRequest request
    ) {
        if (request == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Datos inválidos.");
        }

        passwordResetService.requestReset(request.identifier());
        return ResponseEntity.ok(Map.of(
                "message",
                "Si existe una cuenta con ese dato, enviamos un enlace de recuperación."
        ));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(
            @RequestBody ResetPasswordRequest request
    ) {
        if (request == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Datos inválidos.");
        }

        passwordResetService.resetPassword(request.token(), request.newPassword());
        return ResponseEntity.ok(Map.of(
                "message",
                "Contraseña actualizada correctamente."
        ));
    }
}
