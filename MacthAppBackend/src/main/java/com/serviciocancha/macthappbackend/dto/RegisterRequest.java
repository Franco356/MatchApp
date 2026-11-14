package com.serviciocancha.macthappbackend.dto;

import java.time.LocalDate;

public record RegisterRequest(
        String nombres,
        String apellidos,
        String dni,
        LocalDate fechaNacimiento,
        String email,
        String password
) {
}
