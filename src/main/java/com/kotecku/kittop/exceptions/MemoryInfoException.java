package com.kotecku.kittop.exceptions;

public class MemoryInfoException extends RuntimeException {

    public MemoryInfoException(String message) {
        super(message);
    }

    public MemoryInfoException(String message, Throwable cause) {
        super(message, cause);
    }
}