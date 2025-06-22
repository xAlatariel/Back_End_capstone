package com.example.Capstone.dto;

import com.example.Capstone.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserRoleRequestDTO {

    @NotNull(message = "Il ruolo è obbligatorio")
    private Role role;

    private String reason;
}