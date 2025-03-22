package com.example.Capstone.dto;

import com.example.Capstone.entity.ReservationArea;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
public class TableReservationRequestDTO {
    @NotNull(message = "La data è obbligatoria")
    @Future(message = "La data deve essere futura")
    private LocalDate reservationDate;

    @NotNull(message = "L'orario è obbligatorio")
    private LocalTime reservationTime;

    @NotNull(message = "Il numero di persone è obbligatorio")
    @Min(value = 1, message = "Almeno 1 persona")
    @Max(value = 20, message = "Massimo 20 persone")
    private Integer numberOfPeople;

    @NotNull(message = "L'area è obbligatoria")
    private String reservationArea;

    private Long userId;

}