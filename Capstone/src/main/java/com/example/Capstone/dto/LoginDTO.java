package com.example.Capstone.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record LoginDTO(
                @NotBlank(message = "inserire l'email")
                @Email(message = "email non valida")
                String email,

                @NotNull(message = "inserire la password")
                String password
)
{

}
