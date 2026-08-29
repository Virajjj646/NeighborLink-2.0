package com.neighborlink.rental_service.exception;

public class RentalNotFoundException extends RuntimeException {

    public RentalNotFoundException(String message) {
        super(message);
    }
}