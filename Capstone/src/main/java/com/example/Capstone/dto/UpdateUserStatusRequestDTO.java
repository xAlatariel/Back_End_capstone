package com.example.Capstone.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserStatusRequestDTO {

    @NotNull(message = "Lo stato è obbligatorio")
    private boolean enabled;

    private String reason;
}