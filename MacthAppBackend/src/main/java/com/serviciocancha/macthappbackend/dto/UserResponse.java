package com.serviciocancha.macthappbackend.dto;

import com.serviciocancha.macthappbackend.model.User;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String nombres,
        String apellidos,
        String dni,
        String email,
        String role,
        LocalDate fechaNacimiento,
        OffsetDateTime createdAt
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getNombres(),
                user.getApellidos(),
                user.getDni(),
                user.getEmail(),
                user.getRole(),
                user.getFechaNacimiento(),
                user.getCreatedAt()
        );
    }
}
