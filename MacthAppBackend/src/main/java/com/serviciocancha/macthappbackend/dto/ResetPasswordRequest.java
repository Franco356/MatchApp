package com.serviciocancha.macthappbackend.dto;

public record ResetPasswordRequest(String token, String newPassword) {
}
