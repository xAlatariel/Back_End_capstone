package com.example.Capstone.controller;

import com.example.Capstone.entity.EmailVerificationToken;
import com.example.Capstone.entity.User;
import com.example.Capstone.repository.EmailVerificationTokenRepository;
import com.example.Capstone.repository.UserRepository;
import com.example.Capstone.service.RateLimitingService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/debug")
@RequiredArgsConstructor
@Profile("dev") // Solo in ambiente development
public class DebugController {

    private final RateLimitingService rateLimitingService;
    private final UserRepository userRepository;
    private final EmailVerificationTokenRepository tokenRepository;

    @PostMapping("/reset-rate-limit")
    public ResponseEntity<?> resetRateLimit(HttpServletRequest request) {
        try {
            String clientIp = rateLimitingService.getClientIp(request);
            rateLimitingService.resetRateLimitForIp(clientIp);

            log.info("Rate limit reset per IP: {}", clientIp);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Rate limit reset per IP: " + clientIp);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Errore nel reset rate limit: {}", e.getMessage());

            Map<String, Object> response = new HashMap<>();
            response.put("error", "Errore nel reset: " + e.getMessage());

            return ResponseEntity.status(500).body(response);
        }
    }

    @GetMapping("/rate-limit-status")
    public ResponseEntity<?> getRateLimitStatus(HttpServletRequest request) {
        try {
            String clientIp = rateLimitingService.getClientIp(request);

            Map<String, Object> response = new HashMap<>();
            response.put("clientIp", clientIp);
            response.put("loginTokens", rateLimitingService.getRemainingTokens(clientIp, "login"));
            response.put("registrationTokens", rateLimitingService.getRemainingTokens(clientIp, "registration"));
            response.put("emailResendTokens", rateLimitingService.getRemainingTokens(clientIp, "email_resend"));
            response.put("apiTokens", rateLimitingService.getRemainingTokens(clientIp, "api"));

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("error", "Errore nel controllo status: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    @GetMapping("/email-verification-status/{email}")
    public ResponseEntity<?> checkEmailVerificationStatus(@PathVariable String email) {
        try {
            User user = userRepository.findByEmail(email).orElse(null);

            Map<String, Object> response = new HashMap<>();

            if (user == null) {
                response.put("email", email);
                response.put("exists", false);
                response.put("message", "Utente non trovato");
            } else {
                response.put("email", email);
                response.put("exists", true);
                response.put("emailVerified", user.getEmailVerified());
                response.put("enabled", user.getEnabled());
                response.put("accountStatus", user.getAccountStatus().toString());
                response.put("createdAt", user.getCreatedAt());
                response.put("emailVerifiedAt", user.getEmailVerifiedAt());
            }

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("error", "Errore nel controllo status: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    @GetMapping("/manual-verify-email/{email}")
    public ResponseEntity<?> manualVerifyEmailGet(@PathVariable String email) {
        return manualVerifyEmail(email);
    }

    @PostMapping("/manual-verify-email/{email}")
    public ResponseEntity<?> manualVerifyEmail(@PathVariable String email) {
        try {
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Utente non trovato"));

            // Verifica manualmente l'email
            user.verifyEmail();
            userRepository.save(user);

            log.info("Email verificata manualmente per: {}", email);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Email verificata manualmente per: " + email);
            response.put("emailVerified", user.getEmailVerified());
            response.put("enabled", user.getEnabled());
            response.put("accountStatus", user.getAccountStatus().toString());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Errore nella verifica manuale per {}: {}", email, e.getMessage());

            Map<String, Object> response = new HashMap<>();
            response.put("error", "Errore nella verifica manuale: " + e.getMessage());

            return ResponseEntity.status(500).body(response);
        }
    }

    @GetMapping("/list-tokens/{email}")
    public ResponseEntity<?> listTokensForEmail(@PathVariable String email) {
        try {
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Utente non trovato"));

            // Semplificato senza List di token per evitare errori
            Map<String, Object> response = new HashMap<>();
            response.put("email", email);
            response.put("message", "Utente trovato");
            response.put("emailVerified", user.getEmailVerified());
            response.put("enabled", user.getEnabled());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("error", "Errore nel controllo: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    @DeleteMapping("/cleanup-expired-tokens")
    public ResponseEntity<?> cleanupExpiredTokens() {
        try {
            // Semplificato per evitare errori con metodi non esistenti
            log.info("Cleanup manuale richiesto");

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Cleanup richiesto - implementazione da completare");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Errore nel cleanup manuale: {}", e.getMessage());

            Map<String, Object> response = new HashMap<>();
            response.put("error", "Errore nel cleanup: " + e.getMessage());

            return ResponseEntity.status(500).body(response);
        }
    }
}