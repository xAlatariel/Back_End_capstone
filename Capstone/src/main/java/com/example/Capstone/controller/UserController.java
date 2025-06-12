package com.example.Capstone.controller;

import com.example.Capstone.dto.*;
import com.example.Capstone.entity.User;
import com.example.Capstone.exception.UserAlreadyExistsException;
import com.example.Capstone.exception.UserNotFoundException;
import com.example.Capstone.service.EmailVerificationService;
import com.example.Capstone.service.RateLimitingService;
import com.example.Capstone.service.RecaptchaService;
import com.example.Capstone.service.UserService;
import com.example.Capstone.utils.JWTTools;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final EmailVerificationService emailVerificationService;
    private final RecaptchaService recaptchaService;
    private final RateLimitingService rateLimitingService;
    private final JWTTools jwt;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(
            @Valid @RequestBody UserRegistrationDTO registrationDTO,
            @RequestHeader(value = "g-recaptcha-response", required = false) String recaptchaResponse,
            HttpServletRequest request,
            BindingResult validation
    ) {
        try {
            // Rate limiting check
            String clientIp = rateLimitingService.getClientIp(request);
            if (!rateLimitingService.isRegistrationAllowed(clientIp)) {
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                        .body(new APIResponse<>(APIStatus.ERROR, "Troppi tentativi di registrazione. Riprova più tardi."));
            }

            // Validation check
            if (validation.hasErrors()) {
                String errors = validation.getAllErrors().stream()
                        .map(error -> error.getDefaultMessage())
                        .collect(Collectors.joining(", "));
                return ResponseEntity.badRequest()
                        .body(new APIResponse<>(APIStatus.ERROR, "Errori di validazione: " + errors));
            }

            // reCAPTCHA verification (se abilitato)
            if (recaptchaService.isRecaptchaEnabled() && !recaptchaService.verifyRecaptcha(recaptchaResponse)) {
                log.warn("Tentativo di registrazione con reCAPTCHA non valido da IP: {}", clientIp);
                return ResponseEntity.badRequest()
                        .body(new APIResponse<>(APIStatus.ERROR, "Verifica reCAPTCHA fallita"));
            }

            UserDTO createdUser = userService.registerUser(registrationDTO);

            log.info("Utente registrato con successo: {} da IP: {}", createdUser.getEmail(), clientIp);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new APIResponse<>(APIStatus.SUCCESS,
                            "Registrazione completata! Controlla la tua email per verificare l'account."));

        } catch (UserAlreadyExistsException e) {
            log.warn("Tentativo di registrazione con email esistente: {}", registrationDTO.getEmail());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new APIResponse<>(APIStatus.ERROR, e.getMessage()));
        } catch (Exception e) {
            log.error("Errore durante la registrazione", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new APIResponse<>(APIStatus.ERROR, "Registrazione fallita. Riprova più tardi."));
        }
    }

    @PostMapping("/verify-email")
    public ResponseEntity<?> verifyEmail(@Valid @RequestBody EmailVerificationDTO verificationDTO) {
        try {
            boolean verified = emailVerificationService.verifyEmail(verificationDTO.getToken());

            if (verified) {
                log.info("Email verificata con successo per token: {}", verificationDTO.getToken());
                return ResponseEntity.ok(new APIResponse<>(APIStatus.SUCCESS,
                        "Email verificata con successo! Ora puoi effettuare il login."));
            } else {
                log.warn("Verifica email fallita per token: {}", verificationDTO.getToken());
                return ResponseEntity.badRequest()
                        .body(new APIResponse<>(APIStatus.ERROR,
                                "Token di verifica non valido o scaduto"));
            }
        } catch (Exception e) {
            log.error("Errore durante la verifica email", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new APIResponse<>(APIStatus.ERROR, "Verifica fallita. Riprova più tardi."));
        }
    }

    // NUOVO ENDPOINT GET PER LA VERIFICA EMAIL (per link da email)
    @GetMapping("/verify-email")
    public ResponseEntity<?> verifyEmailGet(@RequestParam("token") String token) {
        try {
            log.info("Tentativo verifica email GET con token: {}", token);

            boolean verified = emailVerificationService.verifyEmail(token);

            if (verified) {
                log.info("Email verificata con successo via GET per token: {}", token);

                Map<String, Object> response = new HashMap<>();
                response.put("status", "SUCCESS");
                response.put("message", "Email verificata con successo! Ora puoi effettuare il login.");
                response.put("redirect", frontendUrl + "/?verified=success");

                return ResponseEntity.status(HttpStatus.FOUND)
                        .header("Location", frontendUrl + "/?verified=success")
                        .body(response);
            } else {
                log.warn("Verifica email GET fallita per token: {}", token);

                Map<String, Object> response = new HashMap<>();
                response.put("status", "ERROR");
                response.put("message", "Token di verifica non valido o scaduto");
                response.put("redirect", frontendUrl + "/?verified=error");

                return ResponseEntity.status(HttpStatus.FOUND)
                        .header("Location", frontendUrl + "/?verified=error")
                        .body(response);
            }
        } catch (Exception e) {
            log.error("Errore durante la verifica email GET", e);

            Map<String, Object> response = new HashMap<>();
            response.put("status", "ERROR");
            response.put("message", "Verifica fallita. Riprova più tardi.");
            response.put("redirect", frontendUrl + "/?verified=error");

            return ResponseEntity.status(HttpStatus.FOUND)
                    .header("Location", frontendUrl + "/?verified=error")
                    .body(response);
        }
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<?> resendVerificationEmail(
            @Valid @RequestBody ResendEmailDTO resendEmailDTO,
            HttpServletRequest request
    ) {
        try {
            // Rate limiting check
            String clientIp = rateLimitingService.getClientIp(request);
            if (!rateLimitingService.isEmailResendAllowed(clientIp)) {
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                        .body(new APIResponse<>(APIStatus.ERROR,
                                "Troppi tentativi di reinvio email. Riprova più tardi."));
            }

            emailVerificationService.resendVerificationEmail(resendEmailDTO.getEmail());

            log.info("Email di verifica reinviata a: {}", resendEmailDTO.getEmail());
            return ResponseEntity.ok(new APIResponse<>(APIStatus.SUCCESS,
                    "Email di verifica inviata! Controlla la tua casella di posta."));

        } catch (UserNotFoundException e) {
            // Non rivelare se l'email esiste o no per sicurezza
            return ResponseEntity.ok(new APIResponse<>(APIStatus.SUCCESS,
                    "Se l'email esiste, un link di verifica è stato inviato."));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest()
                    .body(new APIResponse<>(APIStatus.ERROR, e.getMessage()));
        } catch (Exception e) {
            log.error("Errore durante il reinvio email di verifica", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new APIResponse<>(APIStatus.ERROR, "Invio email fallito. Riprova più tardi."));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(
            @Valid @RequestBody LoginDTO credentials,
            HttpServletRequest request
    ) {
        try {
            // Rate limiting check
            String clientIp = rateLimitingService.getClientIp(request);
            if (!rateLimitingService.isLoginAllowed(clientIp)) {
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                        .body(new APIResponse<>(APIStatus.ERROR,
                                "Troppi tentativi di login. Riprova più tardi."));
            }

            User user = userService.authenticateUser(credentials.email(), credentials.password());
            String token = jwt.createToken(credentials.email(), user.getRuolo());

            log.info("Utente loggato con successo: {} da IP: {}", credentials.email(), clientIp);

            APIResponse<TokenDTO> response = new APIResponse<>(APIStatus.SUCCESS, new TokenDTO(token));
            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            log.warn("Login fallito per email: {} - {}", credentials.email(), e.getMessage());

            // Fornisce messaggi di errore specifici per una migliore UX
            if (e.getMessage().contains("Email non verificata")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new APIResponse<>(APIStatus.ERROR, e.getMessage()));
            } else if (e.getMessage().contains("Account temporaneamente bloccato")) {
                return ResponseEntity.status(HttpStatus.LOCKED)
                        .body(new APIResponse<>(APIStatus.ERROR, e.getMessage()));
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new APIResponse<>(APIStatus.ERROR, "Credenziali non valide"));
            }
        } catch (Exception e) {
            log.error("Errore inaspettato durante il login", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new APIResponse<>(APIStatus.ERROR, "Login fallito. Riprova più tardi."));
        }
    }

    // Metodo rimosso perché mancano i metodi necessari nel JWT e UserService

    @GetMapping("/account-status")
    public ResponseEntity<?> getAccountStatus(@RequestParam String email) {
        try {
            boolean emailVerified = emailVerificationService.isEmailVerified(email);
            Map<String, Object> status = new HashMap<>();
            status.put("emailVerified", emailVerified);
            return ResponseEntity.ok(new APIResponse<>(APIStatus.SUCCESS, status));
        } catch (Exception e) {
            log.error("Errore nel controllo status account per {}", email, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new APIResponse<>(APIStatus.ERROR, "Errore nel controllo status"));
        }
    }
}