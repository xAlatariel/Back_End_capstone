package com.example.Capstone.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "dishes",
        indexes = {
                @Index(name = "idx_dish_category", columnList = "category"),
                @Index(name = "idx_dish_menu", columnList = "menu_id"),
                @Index(name = "idx_dish_available", columnList = "is_available")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Dish {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Il nome del piatto è obbligatorio")
    @Size(min = 2, max = 100, message = "Il nome deve essere tra 2 e 100 caratteri")
    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(length = 200)
    private String ingredients; // Ingredienti principali

    @NotNull(message = "La categoria è obbligatoria")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DishCategory category;

    @DecimalMin(value = "0.0", inclusive = false, message = "Il prezzo deve essere maggiore di 0")
    @Digits(integer = 5, fraction = 2, message = "Formato prezzo non valido")
    @Column(precision = 7, scale = 2)
    private BigDecimal price;

    @Column(name = "is_available", nullable = false)
    @Builder.Default
    private Boolean isAvailable = true;

    @Column(name = "display_order")
    private Integer displayOrder; // Per ordinamento nella visualizzazione

    @NotNull(message = "Il menu è obbligatorio")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "menu_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_dish_menu"))
    private Menu menu;

    // Metodi helper
    public String getFormattedPrice() {
        return price != null ? "€ " + price.toString() : "Prezzo da definire";
    }
}