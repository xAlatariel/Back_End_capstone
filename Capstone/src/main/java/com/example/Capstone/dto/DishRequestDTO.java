package com.example.Capstone.dto;

import com.example.Capstone.entity.DishCategory;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record DishRequestDTO(
        @NotBlank(message = "Il nome del piatto è obbligatorio")
        String name,

        String description,
        String ingredients,

        @NotNull(message = "La categoria del piatto è obbligatoria")
        DishCategory category,

        @DecimalMin(value = "0.0", inclusive = false, message = "Il prezzo deve essere maggiore di 0")
        BigDecimal price,

        Boolean isAvailable,
        Integer displayOrder
) {}
