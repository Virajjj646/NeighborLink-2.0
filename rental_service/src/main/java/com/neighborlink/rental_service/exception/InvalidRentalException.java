package com.neighborlink.rental_service.exception;

public class InvalidRentalException extends RuntimeException {

    public InvalidRentalException(String message) {
        super(message);
    }
}