package com.example.Capstone.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserRequestDTO {

    @NotEmpty(message = "Il nome è obbligatorio")
    @Size(min = 2, max = 50, message = "Il nome deve essere tra 2 e 50 caratteri")
    private String name;

    @NotEmpty(message = "Il cognome è obbligatorio")
    @Size(min = 2, max = 50, message = "Il cognome deve essere tra 2 e 50 caratteri")
    private String surname;

    @NotEmpty(message = "L'email è obbligatoria")
    @Email(message = "Formato email non valido")
    private String email;

    private boolean enabled;

    private boolean emailVerified;
}