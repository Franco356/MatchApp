package com.serviciocancha.macthappbackend.service;

import com.serviciocancha.macthappbackend.exception.ApiException;
import com.serviciocancha.macthappbackend.model.PasswordResetToken;
import com.serviciocancha.macthappbackend.model.User;
import com.serviciocancha.macthappbackend.repository.PasswordResetTokenRepository;
import com.serviciocancha.macthappbackend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
public class PasswordResetService {

    private static final Pattern DNI_PATTERN = Pattern.compile("^\\d{8}$");
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final String frontendUrl;
    private final long expirationMinutes;

    public PasswordResetService(
            UserRepository userRepository,
            PasswordResetTokenRepository tokenRepository,
            PasswordEncoder passwordEncoder,
            EmailService emailService,
            @Value("${app.frontend.url:http://localhost:5173}") String frontendUrl,
            @Value("${app.password-reset.expiration-minutes:30}") long expirationMinutes
    ) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.frontendUrl = frontendUrl;
        this.expirationMinutes = expirationMinutes;
    }

    @Transactional
    public String requestReset(String identifier) {
        Optional<User> user = findUser(identifier);

        if (user.isEmpty()) {
            return null;
        }

        User account = user.get();
        tokenRepository.deleteByUserId(account.getId());

        String rawToken = generateToken();
        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setUser(account);
        resetToken.setTokenHash(hashToken(rawToken));
        resetToken.setExpiresAt(OffsetDateTime.now().plusMinutes(expirationMinutes));
        tokenRepository.save(resetToken);

        String link = frontendUrl.replaceAll("/+$", "")
                + "/recuperar-contrasena?token=" + rawToken;

        emailService.sendPasswordReset(account.getEmail(), link);
        return rawToken;
    }

    @Transactional
    public void resetPassword(String rawToken, String newPassword) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "El enlace no es válido.");
        }
        if (newPassword == null || newPassword.length() < 8) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "La contraseña debe tener al menos 8 caracteres."
            );
        }

        PasswordResetToken resetToken = tokenRepository
                .findByTokenHash(hashToken(rawToken))
                .orElseThrow(() -> new ApiException(
                        HttpStatus.BAD_REQUEST,
                        "El enlace no es válido o ya expiró."
                ));

        if (resetToken.getUsedAt() != null
                || resetToken.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "El enlace no es válido o ya expiró."
            );
        }

        User user = resetToken.getUser();
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        resetToken.setUsedAt(OffsetDateTime.now());
        tokenRepository.save(resetToken);
    }

    private Optional<User> findUser(String identifier) {
        String value = identifier == null ? "" : identifier.trim();

        if (value.contains("@")) {
            return userRepository.findByEmail(value.toLowerCase(Locale.ROOT));
        }
        if (DNI_PATTERN.matcher(value).matches()) {
            return userRepository.findByDni(value);
        }
        return Optional.empty();
    }

    private String generateToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 no está disponible.", exception);
        }
    }
}
