package com.example.Capstone.repository;

import com.example.Capstone.entity.EmailVerificationToken;
import com.example.Capstone.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, Long> {

    Optional<EmailVerificationToken> findByToken(String token);

    Optional<EmailVerificationToken> findByUser(User user);

    Optional<EmailVerificationToken> findByUserEmail(String email);

    boolean existsByToken(String token);

    @Modifying
    @Query("DELETE FROM EmailVerificationToken e WHERE e.expiresAt < :now")
    void deleteExpiredTokens(@Param("now") LocalDateTime now);

    @Modifying
    @Query("DELETE FROM EmailVerificationToken e WHERE e.user = :user")
    void deleteByUser(@Param("user") User user);

    @Query("SELECT COUNT(e) FROM EmailVerificationToken e WHERE e.createdAt > :since")
    long countTokensCreatedSince(@Param("since") LocalDateTime since);
}