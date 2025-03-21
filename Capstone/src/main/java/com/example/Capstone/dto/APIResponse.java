package com.example.Capstone.dto;

public class APIResponse<T> {
    private final APIStatus status;
    private final T data;

    public APIResponse(APIStatus status, T data) {
        this.status = status;
        this.data = data;
    }

    // Getter per status e data
    public APIStatus getStatus() {
        return status;
    }

    public T getData() {
        return data;
    }
}