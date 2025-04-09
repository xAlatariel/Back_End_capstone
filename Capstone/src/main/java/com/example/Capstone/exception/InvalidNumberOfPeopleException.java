package com.example.Capstone.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidNumberOfPeopleException extends RuntimeException {
    public InvalidNumberOfPeopleException(String message) {
        super(message);
    }
}