package com.example.Capstone.dto;

import com.example.Capstone.entity.AccountStatus;
import com.example.Capstone.entity.Role;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Data
@Getter
@Setter
public class UserDTO {
    private Long id;
    private String nome;
    private String cognome;
    private String email;
    private Role ruolo;
    private AccountStatus accountStatus;
    private Boolean emailVerified;
    private Boolean enabled;
    private LocalDateTime createdAt;
    private LocalDateTime lastLogin;
    private LocalDateTime emailVerifiedAt;
    private Integer failedLoginAttempts;

    // Campi calcolati
    private boolean accountLocked;
    private boolean accountVerified;

    public String getFullName() {
        return nome + " " + cognome;
    }
}