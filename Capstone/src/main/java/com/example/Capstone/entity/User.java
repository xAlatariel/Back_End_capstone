package com.example.Capstone.entity;


import jakarta.persistence.*;
import lombok.*;

@Entity
@Table (name = "users")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column (nullable = false)
    private String nome;

    @Column (nullable = false)
    private String cognome;

    @Column (nullable = false,unique = true)
    private String email;

    @Column (nullable = false)
    private String password;



    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Role ruolo = Role.USER;


}
