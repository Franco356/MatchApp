package com.serviciocancha.macthappbackend.service;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.serviciocancha.macthappbackend.dto.AuthResponse;
import com.serviciocancha.macthappbackend.dto.DniResponse;
import com.serviciocancha.macthappbackend.dto.LoginRequest;
import com.serviciocancha.macthappbackend.dto.RegisterRequest;
import com.serviciocancha.macthappbackend.dto.RegisterResponse;
import com.serviciocancha.macthappbackend.dto.UserResponse;
import com.serviciocancha.macthappbackend.exception.ApiException;
import com.serviciocancha.macthappbackend.model.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.serviciocancha.macthappbackend.repository.UserRepository;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.Period;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
public class AuthService {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final Pattern DNI_PATTERN = Pattern.compile("^\\d{8}$");

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final PasswordEncoder passwordEncoder;
    private final EmailVerificationService emailVerificationService;
    private final String googleClientId;
    private final String apiperuToken;

    public AuthService(
            UserRepository userRepository,
            JwtService jwtService,
            ObjectMapper objectMapper,
            PasswordEncoder passwordEncoder,
            EmailVerificationService emailVerificationService,
            @Value("${app.google.client-id:}") String googleClientId,
            @Value("${app.apiperu.token:}") String apiperuToken
    ) {
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.passwordEncoder = passwordEncoder;
        this.emailVerificationService = emailVerificationService;
        this.googleClientId = googleClientId;
        this.apiperuToken = apiperuToken;
    }

    public RegisterResponse register(RegisterRequest request) {
        String nombres = trim(request.nombres());
        String apellidos = trim(request.apellidos());
        String dni = trim(request.dni());
        String email = normalizeEmail(request.email());
        String password = request.password();

        if (nombres.length() < 2) {
            throw badRequest("Ingresa tus nombres.");
        }
        if (apellidos.length() < 2) {
            throw badRequest("Ingresa tus apellidos.");
        }
        if (!DNI_PATTERN.matcher(dni).matches()) {
            throw badRequest("El DNI debe tener exactamente 8 dígitos.");
        }
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw badRequest("Ingresa un correo electrónico válido.");
        }
        if (password == null || password.length() < 8) {
            throw badRequest("La contraseña debe tener al menos 8 caracteres.");
        }
        if (request.fechaNacimiento() == null) {
            throw badRequest("Ingresa tu fecha de nacimiento.");
        }
        if (Period.between(request.fechaNacimiento(), LocalDate.now()).getYears() < 18) {
            throw badRequest("Debes ser mayor de 18 años para registrarte.");
        }
        if (userRepository.existsByEmail(email)) {
            throw badRequest("El correo ya está registrado.");
        }
        if (userRepository.existsByDni(dni)) {
            throw badRequest("El DNI ya está registrado.");
        }

        User user = new User();
        user.setNombres(nombres);
        user.setApellidos(apellidos);
        user.setDni(dni);
        user.setFechaNacimiento(request.fechaNacimiento());
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setRole("user");
        user.setEmailVerified(false);
        user.setTermsAcceptedAt(OffsetDateTime.now());

        User saved = userRepository.save(user);
        emailVerificationService.requestVerification(saved);

        return new RegisterResponse(
                "Te enviamos un correo para verificar tu cuenta.",
                UserResponse.from(saved)
        );
    }

    public AuthResponse login(LoginRequest request) {
        String email = normalizeEmail(request.email());

        if (email.isEmpty() || request.password() == null || request.password().isEmpty()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Credenciales incorrectas.");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Credenciales incorrectas."));

        boolean matches;
        try {
            matches = passwordEncoder.matches(request.password(), user.getPasswordHash());
        } catch (IllegalArgumentException exception) {
            matches = false;
        }

        if (!matches) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Credenciales incorrectas.");
        }
        if (Boolean.FALSE.equals(user.getEmailVerified())) {
            throw new ApiException(
                    HttpStatus.FORBIDDEN,
                    "Debes verificar tu correo antes de iniciar sesión."
            );
        }

        return buildResponse(user);
    }

    public AuthResponse loginWithGoogle(String credential) {
        if (googleClientId == null || googleClientId.isBlank()) {
            throw new ApiException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "El inicio de sesión con Google no está configurado."
            );
        }
        if (credential == null || credential.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Falta la credencial de Google.");
        }

        JsonNode payload = getJson(
                "https://oauth2.googleapis.com/tokeninfo?id_token="
                        + URLEncoder.encode(credential, StandardCharsets.UTF_8),
                null
        );

        String audience = payload.path("aud").asText();
        String email = payload.path("email").asText();
        boolean emailVerified = payload.path("email_verified").asBoolean(false)
                || "true".equalsIgnoreCase(payload.path("email_verified").asText());

        if (!googleClientId.equals(audience) || email.isBlank() || !emailVerified) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Token de Google inválido.");
        }

        User user = userRepository.findByEmail(normalizeEmail(email))
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "No existe una cuenta con ese correo. Regístrate primero."
                ));

        if (Boolean.FALSE.equals(user.getEmailVerified())) {
            user.setEmailVerified(true);
            userRepository.save(user);
        }

        return buildResponse(user);
    }

    public AuthResponse verifyEmail(String token) {
        return buildResponse(emailVerificationService.verify(token));
    }

    public void resendVerification(String email) {
        emailVerificationService.resend(email);
    }

    public DniResponse verifyDni(String dni) {
        String value = trim(dni);

        if (!DNI_PATTERN.matcher(value).matches()) {
            throw badRequest("El DNI debe tener exactamente 8 dígitos.");
        }
        if (apiperuToken == null || apiperuToken.isBlank()) {
            throw new ApiException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "La validación con RENIEC no está configurada."
            );
        }

        JsonNode payload = getJson("https://apiperu.dev/api/dni/" + value, apiperuToken);

        if (!payload.path("success").asBoolean(false)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "No encontramos información para ese DNI.");
        }

        JsonNode data = payload.path("data");
        String nombres = data.path("nombres").asText("");
        String apellidos = (data.path("apellido_paterno").asText("") + " "
                + data.path("apellido_materno").asText("")).trim();

        if (nombres.isBlank()) {
            throw new ApiException(HttpStatus.NOT_FOUND, "No encontramos información para ese DNI.");
        }

        return new DniResponse(value, nombres, apellidos);
    }

    private AuthResponse buildResponse(User user) {
        return new AuthResponse(jwtService.generateToken(user), UserResponse.from(user));
    }

    private JsonNode getJson(String url, String bearerToken) {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url))
                .GET()
                .timeout(Duration.ofSeconds(20))
                .header("Accept", "application/json");

        if (bearerToken != null && !bearerToken.isBlank()) {
            builder.header("Authorization", "Bearer " + bearerToken);
        }

        try {
            HttpResponse<String> response = httpClient.send(
                    builder.build(),
                    HttpResponse.BodyHandlers.ofString()
            );

            if (response.statusCode() == 401 || response.statusCode() == 403) {
                throw new ApiException(
                        HttpStatus.UNAUTHORIZED,
                        "El servicio externo rechazó las credenciales."
                );
            }
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new ApiException(
                        HttpStatus.BAD_GATEWAY,
                        "No pudimos consultar el servicio externo."
                );
            }

            return objectMapper.readTree(response.body());
        } catch (ApiException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ApiException(
                    HttpStatus.BAD_GATEWAY,
                    "No pudimos consultar el servicio externo."
            );
        }
    }

    private ApiException badRequest(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, message);
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private String normalizeEmail(String value) {
        return trim(value).toLowerCase(Locale.ROOT);
    }
}
