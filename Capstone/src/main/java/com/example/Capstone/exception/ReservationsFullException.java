package com.example.Capstone.exception;

public class ReservationsFullException extends RuntimeException {
    public ReservationsFullException(String message) {
        super(message);
    }
}