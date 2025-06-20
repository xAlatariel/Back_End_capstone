package com.example.Capstone.entity;

public enum DishCategory {
    ANTIPASTI("Antipasti"),
    PRIMI("Primi Piatti"),
    SECONDI("Secondi Piatti"),
    CONTORNI("Contorni"),
    DOLCI("Dolci");

    private final String displayName;

    DishCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}