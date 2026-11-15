package com.serviciocancha.macthappbackend;

import com.serviciocancha.macthappbackend.model.User;
import com.serviciocancha.macthappbackend.repository.UserRepository;
import com.serviciocancha.macthappbackend.service.EmailVerificationService;
import com.serviciocancha.macthappbackend.service.PasswordResetService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthFlowTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PasswordResetService passwordResetService;

    @Autowired
    private EmailVerificationService emailVerificationService;

    @Autowired
    private UserRepository userRepository;

    @Test
    void registerVerifyEmailLoginAndManageAccount() throws Exception {
        String email = "user-" + UUID.randomUUID() + "@test.pe";
        String dni = String.format("%08d", Math.floorMod(UUID.randomUUID().hashCode(), 100_000_000));
        String password = "secreta123";
        String newPassword = "nuevaClave123";

        String registerBody = """
                {
                  "nombres": "Ana",
                  "apellidos": "Pérez",
                  "dni": "%s",
                  "fechaNacimiento": "1995-05-10",
                  "email": "%s",
                  "password": "%s"
                }
                """.formatted(dni, email, password);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").isNotEmpty())
                .andExpect(jsonPath("$.user.email").value(email))
                .andExpect(jsonPath("$.token").doesNotExist());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s"}
                                """.formatted(email, password)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(
                        "Debes verificar tu correo antes de iniciar sesión."
                ));

        mockMvc.perform(post("/api/auth/resend-verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s"}
                                """.formatted(email)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").isNotEmpty());

        User registered = userRepository.findByEmail(email).orElseThrow();
        String verificationToken = emailVerificationService.requestVerification(registered);
        assertNotNull(verificationToken);

        MvcResult verifyResult = mockMvc.perform(post("/api/auth/verify-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"token":"%s"}
                                """.formatted(verificationToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.user.email").value(email))
                .andReturn();

        String token = objectMapper
                .readTree(verifyResult.getResponse().getContentAsString())
                .path("token")
                .asText();

        mockMvc.perform(post("/api/auth/verify-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"token":"%s"}
                                """.formatted(verificationToken)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("El enlace no es válido o ya expiró."));

        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.dni").value(dni));

        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s"}
                                """.formatted(email, password)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());

        mockMvc.perform(patch("/api/users/me/password")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currentPassword":"%s","newPassword":"%s"}
                                """.formatted(password, newPassword)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s"}
                                """.formatted(email, newPassword)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("El correo ya está registrado."));
    }

    @Test
    void requestAndResetPasswordWithToken() throws Exception {
        String email = "reset-" + UUID.randomUUID() + "@test.pe";
        String dni = String.format("%08d", Math.floorMod(UUID.randomUUID().hashCode(), 100_000_000));
        String oldPassword = "secreta123";
        String newPassword = "nuevaClave456";

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nombres": "Luis",
                                  "apellidos": "Torres",
                                  "dni": "%s",
                                  "fechaNacimiento": "1990-03-20",
                                  "email": "%s",
                                  "password": "%s"
                                }
                                """.formatted(dni, email, oldPassword)))
                .andExpect(status().isCreated());

        User user = userRepository.findByEmail(email).orElseThrow();
        user.setEmailVerified(true);
        userRepository.save(user);

        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"identifier":"no-existe@test.pe"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").isNotEmpty());

        String resetToken = passwordResetService.requestReset(email);
        assertNotNull(resetToken);

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"token":"%s","newPassword":"%s"}
                                """.formatted(resetToken, newPassword)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Contraseña actualizada correctamente."));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s"}
                                """.formatted(email, oldPassword)))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s"}
                                """.formatted(email, newPassword)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"token":"%s","newPassword":"otraClave789"}
                                """.formatted(resetToken)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("El enlace no es válido o ya expiró."));
    }
}
