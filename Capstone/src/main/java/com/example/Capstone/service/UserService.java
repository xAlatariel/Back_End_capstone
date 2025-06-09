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
import java.util.Optional;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailVerificationService emailVerificationService;

    // User Registration with Email Verification
    public UserDTO registerUser(UserRegistrationDTO registrationDTO) throws UserAlreadyExistsException {
        if (userRepository.existsByEmail(registrationDTO.getEmail())) {
            throw new UserAlreadyExistsException("Email already registered: " + registrationDTO.getEmail());
        }

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

        // Send verification email
        try {
            emailVerificationService.createAndSendVerificationToken(newUser);
            log.info("User registered successfully and verification email sent: {}", newUser.getEmail());
        } catch (Exception e) {
            log.error("Failed to send verification email for user: {}", newUser.getEmail(), e);
            // Don't fail registration if email sending fails
        }

        return convertToDTO(newUser);
    }

    public User findById(Long id) {
        Optional<User> userOptional = userRepository.findById(id);
        return userOptional.orElseThrow(() -> new UserNotFoundException("User not found with id: " + id));
    }

    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found with email: " + email));
    }

    // Enhanced authentication check
    public User authenticateUser(String email, String password) {
        User user = findByEmail(email);

        // Check if account is locked
        if (user.isAccountLocked()) {
            throw new RuntimeException("Account is temporarily locked due to multiple failed login attempts");
        }

        // Check if email is verified
        if (!user.getEmailVerified()) {
            throw new RuntimeException("Email not verified. Please check your email for verification link.");
        }

        // Check if account is enabled
        if (!user.getEnabled()) {
            throw new RuntimeException("Account is disabled. Please contact support.");
        }

        if (!passwordEncoder.matches(password, user.getPassword())) {
            user.incrementFailedLoginAttempts();
            userRepository.save(user);
            throw new RuntimeException("Invalid credentials");
        }

        // Reset failed attempts on successful login
        user.resetFailedLoginAttempts();
        user.updateLastLogin();
        userRepository.save(user);

        return user;
    }

    // Get User by ID
    public UserDTO getUserById(Long id) throws UserNotFoundException {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + id));
        return convertToDTO(user);
    }

    // Change Password
    public void changePassword(Long userId, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        log.info("Password changed successfully for user: {}", user.getEmail());
    }

    // Check if user can login (email verified and account active)
    public boolean canUserLogin(String email) {
        try {
            User user = findByEmail(email);
            return user.isAccountVerified() && !user.isAccountLocked();
        } catch (UserNotFoundException e) {
            return false;
        }
    }

    // Get account status for troubleshooting
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

    private UserDTO convertToDTO(User user) {
        UserDTO dto = new UserDTO();
        dto.setNome(user.getNome());
        dto.setCognome(user.getCognome());
        dto.setEmail(user.getEmail());
        return dto;
    }
}