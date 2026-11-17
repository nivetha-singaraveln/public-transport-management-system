package com.vcet.transport.exception;

/** Raised when a booking / ticket cannot be found. */
public class BookingNotFoundException extends TransportException {
    public BookingNotFoundException(String message) { super(message); }
}
