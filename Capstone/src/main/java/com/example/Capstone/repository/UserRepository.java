package com.example.Capstone.repository;

import com.example.Capstone.entity.AccountStatus;
import com.example.Capstone.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // Metodi di ricerca base
    Optional<User> findByEmail(String email);

    Optional<User> findById(Long id);

    boolean existsByEmail(String email);

    // Metodi di conteggio
    long countByEnabledTrue();

    long countByEmailVerifiedFalse();

    long countByEmailVerifiedTrue();

    long countByAccountStatus(AccountStatus status);

    // Query personalizzate per statistiche
    @Query("SELECT COUNT(u) FROM User u WHERE u.enabled = true AND u.emailVerified = true")
    long countActiveUsers();

    @Query("SELECT COUNT(u) FROM User u WHERE u.emailVerified = false")
    long countPendingVerificationUsers();

    @Query("SELECT COUNT(u) FROM User u WHERE u.accountLockedUntil IS NOT NULL AND u.accountLockedUntil > :now")
    long countLockedUsers(@Param("now") LocalDateTime now);

    @Query("SELECT COUNT(u) FROM User u WHERE u.createdAt >= :since")
    long countUsersRegisteredSince(@Param("since") LocalDateTime since);

    // Ricerca utenti per stato
    List<User> findByEmailVerifiedFalse();

    List<User> findByEnabledFalse();

    List<User> findByAccountStatus(AccountStatus status);

    @Query("SELECT u FROM User u WHERE u.accountLockedUntil IS NOT NULL AND u.accountLockedUntil > :now")
    List<User> findLockedUsers(@Param("now") LocalDateTime now);

    // Pulizia automatica
    @Query("SELECT u FROM User u WHERE u.emailVerified = false AND u.createdAt < :cutoff")
    List<User> findUnverifiedUsersOlderThan(@Param("cutoff") LocalDateTime cutoff);
}