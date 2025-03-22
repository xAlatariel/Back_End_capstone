package com.example.Capstone.controller;

import com.example.Capstone.config.ReservationSecurityService;
import com.example.Capstone.dto.TableReservationRequestDTO;
import com.example.Capstone.dto.TableReservationResponseDTO;
import com.example.Capstone.entity.TableReservation;
import com.example.Capstone.entity.ReservationArea;
import com.example.Capstone.entity.User;
import com.example.Capstone.exception.CapacityExceededException;
import com.example.Capstone.exception.ReservationNotFoundException;
import com.example.Capstone.repository.TableReservationRepository;
import com.example.Capstone.service.TableReservationService;
import com.example.Capstone.exception.UserNotFoundException;
import com.example.Capstone.exception.ReservationsFullException;
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

import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

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
    ) throws CapacityExceededException, UserNotFoundException {
        System.out.println("Utente autenticato: " + user.getEmail() + ", Ruolo: " + user.getRuolo()); // Debug
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(reservationService.createReservation(user.getId(), request));
    }




    @GetMapping("/{id}")
    @PreAuthorize("@reservationSecurityService.canAccessReservation(#id, principal)")
    public ResponseEntity<TableReservationResponseDTO> getReservation(@PathVariable Long id)
            throws ReservationNotFoundException {
        return ResponseEntity.ok(reservationService.getReservationById(id));
    }




    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<TableReservationResponseDTO>> getAllReservations() {
        return ResponseEntity.ok(reservationService.getAllReservations());
    }



    @GetMapping("/date/{date}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<TableReservationResponseDTO>> getReservationsByDate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        List<TableReservationResponseDTO> reservations = reservationService.getReservationsByDate(date);
        return ResponseEntity.ok(reservationService.getReservationsByDate(date));
    }


    @PutMapping("/{id}")
    @PreAuthorize("@reservationSecurityService.isOwner(#id, principal)")
    public ResponseEntity<TableReservationResponseDTO> updateReservation(
            @PathVariable Long id,
            @Valid @RequestBody TableReservationRequestDTO request
    ) throws ReservationNotFoundException, CapacityExceededException {
        return ResponseEntity.ok(reservationService.updateReservation(id, request));
    }


    @DeleteMapping("/{id}")
    @PreAuthorize("@reservationSecurityService.isOwnerOrAdmin(#id, principal)")
    public ResponseEntity<Void> deleteReservation(@PathVariable Long id)
            throws ReservationNotFoundException {
        reservationService.deleteReservation(id);
        return ResponseEntity.noContent().build();
    }
}


