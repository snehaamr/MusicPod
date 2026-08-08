package com.musicpod.common.exception;

public class ResourceConflictException
        extends RuntimeException {

    public ResourceConflictException(String message) {
        super(message);
    }
}