package com.example.Capstone.entity;

public enum MenuType {
    DAILY("Menu del Giorno"),
    SEASONAL("Menu Stagionale");

    private final String displayName;

    MenuType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}