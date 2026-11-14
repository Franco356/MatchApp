package com.serviciocancha.macthappbackend.dto;

public record ChangePasswordRequest(String currentPassword, String newPassword) {
}
