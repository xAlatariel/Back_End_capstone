package com.example.Capstone.dto;

import com.example.Capstone.entity.ReservationArea;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
public class TableReservationResponseDTO {
    private Long id;
    private LocalDate reservationDate;  // Campo aggiunto
    private LocalTime reservationTime;
    private Integer numberOfPeople;
    private ReservationArea reservationArea;
    private Long userId;
    private String userFullName;
}