package com.example.errors;

public class InvalidReservation extends Exception {
    public InvalidReservation(String errorMessage) {
        super(errorMessage);
    }
}
