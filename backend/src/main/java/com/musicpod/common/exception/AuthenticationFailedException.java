package com.musicpod.common.exception;

public class AuthenticationFailedException
        extends RuntimeException {

    public AuthenticationFailedException(
            String message) {

        super(message);
    }
}