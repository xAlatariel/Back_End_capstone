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

    // ===================================================================
    // REGISTRAZIONE UTENTE
    // ===================================================================
    @PostMapping("/register")
    public ResponseEntity<?> registerUser(
            @Valid @RequestBody UserRegistrationDTO registrationDTO,
            @RequestHeader(value = "g-recaptcha-response", required = false) String recaptchaResponse,
            HttpServletRequest request,
            BindingResult validation
    ) {
        try {
            log.info("Tentativo di registrazione per email: {}", registrationDTO.getEmail());

            // Rate limiting check
            String clientIp = rateLimitingService.getClientIp(request);
            if (!rateLimitingService.isRegistrationAllowed(clientIp)) {
                log.warn("Rate limit superato per registrazione da IP: {}", clientIp);
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                        .body(new APIResponse<>(APIStatus.ERROR, "Troppi tentativi di registrazione. Riprova più tardi."));
            }

            // Validation check
            if (validation.hasErrors()) {
                String errors = validation.getAllErrors().stream()
                        .map(error -> error.getDefaultMessage())
                        .collect(Collectors.joining(", "));
                log.warn("Errori di validazione per registrazione: {}", errors);
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
            log.error("Errore durante la registrazione per {}: {}", registrationDTO.getEmail(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new APIResponse<>(APIStatus.ERROR, "Registrazione fallita. Riprova più tardi."));
        }
    }

    // ===================================================================
    // LOGIN UTENTE
    // ===================================================================
    @PostMapping("/login")
    public ResponseEntity<?> login(
            @Valid @RequestBody LoginDTO loginDTO,
            HttpServletRequest request
    ) {
        try {
            log.info("Tentativo di login per email: {}", loginDTO.email());

            // Rate limiting check
            String clientIp = rateLimitingService.getClientIp(request);
            if (!rateLimitingService.isLoginAllowed(clientIp)) {
                log.warn("Rate limit superato per login da IP: {}", clientIp);
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                        .body(new APIResponse<>(APIStatus.ERROR, "Troppi tentativi di login. Riprova più tardi."));
            }

            User authenticatedUser = userService.authenticateUser(loginDTO.email(), loginDTO.password());

            // Reset failed login attempts on successful authentication
            authenticatedUser.resetFailedLoginAttempts();
            authenticatedUser.updateLastLogin();
            userService.saveUser(authenticatedUser);

            // Generate JWT token
            String token = jwt.createToken(authenticatedUser.getEmail(), authenticatedUser.getRuolo());

            // Create response
            UserDTO userDTO = userService.convertToDTO(authenticatedUser);
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("token", token);
            responseData.put("user", userDTO);

            log.info("Login effettuato con successo per: {} da IP: {}", loginDTO.email(), clientIp);

            return ResponseEntity.ok(new APIResponse<>(APIStatus.SUCCESS, "Login effettuato con successo", responseData));

        } catch (RuntimeException e) {
            log.warn("Tentativo di login fallito per {}: {}", loginDTO.email(), e.getMessage());

            // Increment failed login attempts if user exists
            try {
                User user = userService.findByEmail(loginDTO.email());
                user.incrementFailedLoginAttempts();
                userService.saveUser(user);
            } catch (UserNotFoundException ignored) {
                // User doesn't exist, ignore
            }

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new APIResponse<>(APIStatus.ERROR, e.getMessage()));
        } catch (Exception e) {
            log.error("Errore durante il login per {}: {}", loginDTO.email(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new APIResponse<>(APIStatus.ERROR, "Login fallito. Riprova più tardi."));
        }
    }

    // ===================================================================
    // VERIFICA EMAIL - ENDPOINT POST (per frontend)
    // ===================================================================
    @PostMapping("/verify-email")
    public ResponseEntity<?> verifyEmail(@Valid @RequestBody EmailVerificationDTO verificationDTO) {
        try {
            log.info("Tentativo di verifica email con token: {}", verificationDTO.getToken());

            boolean verified = emailVerificationService.verifyEmail(verificationDTO.getToken());

            if (verified) {
                log.info("Email verificata con successo per token: {}", verificationDTO.getToken());
                return ResponseEntity.ok(new APIResponse<>(APIStatus.SUCCESS,
                        "Email verificata con successo! Ora puoi effettuare il login."));
            } else {
                log.warn("Tentativo di verifica email con token non valido: {}", verificationDTO.getToken());
                return ResponseEntity.badRequest()
                        .body(new APIResponse<>(APIStatus.ERROR,
                                "Token non valido o scaduto. Richiedi un nuovo link di verifica."));
            }

        } catch (Exception e) {
            log.error("Errore durante la verifica email per token: {}", verificationDTO.getToken(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new APIResponse<>(APIStatus.ERROR, "Errore durante la verifica. Riprova più tardi."));
        }
    }

    // ===================================================================
    // VERIFICA EMAIL - ENDPOINT GET (per link email)
    // ===================================================================
    @GetMapping("/verify-email")
    public ResponseEntity<?> verifyEmailFromLink(@RequestParam("token") String token) {
        try {
            log.info("Tentativo di verifica email da link con token: {}", token);

            boolean verified = emailVerificationService.verifyEmail(token);

            if (verified) {
                log.info("Email verificata con successo per token: {}", token);
                // Redirect al frontend con successo
                return ResponseEntity.status(HttpStatus.FOUND)
                        .header("Location", frontendUrl + "/login?verified=true")
                        .build();
            } else {
                log.warn("Tentativo di verifica email con token non valido: {}", token);
                // Redirect al frontend con errore
                return ResponseEntity.status(HttpStatus.FOUND)
                        .header("Location", frontendUrl + "/login?verified=false")
                        .build();
            }

        } catch (Exception e) {
            log.error("Errore durante la verifica email da link per token: {}", token, e);
            return ResponseEntity.status(HttpStatus.FOUND)
                    .header("Location", frontendUrl + "/login?verified=error")
                    .build();
        }
    }

    // ===================================================================
    // STATO ACCOUNT
    // ===================================================================
    @GetMapping("/account-status")
    public ResponseEntity<?> getAccountStatus(@RequestParam("email") String email) {
        try {
            log.debug("Richiesta stato account per email: {}", email);

            User user = userService.findByEmail(email);

            Map<String, Object> status = new HashMap<>();
            status.put("email", user.getEmail());
            status.put("emailVerified", user.getEmailVerified());
            status.put("enabled", user.getEnabled());
            status.put("accountStatus", user.getAccountStatus());
            status.put("accountLocked", user.isAccountLocked());

            return ResponseEntity.ok(new APIResponse<>(APIStatus.SUCCESS, "Status recuperato", status));

        } catch (UserNotFoundException e) {
            log.warn("Richiesta stato per email non esistente: {}", email);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new APIResponse<>(APIStatus.ERROR, "Utente non trovato"));
        } catch (Exception e) {
            log.error("Errore durante il recupero dello status per {}: {}", email, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new APIResponse<>(APIStatus.ERROR, "Errore nel recupero dello status"));
        }
    }

    // ===================================================================
    // PROFILO UTENTE
    // ===================================================================
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or authentication.name == @userService.findById(#id).email")
    public ResponseEntity<?> getUserById(@PathVariable Long id) {
        try {
            log.debug("Richiesta dettagli utente per ID: {}", id);

            User user = userService.findById(id);
            UserDTO userDTO = userService.convertToDTO(user);

            return ResponseEntity.ok(new APIResponse<>(APIStatus.SUCCESS, "Utente trovato", userDTO));

        } catch (UserNotFoundException e) {
            log.warn("Richiesta utente per ID non esistente: {}", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new APIResponse<>(APIStatus.ERROR, e.getMessage()));
        } catch (Exception e) {
            log.error("Errore durante il recupero dell'utente con ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new APIResponse<>(APIStatus.ERROR, "Errore nel recupero dell'utente"));
        }
    }

    // ===================================================================
    // CAMBIO PASSWORD
    // ===================================================================
    @PostMapping("/{id}/change-password")
    @PreAuthorize("hasRole('ADMIN') or authentication.name == @userService.findById(#id).email")
    public ResponseEntity<?> changePassword(
            @PathVariable Long id,
            @Valid @RequestBody ChangePasswordDTO changePasswordDTO
    ) {
        try {
            log.info("Tentativo di cambio password per utente ID: {}", id);

            userService.changePassword(id, changePasswordDTO.getNewPassword());

            log.info("Password cambiata con successo per utente ID: {}", id);

            return ResponseEntity.ok(new APIResponse<>(APIStatus.SUCCESS, "Password cambiata con successo"));

        } catch (UserNotFoundException e) {
            log.warn("Tentativo di cambio password per utente ID non esistente: {}", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new APIResponse<>(APIStatus.ERROR, e.getMessage()));
        } catch (Exception e) {
            log.error("Errore durante il cambio password per utente ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new APIResponse<>(APIStatus.ERROR, "Errore nel cambio password"));
        }
    }

    // ===================================================================
    // REINVIO EMAIL DI VERIFICA
    // ===================================================================
    @PostMapping("/resend-verification")
    public ResponseEntity<?> resendVerificationEmail(
            @Valid @RequestBody ResendEmailDTO resendEmailDTO,
            HttpServletRequest request
    ) {
        try {
            log.info("Tentativo di reinvio email di verifica per: {}", resendEmailDTO.getEmail());

            // Rate limiting check
            String clientIp = rateLimitingService.getClientIp(request);
            if (!rateLimitingService.isEmailResendAllowed(clientIp)) {
                log.warn("Rate limit superato per reinvio email da IP: {}", clientIp);
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                        .body(new APIResponse<>(APIStatus.ERROR, "Troppi tentativi di reinvio email. Riprova più tardi."));
            }

            emailVerificationService.resendVerificationEmail(resendEmailDTO.getEmail());

            log.info("Email di verifica reinviata per: {}", resendEmailDTO.getEmail());

            return ResponseEntity.ok(new APIResponse<>(APIStatus.SUCCESS,
                    "Email di verifica reinviata. Controlla la tua casella di posta."));

        } catch (UserNotFoundException e) {
            log.warn("Tentativo di reinvio per email non registrata: {}", resendEmailDTO.getEmail());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new APIResponse<>(APIStatus.ERROR, e.getMessage()));
        } catch (IllegalStateException e) {
            log.warn("Tentativo di reinvio per email già verificata: {}", resendEmailDTO.getEmail());
            return ResponseEntity.badRequest()
                    .body(new APIResponse<>(APIStatus.ERROR, e.getMessage()));
        } catch (Exception e) {
            log.error("Errore durante il reinvio email per {}: {}", resendEmailDTO.getEmail(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new APIResponse<>(APIStatus.ERROR, "Reinvio fallito. Riprova più tardi."));
        }
    }
}