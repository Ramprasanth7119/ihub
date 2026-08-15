package com.ihub.exception;

import org.springframework.http.HttpStatus;

/**
 * The request is well-formed but conflicts with the current state of the resource —
 * e.g. starting an auction that is already active, or auctioning an idea that
 * already has a live auction.
 */
public class ConflictException extends CustomException {

    public ConflictException(String message) {
        super(message, HttpStatus.CONFLICT);
    }
}
