package com.example.Capstone.dto;

import com.example.Capstone.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateUserRequestDTO {

    @NotEmpty(message = "Il nome è obbligatorio")
    @Size(min = 2, max = 50, message = "Il nome deve essere tra 2 e 50 caratteri")
    private String name;

    @NotEmpty(message = "Il cognome è obbligatorio")
    @Size(min = 2, max = 50, message = "Il cognome deve essere tra 2 e 50 caratteri")
    private String surname;

    @NotEmpty(message = "L'email è obbligatoria")
    @Email(message = "Formato email non valido")
    private String email;

    @NotNull(message = "Il ruolo è obbligatorio")
    private Role role;

    private boolean enabled = true;

    private boolean emailVerified = false;

    private boolean sendWelcomeEmail = true;
}