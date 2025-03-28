package com.example.Capstone.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@ResponseStatus(HttpStatus.CONFLICT)
public class CapacityExceededException extends RuntimeException {
    public CapacityExceededException(String area, LocalDate date, int maxCapacity) {
        super(String.format("Capacity exceeded for %s area on %s. Max capacity: %d",
                area, date, maxCapacity));
    }
}