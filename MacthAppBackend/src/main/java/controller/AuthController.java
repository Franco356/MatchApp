package controller;

import model.User;
import repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:5173") // Permite la conexión con tu frontend de Vite/React
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody User user) {
        if (userRepository.existsByEmail(user.getEmail())) {
            return ResponseEntity.badRequest().body("El correo ya está registrado.");
        }
        if (userRepository.existsByDni(user.getDni())) {
            return ResponseEntity.badRequest().body("El DNI ya está registrado.");
        }
        user.setTermsAcceptedAt(OffsetDateTime.now());
        User saved = userRepository.save(user);
        return ResponseEntity.ok(saved);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody User loginData) {
        Optional<User> found = userRepository.findByEmail(loginData.getEmail());
        if (found.isPresent() && found.get().getPasswordHash().equals(loginData.getPasswordHash())) {
            return ResponseEntity.ok(found.get());
        }
        return ResponseEntity.status(401).body("Credenciales incorrectas.");
    }
}