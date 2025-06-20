package com.example.Capstone.dto;

import com.example.Capstone.entity.DishCategory;
import com.example.Capstone.entity.MenuType;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record DishRequestDTO(
        @NotBlank(message = "Il nome del piatto è obbligatorio")
        @Size(min = 2, max = 100)
        String name,

        String description,
        String ingredients,

        @NotNull(message = "La categoria è obbligatoria")
        DishCategory category,

        @DecimalMin(value = "0.0", inclusive = false)
        @Digits(integer = 5, fraction = 2)
        BigDecimal price,

        Boolean isAvailable,
        Integer displayOrder
) {}