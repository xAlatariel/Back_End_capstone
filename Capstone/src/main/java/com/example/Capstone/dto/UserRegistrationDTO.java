package com.example.Capstone.dto;


import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Data
public class UserRegistrationDTO {
    @NotBlank(message = "Campo obbligatorio")
    private String nome;

    @NotBlank(message = "Campo obbligatorio")
    private String cognome;

    @Email(message = "Email non valida")
    @NotBlank(message = "L'email è obbligatoria")
    private String email;

    @Size(min = 8,message = "La password deve contente almeno 8 caratteri")
    @NotBlank(message = "Password obbligatoria")
    private String password;

    @NotBlank(message = "Conferma la password")
    private String confirmPassword;

}
