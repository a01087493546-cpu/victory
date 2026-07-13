package com.victory.exception;

public class DuplicateLoginIdException extends RuntimeException {

    public DuplicateLoginIdException(String message) {
        super(message);
    }
}
