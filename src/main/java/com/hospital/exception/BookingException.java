package com.hospital.exception;

/** Thrown when a booking or status change breaks a business rule. */
public class BookingException extends RuntimeException {

    public BookingException(String message) {
        super(message);
    }
}
