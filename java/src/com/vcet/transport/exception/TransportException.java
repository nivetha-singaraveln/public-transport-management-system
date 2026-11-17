package com.vcet.transport.exception;

/** Base checked exception for every business-rule failure in the system. */
public class TransportException extends Exception {
    public TransportException(String message) { super(message); }
    public TransportException(String message, Throwable cause) { super(message, cause); }
}
