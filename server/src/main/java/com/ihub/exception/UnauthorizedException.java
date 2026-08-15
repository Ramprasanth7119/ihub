package com.ihub.exception;

import org.springframework.http.HttpStatus;

/** No usable credentials were presented, or they were rejected. */
public class UnauthorizedException extends CustomException {

    public UnauthorizedException(String message) {
        super(message, HttpStatus.UNAUTHORIZED);
    }
}
