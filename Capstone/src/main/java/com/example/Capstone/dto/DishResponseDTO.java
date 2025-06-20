package com.example.Capstone.dto;

import com.example.Capstone.entity.DishCategory;
import java.math.BigDecimal;

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