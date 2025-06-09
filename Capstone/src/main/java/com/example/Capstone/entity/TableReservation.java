package com.example.Capstone.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"user"})
@Table(name = "table_reservations",
        indexes = {
                @Index(name = "idx_reservation_date", columnList = "reservation_date"),
                @Index(name = "idx_reservation_user", columnList = "user_id"),
                @Index(name = "idx_reservation_datetime", columnList = "reservation_date, reservation_time")
        })
public class TableReservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "La data è obbligatoria")
    @Future(message = "La data deve essere futura")
    @Column(name = "reservation_date", nullable = false)
    private LocalDate reservationDate;

    @NotNull(message = "L'orario è obbligatorio")
    @Column(name = "reservation_time", nullable = false)
    private LocalTime reservationTime;

    @NotNull(message = "Il numero di persone è obbligatorio")
    @Min(value = 1, message = "Almeno 1 persona")
    @Max(value = 20, message = "Massimo 20 persone")
    @Column(nullable = false)
    private Integer numberOfPeople;

    @NotNull(message = "L'utente è obbligatorio")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_reservation_user"))
    private User user;

    @NotNull(message = "L'area è obbligatoria")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReservationArea reservationArea;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        validateReservation();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        validateReservation();
    }

    // Helper methods
    public LocalDateTime getReservationDateTime() {
        return LocalDateTime.of(reservationDate, reservationTime);
    }

    public boolean isPast() {
        return getReservationDateTime().isBefore(LocalDateTime.now());
    }

    public boolean isFuture() {
        return getReservationDateTime().isAfter(LocalDateTime.now());
    }

    public boolean isToday() {
        return reservationDate.equals(LocalDate.now());
    }

    // Validazione custom
    private void validateReservation() {
        if (reservationDate != null && reservationTime != null) {
            LocalDateTime reservationDateTime = LocalDateTime.of(reservationDate, reservationTime);
            if (reservationDateTime.isBefore(LocalDateTime.now())) {
                throw new IllegalArgumentException("La prenotazione deve essere per una data/ora futura");
            }
        }
    }
}