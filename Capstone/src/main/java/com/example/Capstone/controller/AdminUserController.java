package com.example.Capstone.controllers;

import com.example.Capstone.entities.User;
import com.example.Capstone.entities.UserActivity;
import com.example.Capstone.dto.*;
import com.example.Capstone.services.UserService;
import com.example.Capstone.services.UserActivityService;
import com.example.Capstone.services.EmailService;
import com.example.Capstone.exceptions.BadRequestException;
import com.example.Capstone.exceptions.NotFoundException;
import com.example.Capstone.security.JWTTools;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.Email;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/admin/users")
@PreAuthorize("hasRole('ADMIN')")
@Validated
@CrossOrigin
public class AdminUserController {

    @Autowired
    private UserService userService;

    @Autowired
    private UserActivityService userActivityService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private JWTTools jwtTools;

    // ===================================================================
    // RECUPERO UTENTI
    // ===================================================================

    @GetMapping
    public ResponseEntity<Page<UserResponseDTO>> getAllUsers(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Boolean emailVerified) {

        try {
            Sort.Direction direction = sortDir.equalsIgnoreCase("desc") ?
                    Sort.Direction.DESC : Sort.Direction.ASC;
            Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

            Page<User> usersPage = userService.getAllUsersFiltered(
                    pageable, search, role, status, emailVerified
            );

            Page<UserResponseDTO> responsePage = usersPage.map(this::convertToUserResponseDTO);

            return ResponseEntity.ok(responsePage);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserDetailResponseDTO> getUserById(@PathVariable Long id) {
        try {
            User user = userService.findById(id);
            UserDetailResponseDTO response = convertToUserDetailResponseDTO(user);
            return ResponseEntity.ok(response);
        } catch (NotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/stats")
    public ResponseEntity<UserStatsDTO> getUserStats() {
        try {
            UserStatsDTO stats = userService.getUserStats();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ===================================================================
    // CREAZIONE E MODIFICA UTENTI
    // ===================================================================

    @PostMapping
    public ResponseEntity<UserResponseDTO> createUser(
            @Valid @RequestBody CreateUserRequestDTO createUserRequest,
            @AuthenticationPrincipal User adminUser) {
        try {
            User newUser = userService.createUserByAdmin(createUserRequest, adminUser);
            UserResponseDTO response = convertToUserResponseDTO(newUser);

            // Log dell'attività admin
            userActivityService.logActivity(adminUser,
                    UserActivity.ActivityType.USER_CREATED,
                    "Creato utente: " + newUser.getEmail());

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (BadRequestException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserResponseDTO> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequestDTO updateUserRequest,
            @AuthenticationPrincipal User adminUser) {
        try {
            User updatedUser = userService.updateUserByAdmin(id, updateUserRequest, adminUser);
            UserResponseDTO response = convertToUserResponseDTO(updatedUser);

            // Log dell'attività admin
            userActivityService.logActivity(adminUser,
                    UserActivity.ActivityType.USER_UPDATED,
                    "Aggiornato utente: " + updatedUser.getEmail());

            return ResponseEntity.ok(response);
        } catch (NotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (BadRequestException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ===================================================================
    // GESTIONE STATO UTENTI
    // ===================================================================

    @PatchMapping("/{id}/status")
    public ResponseEntity<UserResponseDTO> updateUserStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserStatusRequestDTO statusRequest,
            @AuthenticationPrincipal User adminUser) {
        try {
            User updatedUser = userService.updateUserStatus(id, statusRequest.isEnabled(), adminUser);
            UserResponseDTO response = convertToUserResponseDTO(updatedUser);

            // Log dell'attività
            String action = statusRequest.isEnabled() ? "abilitato" : "disabilitato";
            userActivityService.logActivity(adminUser,
                    UserActivity.ActivityType.USER_STATUS_CHANGED,
                    "Utente " + action + ": " + updatedUser.getEmail());

            return ResponseEntity.ok(response);
        } catch (NotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PatchMapping("/{id}/role")
    public ResponseEntity<UserResponseDTO> updateUserRole(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRoleRequestDTO roleRequest,
            @AuthenticationPrincipal User adminUser) {
        try {
            // Verifica che l'admin non stia modificando il proprio ruolo
            if (id.equals(adminUser.getId())) {
                return ResponseEntity.badRequest().build();
            }

            User updatedUser = userService.updateUserRole(id, roleRequest.getRole(), adminUser);
            UserResponseDTO response = convertToUserResponseDTO(updatedUser);

            // Log dell'attività
            userActivityService.logActivity(adminUser,
                    UserActivity.ActivityType.ROLE_CHANGED,
                    "Ruolo cambiato per utente: " + updatedUser.getEmail() +
                            " -> " + roleRequest.getRole());

            return ResponseEntity.ok(response);
        } catch (NotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (BadRequestException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PatchMapping("/{id}/password-reset")
    public ResponseEntity<Map<String, String>> resetUserPassword(
            @PathVariable Long id,
            @AuthenticationPrincipal User adminUser) {
        try {
            String temporaryPassword = userService.resetUserPasswordByAdmin(id, adminUser);

            // Log dell'attività
            User targetUser = userService.findById(id);
            userActivityService.logActivity(adminUser,
                    UserActivity.ActivityType.PASSWORD_RESET,
                    "Password resettata per utente: " + targetUser.getEmail());

            Map<String, String> response = new HashMap<>();
            response.put("message", "Password resettata con successo");
            response.put("temporaryPassword", temporaryPassword);

            return ResponseEntity.ok(response);
        } catch (NotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ===================================================================
    // GESTIONE EMAIL E VERIFICHE
    // ===================================================================

    @PostMapping("/{id}/resend-verification")
    public ResponseEntity<Map<String, String>> resendVerificationEmail(
            @PathVariable Long id,
            @AuthenticationPrincipal User adminUser) {
        try {
            User user = userService.findById(id);

            if (user.isEmailVerified()) {
                Map<String, String> response = new HashMap<>();
                response.put("message", "L'email è già verificata");
                return ResponseEntity.badRequest().body(response);
            }

            emailService.sendVerificationEmail(user);

            // Log dell'attività
            userActivityService.logActivity(adminUser,
                    UserActivity.ActivityType.VERIFICATION_EMAIL_SENT,
                    "Email di verifica inviata a: " + user.getEmail());

            Map<String, String> response = new HashMap<>();
            response.put("message", "Email di verifica inviata con successo");
            return ResponseEntity.ok(response);
        } catch (NotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PatchMapping("/{id}/verify-email")
    public ResponseEntity<UserResponseDTO> verifyUserEmail(
            @PathVariable Long id,
            @AuthenticationPrincipal User adminUser) {
        try {
            User user = userService.verifyUserEmailByAdmin(id, adminUser);
            UserResponseDTO response = convertToUserResponseDTO(user);

            // Log dell'attività
            userActivityService.logActivity(adminUser,
                    UserActivity.ActivityType.EMAIL_VERIFIED,
                    "Email verificata manualmente per: " + user.getEmail());

            return ResponseEntity.ok(response);
        } catch (NotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ===================================================================
    // ELIMINAZIONE UTENTI
    // ===================================================================

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteUser(
            @PathVariable Long id,
            @AuthenticationPrincipal User adminUser) {
        try {
            // Verifica che l'admin non stia eliminando se stesso
            if (id.equals(adminUser.getId())) {
                Map<String, String> response = new HashMap<>();
                response.put("message", "Non puoi eliminare il tuo account");
                return ResponseEntity.badRequest().body(response);
            }

            User userToDelete = userService.findById(id);
            String deletedEmail = userToDelete.getEmail();

            userService.deleteUserByAdmin(id, adminUser);

            // Log dell'attività
            userActivityService.logActivity(adminUser,
                    UserActivity.ActivityType.USER_DELETED,
                    "Utente eliminato: " + deletedEmail);

            Map<String, String> response = new HashMap<>();
            response.put("message", "Utente eliminato con successo");
            return ResponseEntity.ok(response);
        } catch (NotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (BadRequestException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ===================================================================
    // ATTIVITÀ E LOG
    // ===================================================================

    @GetMapping("/{id}/activities")
    public ResponseEntity<Page<UserActivityResponseDTO>> getUserActivities(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) int size) {
        try {
            Pageable pageable = PageRequest.of(page, size,
                    Sort.by(Sort.Direction.DESC, "createdAt"));

            Page<UserActivity> activitiesPage = userActivityService.getUserActivities(id, pageable);
            Page<UserActivityResponseDTO> responsePage = activitiesPage.map(this::convertToActivityResponseDTO);

            return ResponseEntity.ok(responsePage);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/activities/admin")
    public ResponseEntity<Page<UserActivityResponseDTO>> getAdminActivities(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) int size,
            @AuthenticationPrincipal User adminUser) {
        try {
            Pageable pageable = PageRequest.of(page, size,
                    Sort.by(Sort.Direction.DESC, "createdAt"));

            Page<UserActivity> activitiesPage = userActivityService.getAdminActivities(pageable);
            Page<UserActivityResponseDTO> responsePage = activitiesPage.map(this::convertToActivityResponseDTO);

            return ResponseEntity.ok(responsePage);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ===================================================================
    // EXPORT E REPORTING
    // ===================================================================

    @GetMapping("/export")
    public ResponseEntity<List<UserExportDTO>> exportUsers(
            @RequestParam(required = false) String format,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String status) {
        try {
            List<UserExportDTO> users = userService.exportUsers(search, role, status);
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ===================================================================
    // METODI DI CONVERSIONE DTO
    // ===================================================================

    private UserResponseDTO convertToUserResponseDTO(User user) {
        return UserResponseDTO.builder()
                .id(user.getId())
                .name(user.getName())
                .surname(user.getSurname())
                .email(user.getEmail())
                .role(user.getRole())
                .enabled(user.isEnabled())
                .emailVerified(user.isEmailVerified())
                .createdAt(user.getCreatedAt())
                .lastLogin(user.getLastLogin())
                .reservationsCount(user.getReservations() != null ? user.getReservations().size() : 0)
                .build();
    }

    private UserDetailResponseDTO convertToUserDetailResponseDTO(User user) {
        return UserDetailResponseDTO.builder()
                .id(user.getId())
                .name(user.getName())
                .surname(user.getSurname())
                .email(user.getEmail())
                .role(user.getRole())
                .enabled(user.isEnabled())
                .emailVerified(user.isEmailVerified())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .lastLogin(user.getLastLogin())
                .reservationsCount(user.getReservations() != null ? user.getReservations().size() : 0)
                .profileImage(user.getProfileImage())
                .build();
    }

    private UserActivityResponseDTO convertToActivityResponseDTO(UserActivity activity) {
        return UserActivityResponseDTO.builder()
                .id(activity.getId())
                .activityType(activity.getActivityType())
                .description(activity.getDescription())
                .createdAt(activity.getCreatedAt())
                .userId(activity.getUser().getId())
                .userName(activity.getUser().getName() + " " + activity.getUser().getSurname())
                .performedByUserId(activity.getPerformedBy() != null ? activity.getPerformedBy().getId() : null)
                .performedByUserName(activity.getPerformedBy() != null ?
                        activity.getPerformedBy().getName() + " " + activity.getPerformedBy().getSurname() : null)
                .build();
    }
}