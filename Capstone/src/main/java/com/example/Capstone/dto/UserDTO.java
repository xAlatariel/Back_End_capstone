package com.example.Capstone.dto;

import com.example.Capstone.entity.Role;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class UserDTO {
    private Long id;
    private String nome;
    private String cognome;
    private String email;
    private Role ruolo;
    private String password;
}
