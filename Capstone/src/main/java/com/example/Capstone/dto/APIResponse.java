package com.example.Capstone.dto;

public class APIResponse<T> {
    private final APIStatus status;
    private final String message;
    private final T data;

    // Costruttore con solo status e messaggio (per messaggi senza dati)
    public APIResponse(APIStatus status, String message) {
        this.status = status;
        this.message = message;
        this.data = null;
    }

    // Costruttore con status, messaggio e dati
    public APIResponse(APIStatus status, String message, T data) {
        this.status = status;
        this.message = message;
        this.data = data;
    }

    // Costruttore legacy (per retrocompatibilità)
    public APIResponse(APIStatus status, T data) {
        this.status = status;
        this.message = null;
        this.data = data;
    }

    // Getter
    public APIStatus getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public T getData() {
        return data;
    }

    // Metodi di utilità per creare risposte standard
    public static <T> APIResponse<T> success(String message) {
        return new APIResponse<>(APIStatus.SUCCESS, message);
    }

    public static <T> APIResponse<T> success(String message, T data) {
        return new APIResponse<>(APIStatus.SUCCESS, message, data);
    }

    public static <T> APIResponse<T> error(String message) {
        return new APIResponse<>(APIStatus.ERROR, message);
    }

    public static <T> APIResponse<T> error(String message, T data) {
        return new APIResponse<>(APIStatus.ERROR, message, data);
    }
}