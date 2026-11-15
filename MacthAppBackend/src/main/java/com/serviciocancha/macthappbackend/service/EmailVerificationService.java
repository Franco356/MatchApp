package com.serviciocancha.macthappbackend.service;

import com.serviciocancha.macthappbackend.exception.ApiException;
import com.serviciocancha.macthappbackend.model.EmailVerificationToken;
import com.serviciocancha.macthappbackend.model.User;
import com.serviciocancha.macthappbackend.repository.EmailVerificationTokenRepository;
import com.serviciocancha.macthappbackend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
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

@Service
public class EmailVerificationService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final EmailVerificationTokenRepository tokenRepository;
    private final EmailService emailService;
    private final String frontendUrl;
    private final long expirationMinutes;

    public EmailVerificationService(
            UserRepository userRepository,
            EmailVerificationTokenRepository tokenRepository,
            EmailService emailService,
            @Value("${app.frontend.url:http://localhost:5173}") String frontendUrl,
            @Value("${app.email-verification.expiration-minutes:1440}") long expirationMinutes
    ) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.emailService = emailService;
        this.frontendUrl = frontendUrl;
        this.expirationMinutes = expirationMinutes;
    }

    @Transactional
    public String requestVerification(User user) {
        tokenRepository.deleteByUserId(user.getId());

        String rawToken = generateToken();
        EmailVerificationToken verificationToken = new EmailVerificationToken();
        verificationToken.setUser(user);
        verificationToken.setTokenHash(hashToken(rawToken));
        verificationToken.setExpiresAt(OffsetDateTime.now().plusMinutes(expirationMinutes));
        tokenRepository.save(verificationToken);

        String link = frontendUrl.replaceAll("/+$", "")
                + "/verificar-correo?token=" + rawToken;

        emailService.sendEmailVerification(user.getEmail(), link);
        return rawToken;
    }

    @Transactional
    public User verify(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "El enlace no es válido.");
        }

        EmailVerificationToken verificationToken = tokenRepository
                .findByTokenHash(hashToken(rawToken))
                .orElseThrow(() -> new ApiException(
                        HttpStatus.BAD_REQUEST,
                        "El enlace no es válido o ya expiró."
                ));

        if (verificationToken.getUsedAt() != null
                || verificationToken.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "El enlace no es válido o ya expiró."
            );
        }

        User user = verificationToken.getUser();
        user.setEmailVerified(true);
        userRepository.save(user);

        verificationToken.setUsedAt(OffsetDateTime.now());
        tokenRepository.save(verificationToken);
        return user;
    }

    @Transactional
    public void resend(String email) {
        String value = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        Optional<User> user = userRepository.findByEmail(value);

        if (user.isEmpty() || Boolean.TRUE.equals(user.get().getEmailVerified())) {
            return;
        }

        requestVerification(user.get());
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
