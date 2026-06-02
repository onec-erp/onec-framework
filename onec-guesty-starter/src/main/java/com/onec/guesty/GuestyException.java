package com.onec.guesty;

/** Raised when communication with the Guesty Open API fails at the transport or protocol level. */
public class GuestyException extends RuntimeException {

    public GuestyException(String message) {
        super(message);
    }

    public GuestyException(String message, Throwable cause) {
        super(message, cause);
    }
}
