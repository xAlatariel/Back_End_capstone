package com.example.Capstone.service;

import com.example.Capstone.entity.EmailVerificationToken;
import com.example.Capstone.entity.User;
import com.example.Capstone.exception.UserNotFoundException;
import com.example.Capstone.repository.EmailVerificationTokenRepository;
import com.example.Capstone.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class EmailVerificationService {

    private final EmailVerificationTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final SecureRandom secureRandom = new SecureRandom();

    public void createAndSendVerificationToken(User user) {
        // Delete existing tokens for this user
        tokenRepository.deleteByUser(user);

        // Create new token
        String token = generateSecureToken();

        EmailVerificationToken verificationToken = EmailVerificationToken.builder()
                .token(token)
                .user(user)
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusHours(24))
                .build();

        tokenRepository.save(verificationToken);

        // Send verification email
        emailService.sendEmailVerification(
                user.getEmail(),
                user.getFullName(),
                token
        );

        log.info("Token di verifica creato e inviato per l'utente: {}", user.getEmail());
    }

    public boolean verifyEmail(String token) {
        Optional<EmailVerificationToken> optionalToken = tokenRepository.findByToken(token);

        if (optionalToken.isEmpty()) {
            log.warn("Tentativo di verifica con token inesistente: {}", token);
            return false;
        }

        EmailVerificationToken verificationToken = optionalToken.get();

        if (!verificationToken.isValid()) {
            log.warn("Tentativo di verifica con token non valido: {}", token);
            return false;
        }

        // Mark token as used
        verificationToken.markAsUsed();
        tokenRepository.save(verificationToken);

        // Verify user email
        User user = verificationToken.getUser();
        user.verifyEmail();
        userRepository.save(user);

        // Send welcome email
        emailService.sendWelcomeEmail(user.getEmail(), user.getFullName());

        log.info("Email verificata con successo per l'utente: {}", user.getEmail());
        return true;
    }

    public void resendVerificationEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("Utente non trovato con email: " + email));

        if (user.getEmailVerified()) {
            throw new IllegalStateException("Email già verificata");
        }

        // Check rate limiting (max 3 emails per hour)
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
        long recentTokens = tokenRepository.countTokensCreatedSince(oneHourAgo);

        if (recentTokens >= 3) {
            throw new IllegalStateException("Troppi tentativi di reinvio. Riprova più tardi.");
        }

        createAndSendVerificationToken(user);
        log.info("Email di verifica reinviata a: {}", email);
    }

    private String generateSecureToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    @Scheduled(fixedRate = 3600000) // Run every hour
    public void cleanupExpiredTokens() {
        try {
            LocalDateTime now = LocalDateTime.now();
            tokenRepository.deleteExpiredTokens(now);
            log.debug("Cleanup dei token scaduti completato");
        } catch (Exception e) {
            log.error("Errore durante il cleanup dei token scaduti", e);
        }
    }

    public boolean isEmailVerified(String email) {
        return userRepository.findByEmail(email)
                .map(User::getEmailVerified)
                .orElse(false);
    }

    public Optional<EmailVerificationToken> findByToken(String token) {
        return tokenRepository.findByToken(token);
    }
}