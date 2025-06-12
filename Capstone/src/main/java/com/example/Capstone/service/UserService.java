package com.example.Capstone.service;

import com.example.Capstone.dto.UserDTO;
import com.example.Capstone.dto.UserRegistrationDTO;
import com.example.Capstone.entity.AccountStatus;
import com.example.Capstone.entity.Role;
import com.example.Capstone.entity.User;
import com.example.Capstone.exception.UserAlreadyExistsException;
import com.example.Capstone.exception.UserNotFoundException;
import com.example.Capstone.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailVerificationService emailVerificationService;

    // ===================================================================
    // REGISTRAZIONE UTENTE CON VERIFICA EMAIL
    // ===================================================================
    public UserDTO registerUser(UserRegistrationDTO registrationDTO) throws UserAlreadyExistsException {
        // Controllo se email già esistente
        if (userRepository.existsByEmail(registrationDTO.getEmail())) {
            throw new UserAlreadyExistsException("Email già registrata: " + registrationDTO.getEmail());
        }

        // Creazione nuovo utente
        User newUser = User.builder()
                .nome(registrationDTO.getNome())
                .cognome(registrationDTO.getCognome())
                .email(registrationDTO.getEmail())
                .password(passwordEncoder.encode(registrationDTO.getPassword()))
                .ruolo(Role.USER)
                .accountStatus(AccountStatus.PENDING_VERIFICATION)
                .enabled(false)
                .emailVerified(false)
                .failedLoginAttempts(0)
                .createdAt(LocalDateTime.now())
                .build();

        newUser = userRepository.save(newUser);

        // Invio email di verifica
        try {
            emailVerificationService.createAndSendVerificationToken(newUser);
            log.info("Utente registrato con successo e email di verifica inviata: {}", newUser.getEmail());
        } catch (Exception e) {
            log.error("Errore nell'invio dell'email di verifica per: {}", newUser.getEmail(), e);
            // Non bloccare la registrazione se l'email fallisce
        }

        return convertToDTO(newUser);
    }

    // ===================================================================
    // AUTENTICAZIONE UTENTE
    // ===================================================================
    public User authenticateUser(String email, String password) {
        User user = findByEmail(email);

        // Controllo se account è bloccato per troppi tentativi
        if (user.isAccountLocked()) {
            throw new RuntimeException("Account temporaneamente bloccato per troppi tentativi di login falliti. Riprova più tardi.");
        }

        // Controllo se email è verificata
        if (!user.getEmailVerified()) {
            throw new RuntimeException("Email non verificata. Controlla la tua casella di posta per il link di verifica.");
        }

        // Controllo se account è abilitato
        if (!user.getEnabled()) {
            throw new RuntimeException("Account disabilitato. Contatta il supporto.");
        }

        // Verifica password
        if (!passwordEncoder.matches(password, user.getPassword())) {
            user.incrementFailedLoginAttempts();
            userRepository.save(user);
            throw new RuntimeException("Credenziali non valide");
        }

        // Reset tentativi falliti su login riuscito
        user.resetFailedLoginAttempts();
        user.updateLastLogin();
        userRepository.save(user);

        return user;
    }

    // ===================================================================
    // METODI DI RICERCA UTENTI
    // ===================================================================
    public User findByEmail(String email) throws UserNotFoundException {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("Utente non trovato con email: " + email));
    }

    public User findById(Long id) throws UserNotFoundException {
        return userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("Utente non trovato con ID: " + id));
    }

    // Metodo per ottenere UserDTO by ID
    public UserDTO getUserById(Long id) throws UserNotFoundException {
        User user = findById(id);
        return convertToDTO(user);
    }

    // ===================================================================
    // GESTIONE UTENTI
    // ===================================================================
    public User saveUser(User user) {
        return userRepository.save(user);
    }

    // Cambio password
    public void changePassword(Long userId, String newPassword) {
        User user = findById(userId);
        user.setPassword(passwordEncoder.encode(newPassword));
        saveUser(user);
        log.info("Password cambiata con successo per utente: {}", user.getEmail());
    }

    // ===================================================================
    // VERIFICA STATO ACCOUNT
    // ===================================================================
    // Controllo se l'utente può fare login
    public boolean canUserLogin(String email) {
        try {
            User user = findByEmail(email);
            return user.isAccountVerified() && !user.isAccountLocked();
        } catch (UserNotFoundException e) {
            return false;
        }
    }

    // Ottieni stato account per troubleshooting
    public String getAccountStatus(String email) {
        try {
            User user = findByEmail(email);
            if (!user.getEmailVerified()) {
                return "EMAIL_NOT_VERIFIED";
            }
            if (user.isAccountLocked()) {
                return "ACCOUNT_LOCKED";
            }
            if (!user.getEnabled()) {
                return "ACCOUNT_DISABLED";
            }
            return "ACTIVE";
        } catch (UserNotFoundException e) {
            return "USER_NOT_FOUND";
        }
    }

    // ===================================================================
    // CONVERSIONE DTO
    // ===================================================================
    public UserDTO convertToDTO(User user) {
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setNome(user.getNome());
        dto.setCognome(user.getCognome());
        dto.setEmail(user.getEmail());
        dto.setRuolo(user.getRuolo());
        dto.setAccountStatus(user.getAccountStatus());
        dto.setEmailVerified(user.getEmailVerified());
        dto.setEnabled(user.getEnabled());
        dto.setCreatedAt(user.getCreatedAt());
        dto.setLastLogin(user.getLastLogin());
        dto.setEmailVerifiedAt(user.getEmailVerifiedAt());
        dto.setFailedLoginAttempts(user.getFailedLoginAttempts());
        dto.setAccountLocked(user.isAccountLocked());
        dto.setAccountVerified(user.isAccountVerified());
        return dto;
    }

    // ===================================================================
    // GESTIONE EMAIL VERIFICATION
    // ===================================================================
    public void enableUserAfterEmailVerification(String email) {
        try {
            User user = findByEmail(email);
            user.verifyEmail(); // Questo metodo è già nell'entità User
            saveUser(user);
            log.info("Utente abilitato dopo verifica email: {}", email);
        } catch (UserNotFoundException e) {
            log.error("Tentativo di abilitare utente non esistente: {}", email);
            throw e;
        }
    }

    public void enableUserAfterEmailVerification(User user) {
        user.verifyEmail();
        saveUser(user);
        log.info("Utente abilitato dopo verifica email: {}", user.getEmail());
    }

    // ===================================================================
    // METODI DI UTILITÀ
    // ===================================================================
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    public long countUsers() {
        return userRepository.count();
    }

    public long countActiveUsers() {
        return userRepository.countActiveUsers();
    }

    public long countPendingVerificationUsers() {
        return userRepository.countPendingVerificationUsers();
    }

    public long countEnabledUsers() {
        return userRepository.countByEnabledTrue();
    }

    public long countEmailVerifiedUsers() {
        return userRepository.countByEmailVerifiedTrue();
    }

    public long countEmailUnverifiedUsers() {
        return userRepository.countByEmailVerifiedFalse();
    }
}