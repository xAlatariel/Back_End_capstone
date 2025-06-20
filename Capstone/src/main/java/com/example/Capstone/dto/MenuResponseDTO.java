package com.example.Capstone.dto;

import com.example.Capstone.entity.MenuType;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record MenuResponseDTO(
        Long id,
        String name,
        String description,
        MenuType menuType,
        LocalDate menuDate,
        Boolean isActive,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<DishResponseDTO> dishes
) {}