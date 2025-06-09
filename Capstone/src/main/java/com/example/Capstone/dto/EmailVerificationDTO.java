package com.example.Capstone.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class EmailVerificationDTO {
    @NotBlank(message = "Il token è obbligatorio")
    private String token;
}

