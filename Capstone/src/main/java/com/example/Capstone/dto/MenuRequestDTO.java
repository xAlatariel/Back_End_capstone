package com.example.Capstone.dto;

import com.example.Capstone.entity.DishCategory;
import com.example.Capstone.entity.MenuType;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

// Request DTO per creazione menu
public record MenuRequestDTO(
        @NotBlank(message = "Il nome del menu è obbligatorio")
        @Size(min = 3, max = 100)
        String name,

        String description,

        @NotNull(message = "Il tipo di menu è obbligatorio")
        MenuType menuType,

        LocalDate menuDate, // Solo per DAILY menu

        @NotNull(message = "I piatti sono obbligatori")
        @Size(min = 1, message = "Deve esserci almeno un piatto")
        List<DishRequestDTO> dishes
) {}