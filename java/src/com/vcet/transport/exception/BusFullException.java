package com.vcet.transport.exception;

/** Raised when a booking would exceed the bus capacity. */
public class BusFullException extends TransportException {
    public BusFullException(String message) { super(message); }
}
