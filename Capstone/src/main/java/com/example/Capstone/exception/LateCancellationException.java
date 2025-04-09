package com.example.Capstone.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class LateCancellationException extends RuntimeException {
    public LateCancellationException(String message) {
        super(message);
    }
}