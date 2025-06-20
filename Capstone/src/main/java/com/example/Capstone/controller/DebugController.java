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
            response.put("timestamp", LocalDateTime.now());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Errore nel reset rate limit: {}", e.getMessage());

            Map<String, Object> response = new HashMap<>();
            response.put("error", "Errore nel reset: " + e.getMessage());
            response.put("timestamp", LocalDateTime.now());

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
            response.put("timestamp", LocalDateTime.now());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("error", "Errore nel controllo status: " + e.getMessage());
            response.put("timestamp", LocalDateTime.now());
            return ResponseEntity.status(500).body(response);
        }
    }

    @GetMapping("/email-verification-status/{email}")
    public ResponseEntity<?> checkEmailVerificationStatus(@PathVariable String email) {
        try {
            User user = userRepository.findByEmail(email).orElse(null);

            Map<String, Object> response = new HashMap<>();
            response.put("timestamp", LocalDateTime.now());

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
                response.put("accountLocked", user.isAccountLocked());
                response.put("failedLoginAttempts", user.getFailedLoginAttempts());
            }

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("error", "Errore nel controllo status: " + e.getMessage());
            response.put("timestamp", LocalDateTime.now());
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
            response.put("timestamp", LocalDateTime.now());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Errore nella verifica manuale per {}: {}", email, e.getMessage());

            Map<String, Object> response = new HashMap<>();
            response.put("error", "Errore nella verifica manuale: " + e.getMessage());
            response.put("timestamp", LocalDateTime.now());

            return ResponseEntity.status(500).body(response);
        }
    }

    @GetMapping("/list-tokens/{email}")
    public ResponseEntity<?> listTokensForEmail(@PathVariable String email) {
        try {
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Utente non trovato"));

            // Cerca token di verifica
            EmailVerificationToken token = tokenRepository.findByUser(user).orElse(null);

            Map<String, Object> response = new HashMap<>();
            response.put("email", email);
            response.put("emailVerified", user.getEmailVerified());
            response.put("enabled", user.getEnabled());
            response.put("timestamp", LocalDateTime.now());

            if (token != null) {
                Map<String, Object> tokenInfo = new HashMap<>();
                tokenInfo.put("token", token.getToken());
                tokenInfo.put("createdAt", token.getCreatedAt());
                tokenInfo.put("expiresAt", token.getExpiresAt());
                tokenInfo.put("used", token.getUsed());
                tokenInfo.put("expired", token.isExpired());
                tokenInfo.put("valid", token.isValid());
                response.put("verificationToken", tokenInfo);
            } else {
                response.put("verificationToken", null);
                response.put("message", "Nessun token di verifica trovato");
            }

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("error", "Errore nel controllo: " + e.getMessage());
            response.put("timestamp", LocalDateTime.now());
            return ResponseEntity.status(500).body(response);
        }
    }

    @DeleteMapping("/cleanup-expired-tokens")
    public ResponseEntity<?> cleanupExpiredTokens() {
        try {
            LocalDateTime now = LocalDateTime.now();
            tokenRepository.deleteExpiredTokens(now);

            log.info("Cleanup manuale dei token scaduti completato");

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Cleanup dei token scaduti completato");
            response.put("timestamp", now);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Errore nel cleanup manuale: {}", e.getMessage());

            Map<String, Object> response = new HashMap<>();
            response.put("error", "Errore nel cleanup: " + e.getMessage());
            response.put("timestamp", LocalDateTime.now());

            return ResponseEntity.status(500).body(response);
        }
    }

    @GetMapping("/test-connection")
    public ResponseEntity<?> testConnection() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "OK");
        response.put("message", "Debug controller is working");
        response.put("timestamp", LocalDateTime.now());
        response.put("server", "Capstone Backend");

        return ResponseEntity.ok(response);
    }
}