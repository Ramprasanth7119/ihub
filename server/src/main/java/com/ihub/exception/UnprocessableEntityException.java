package com.ihub.exception;

import org.springframework.http.HttpStatus;

/**
 * The request is syntactically valid but violates a business rule — e.g. a bid
 * below the required minimum, or a budget range that inverts itself.
 */
public class UnprocessableEntityException extends CustomException {

    public UnprocessableEntityException(String message) {
        super(message, HttpStatus.UNPROCESSABLE_ENTITY);
    }
}
