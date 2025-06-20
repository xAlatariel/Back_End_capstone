package com.example.Capstone.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "menus",
        indexes = {
                @Index(name = "idx_menu_type", columnList = "menu_type"),
                @Index(name = "idx_menu_date", columnList = "menu_date"),
                @Index(name = "idx_menu_active", columnList = "is_active")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Menu {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Il nome del menu è obbligatorio")
    @Size(min = 3, max = 100, message = "Il nome deve essere tra 3 e 100 caratteri")
    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    @NotNull(message = "Il tipo di menu è obbligatorio")
    @Enumerated(EnumType.STRING)
    @Column(name = "menu_type", nullable = false, length = 20)
    private MenuType menuType;

    @Column(name = "menu_date")
    private LocalDate menuDate; // Solo per DAILY menu

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @OneToMany(mappedBy = "menu", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Dish> dishes = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();

        // Per menu giornalieri, imposta la data di oggi se non specificata
        if (menuType == MenuType.DAILY && menuDate == null) {
            menuDate = LocalDate.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Metodi di validazione business
    public boolean isExpired() {
        if (menuType == MenuType.DAILY && menuDate != null) {
            return menuDate.isBefore(LocalDate.now());
        }
        return false;
    }

    public boolean isDailyMenu() {
        return menuType == MenuType.DAILY;
    }

    public boolean isSeasonalMenu() {
        return menuType == MenuType.SEASONAL;
    }
}