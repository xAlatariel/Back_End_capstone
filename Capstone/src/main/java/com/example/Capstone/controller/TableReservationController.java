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

@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
public class TableReservationController {

    private final TableReservationService reservationService;
    private final ReservationSecurityService securityService;

    //corretto
    @PostMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN' )")
    public ResponseEntity<TableReservationResponseDTO> createReservation(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody TableReservationRequestDTO request
    ) throws CapacityExceededException, UserNotFoundException, InvalidReservationDateException,
            InvalidReservationTimeException, InvalidNumberOfPeopleException {
        System.out.println("Utente autenticato: " + user.getEmail() + ", Ruolo: " + user.getRuolo()); // Debug
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(reservationService.createReservation(user.getId(), request));
    }

    //corretto
    @GetMapping("/{id}")
    @PreAuthorize("@reservationSecurityService.canAccessReservation(#id, principal)")
    public ResponseEntity<TableReservationResponseDTO> getReservation(@PathVariable Long id)
            throws ReservationNotFoundException {
        return ResponseEntity.ok(reservationService.getReservationById(id));
    }

    //corretto
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<TableReservationResponseDTO>> getAllReservations() {
        return ResponseEntity.ok(reservationService.getAllReservations());
    }

    //corretto
    @GetMapping("/user")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<List<TableReservationResponseDTO>> getUserReservations(
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(reservationService.getReservationsByUserId(user.getId()));
    }

    //corretto
    @GetMapping("/date/{date}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<TableReservationResponseDTO>> getReservationsByDate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        List<TableReservationResponseDTO> reservations = reservationService.getReservationsByDate(date);
        return ResponseEntity.ok(reservationService.getReservationsByDate(date));
    }

    //corretto
    @PutMapping("/{id}")
    @PreAuthorize("@reservationSecurityService.isOwner(#id, principal)")
    public ResponseEntity<TableReservationResponseDTO> updateReservation(
            @PathVariable Long id,
            @Valid @RequestBody TableReservationRequestDTO request
    ) throws ReservationNotFoundException, CapacityExceededException, InvalidReservationDateException,
            InvalidReservationTimeException, InvalidNumberOfPeopleException, LateCancellationException {
        return ResponseEntity.ok(reservationService.updateReservation(id, request));
    }

    //corretto
    @DeleteMapping("/{id}")
    @PreAuthorize("@reservationSecurityService.isOwnerOrAdmin(#id, principal)")
    public ResponseEntity<Void> deleteReservation(@PathVariable Long id)
            throws ReservationNotFoundException, LateCancellationException {
        reservationService.deleteReservation(id);
        return ResponseEntity.noContent().build();
    }
}

