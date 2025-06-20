package com.example.Capstone.dto;

import com.example.Capstone.entity.MenuType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

public record MenuRequestDTO(
        @NotBlank(message = "Il nome del menu è obbligatorio")
        String name,

        String description,

        @NotNull(message = "Il tipo di menu è obbligatorio")
        MenuType menuType,

        LocalDate menuDate,

        Boolean isActive,

        @NotEmpty(message = "Il menu deve contenere almeno un piatto")
        @Valid
        List<DishRequestDTO> dishes
) {}