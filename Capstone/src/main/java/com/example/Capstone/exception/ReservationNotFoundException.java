package com.example.Capstone.exception;

import com.example.Capstone.entity.ReservationArea;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.time.LocalDate;

@ResponseStatus(HttpStatus.NOT_FOUND) // 404 Not Found
public class ReservationNotFoundException extends RuntimeException {
    public ReservationNotFoundException(Long reservationId) {
        super("Prenotazione non trovata con ID: " + reservationId);
    }

    public ReservationNotFoundException(String message) {
        super(message);
    }

    public ReservationNotFoundException(LocalDate date, ReservationArea area) {
        super("Nessuna prenotazione trovata per il " + date + " nell'area " + area);
    }
}