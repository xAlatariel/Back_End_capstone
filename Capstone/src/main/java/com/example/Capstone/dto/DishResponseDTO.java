package com.example.Capstone.dto;

import com.example.Capstone.entity.DishCategory;
import com.example.Capstone.entity.MenuType;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;


public record DishResponseDTO(
        Long id,
        String name,
        String description,
        String ingredients,
        DishCategory category,
        BigDecimal price,
        String formattedPrice,
        Boolean isAvailable,
        Integer displayOrder
) {}