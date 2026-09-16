package com.kotecku.kittop.exceptions;

public class DiskInfoException extends RuntimeException {

    public DiskInfoException(String message) {
        super(message);
    }

    public DiskInfoException(String message, Throwable cause) {
        super(message, cause);
    }
}