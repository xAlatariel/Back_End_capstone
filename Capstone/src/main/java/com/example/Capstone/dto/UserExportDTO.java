package com.example.Capstone.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserExportDTO {
    private Long id;
    private String name;
    private String surname;
    private String email;
    private String role;
    private String status;
    private String emailVerified;
    private String registrationDate;
    private String lastLoginDate;
    private int totalReservations;
}