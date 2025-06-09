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
import org.apache.coyote.BadRequestException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

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
                        .body(new APIResponse<>(APIStatus.ERROR, "Too many registration attempts. Please try again later."));
            }

            // Validation check
            if (validation.hasErrors()) {
                String errors = validation.getAllErrors().stream()
                        .map(error -> error.getDefaultMessage())
                        .collect(Collectors.joining(", "));
                return ResponseEntity.badRequest()
                        .body(new APIResponse<>(APIStatus.ERROR, "Validation errors: " + errors));
            }

            // reCAPTCHA verification
            if (recaptchaService.isRecaptchaEnabled() && !recaptchaService.verifyRecaptcha(recaptchaResponse)) {
                log.warn("Registration attempt with invalid reCAPTCHA from IP: {}", clientIp);
                return ResponseEntity.badRequest()
                        .body(new APIResponse<>(APIStatus.ERROR, "reCAPTCHA verification failed"));
            }

            UserDTO createdUser = userService.registerUser(registrationDTO);

            log.info("User registered successfully: {} from IP: {}", createdUser.getEmail(), clientIp);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new APIResponse<>(APIStatus.SUCCESS,
                            "Registration successful! Please check your email to verify your account."));

        } catch (UserAlreadyExistsException e) {
            log.warn("Registration attempt with existing email: {}", registrationDTO.getEmail());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new APIResponse<>(APIStatus.ERROR, e.getMessage()));
        } catch (Exception e) {
            log.error("Error during registration", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new APIResponse<>(APIStatus.ERROR, "Registration failed. Please try again."));
        }
    }

    @PostMapping("/verify-email")
    public ResponseEntity<?> verifyEmail(@Valid @RequestBody EmailVerificationDTO verificationDTO) {
        try {
            boolean verified = emailVerificationService.verifyEmail(verificationDTO.getToken());

            if (verified) {
                log.info("Email verified successfully for token: {}", verificationDTO.getToken());
                return ResponseEntity.ok(new APIResponse<>(APIStatus.SUCCESS,
                        "Email verified successfully! You can now log in."));
            } else {
                log.warn("Email verification failed for token: {}", verificationDTO.getToken());
                return ResponseEntity.badRequest()
                        .body(new APIResponse<>(APIStatus.ERROR,
                                "Invalid or expired verification token"));
            }
        } catch (Exception e) {
            log.error("Error during email verification", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new APIResponse<>(APIStatus.ERROR, "Verification failed. Please try again."));
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
                                "Too many email resend attempts. Please try again later."));
            }

            emailVerificationService.resendVerificationEmail(resendEmailDTO.getEmail());

            log.info("Verification email resent to: {}", resendEmailDTO.getEmail());
            return ResponseEntity.ok(new APIResponse<>(APIStatus.SUCCESS,
                    "Verification email sent! Please check your inbox."));

        } catch (UserNotFoundException e) {
            // Don't reveal if email exists or not for security
            return ResponseEntity.ok(new APIResponse<>(APIStatus.SUCCESS,
                    "If the email exists, a verification link has been sent."));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest()
                    .body(new APIResponse<>(APIStatus.ERROR, e.getMessage()));
        } catch (Exception e) {
            log.error("Error resending verification email", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new APIResponse<>(APIStatus.ERROR, "Failed to send email. Please try again."));
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
                                "Too many login attempts. Please try again later."));
            }

            User user = userService.authenticateUser(credentials.email(), credentials.password());
            String token = jwt.createToken(credentials.email(), user.getRuolo());

            log.info("User logged in successfully: {} from IP: {}", credentials.email(), clientIp);

            APIResponse<TokenDTO> response = new APIResponse<>(APIStatus.SUCCESS, new TokenDTO(token));
            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            log.warn("Login failed for email: {} - {}", credentials.email(), e.getMessage());

            // Provide specific error messages for better UX
            if (e.getMessage().contains("Email not verified")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new APIResponse<>(APIStatus.ERROR, e.getMessage()));
            } else if (e.getMessage().contains("Account is temporarily locked")) {
                return ResponseEntity.status(HttpStatus.LOCKED)
                        .body(new APIResponse<>(APIStatus.ERROR, e.getMessage()));
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new APIResponse<>(APIStatus.ERROR, "Invalid credentials"));
            }
        } catch (Exception e) {
            log.error("Unexpected error during login", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new APIResponse<>(APIStatus.ERROR, "Login failed. Please try again."));
        }
    }

    @GetMapping("/account-status")
    public ResponseEntity<?> getAccountStatus(@RequestParam String email) {
        try {
            String status = userService.getAccountStatus(email);
            return ResponseEntity.ok(new APIResponse<>(APIStatus.SUCCESS, status));
        } catch (Exception e) {
            return ResponseEntity.ok(new APIResponse<>(APIStatus.SUCCESS, "USER_NOT_FOUND"));
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("#id == principal.id or hasRole('ADMIN')")
    public ResponseEntity<?> getUserById(@PathVariable Long id) {
        try {
            UserDTO user = userService.getUserById(id);
            return ResponseEntity.ok(new APIResponse<>(APIStatus.SUCCESS, user));
        } catch (UserNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{id}/change-password")
    @PreAuthorize("#id == principal.id or hasRole('ADMIN')")
    public ResponseEntity<?> changePassword(
            @PathVariable Long id,
            @Valid @RequestBody PasswordUpdateDTO passwordDTO
    ) {
        try {
            userService.changePassword(id, passwordDTO.getNewPassword());
            return ResponseEntity.ok(new APIResponse<>(APIStatus.SUCCESS, "Password changed successfully"));
        } catch (UserNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }
}