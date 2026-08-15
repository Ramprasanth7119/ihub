package com.ihub.exception;

import org.springframework.http.HttpStatus;

/** The caller is authenticated but not allowed to perform this action. */
public class ForbiddenException extends CustomException {

    public ForbiddenException(String message) {
        super(message, HttpStatus.FORBIDDEN);
    }
}
