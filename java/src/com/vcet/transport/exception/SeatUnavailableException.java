package com.vcet.transport.exception;

/** Raised when the requested seat is already booked or invalid. */
public class SeatUnavailableException extends TransportException {
    public SeatUnavailableException(String message) { super(message); }
}
