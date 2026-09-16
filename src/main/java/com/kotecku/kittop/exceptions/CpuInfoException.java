package com.kotecku.kittop.exceptions;

public class CpuInfoException extends RuntimeException {
    public CpuInfoException(String message) {
        super(message);
    }
    public CpuInfoException(String message, Throwable cause) {
        super(message, cause);
    }
}
