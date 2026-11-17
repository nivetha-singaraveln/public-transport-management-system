package com.vcet.transport.exception;

/** Raised for invalid user input or an invalid operation. */
public class ValidationException extends TransportException {
    public ValidationException(String message) { super(message); }
}
