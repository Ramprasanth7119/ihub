package com.ihub.exception;

import org.springframework.http.HttpStatus;

/** The requested resource does not exist (or is not visible to the caller). */
public class NotFoundException extends CustomException {

    public NotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND);
    }

    public static NotFoundException of(String entity, Object id) {
        return new NotFoundException(entity + " not found: " + id);
    }
}
