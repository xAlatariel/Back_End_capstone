package com.example.Capstone.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_activities",
        indexes = {
                @Index(name = "idx_user_activities_user_id", columnList = "user_id"),
                @Index(name = "idx_user_activities_created_at", columnList = "created_at"),
                @Index(name = "idx_user_activities_type", columnList = "activity_type")
        })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserActivity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "performed_by_user_id")
    private User performedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "activity_type", nullable = false, length = 50)
    private ActivityType activityType;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public enum ActivityType {
        // Attività utente
        LOGIN,
        LOGOUT,
        PASSWORD_CHANGED,
        EMAIL_VERIFIED,
        PROFILE_UPDATED,
        RESERVATION_CREATED,
        RESERVATION_CANCELLED,

        // Attività admin
        USER_CREATED,
        USER_UPDATED,
        USER_DELETED,
        USER_STATUS_CHANGED,
        ROLE_CHANGED,
        PASSWORD_RESET,
        VERIFICATION_EMAIL_SENT,
        EMAIL_VERIFIED_BY_ADMIN,

        // Attività di sistema
        SYSTEM_LOGIN_ATTEMPT_FAILED,
        SYSTEM_ACCOUNT_LOCKED,
        SYSTEM_ACCOUNT_UNLOCKED
    }
}