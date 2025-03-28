package com.example.Capstone.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

//da implementare
@Data
public class PasswordUpdateDTO {
    @NotBlank(message = "La nuova password è obbligatoria")
    @Size(min = 8, message = "La password deve avere almeno 8 caratteri")
    private String newPassword;
}