package com.example.Capstone.controller;

import com.example.Capstone.config.ReservationSecurityService;
import com.example.Capstone.dto.TableReservationRequestDTO;
import com.example.Capstone.dto.TableReservationResponseDTO;
import com.example.Capstone.entity.TableReservation;
import com.example.Capstone.entity.ReservationArea;
import com.example.Capstone.entity.User;
import com.example.Capstone.exception.*;
import com.example.Capstone.repository.TableReservationRepository;
import com.example.Capstone.service.TableReservationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
public class TableReservationController {

    private final TableReservationService reservationService;
    private final ReservationSecurityService securityService;

    @PostMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<TableReservationResponseDTO> createReservation(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody TableReservationRequestDTO request
    ) throws CapacityExceededException, UserNotFoundException, InvalidReservationDateException,
            InvalidReservationTimeException, InvalidNumberOfPeopleException {
        log.debug("Creazione prenotazione per utente: {} con ruolo: {}", user.getEmail(), user.getRuolo());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(reservationService.createReservation(user.getId(), request));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @reservationSecurityService.canAccessReservation(#id, authentication.principal)")
    public ResponseEntity<TableReservationResponseDTO> getReservation(@PathVariable Long id)
            throws ReservationNotFoundException {
        log.debug("Richiesta prenotazione con ID: {}", id);
        return ResponseEntity.ok(reservationService.getReservationById(id));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<TableReservationResponseDTO>> getAllReservations() {
        log.debug("Richiesta tutte le prenotazioni (solo admin)");
        return ResponseEntity.ok(reservationService.getAllReservations());
    }

    @GetMapping("/user")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<List<TableReservationResponseDTO>> getUserReservations(
            @AuthenticationPrincipal User user
    ) {
        log.debug("Richiesta prenotazioni per utente: {}", user.getEmail());
        return ResponseEntity.ok(reservationService.getReservationsByUserId(user.getId()));
    }

    @GetMapping("/date/{date}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<TableReservationResponseDTO>> getReservationsByDate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        log.debug("Richiesta prenotazioni per data: {} (solo admin)", date);
        return ResponseEntity.ok(reservationService.getReservationsByDate(date));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @reservationSecurityService.isOwner(#id, authentication.principal)")
    public ResponseEntity<TableReservationResponseDTO> updateReservation(
            @PathVariable Long id,
            @Valid @RequestBody TableReservationRequestDTO request
    ) throws ReservationNotFoundException, CapacityExceededException, InvalidReservationDateException,
            InvalidReservationTimeException, InvalidNumberOfPeopleException, LateCancellationException {
        log.debug("Aggiornamento prenotazione ID: {}", id);
        return ResponseEntity.ok(reservationService.updateReservation(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @reservationSecurityService.isOwnerOrAdmin(#id, authentication.principal)")
    public ResponseEntity<Void> deleteReservation(@PathVariable Long id)
            throws ReservationNotFoundException, LateCancellationException {
        log.debug("Cancellazione prenotazione ID: {}", id);
        reservationService.deleteReservation(id);
        return ResponseEntity.noContent().build();
    }
}