package com.serviciocancha.macthappbackend.service;

import com.serviciocancha.macthappbackend.dto.ChangePasswordRequest;
import com.serviciocancha.macthappbackend.dto.UpdateProfileRequest;
import com.serviciocancha.macthappbackend.dto.UserResponse;
import com.serviciocancha.macthappbackend.exception.ApiException;
import com.serviciocancha.macthappbackend.model.User;
import com.serviciocancha.macthappbackend.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public UserResponse getById(UUID id) {
        return UserResponse.from(findUser(id));
    }

    @Transactional
    public UserResponse updateProfile(UUID id, UpdateProfileRequest request) {
        User user = findUser(id);
        String nombres = trim(request.nombres());
        String apellidos = trim(request.apellidos());

        if (!nombres.isEmpty()) {
            if (nombres.length() < 2) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Ingresa tus nombres.");
            }
            user.setNombres(nombres);
        }

        if (!apellidos.isEmpty()) {
            if (apellidos.length() < 2) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Ingresa tus apellidos.");
            }
            user.setApellidos(apellidos);
        }

        return UserResponse.from(userRepository.save(user));
    }

    @Transactional
    public void changePassword(UUID id, ChangePasswordRequest request) {
        User user = findUser(id);

        if (request.currentPassword() == null
                || !passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "La contraseña actual no es correcta.");
        }

        if (request.newPassword() == null || request.newPassword().length() < 8) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "La nueva contraseña debe tener al menos 8 caracteres."
            );
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
    }

    private User findUser(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "Usuario no encontrado."
                ));
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
