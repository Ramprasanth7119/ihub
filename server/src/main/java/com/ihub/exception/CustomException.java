package com.ihub.exception;

import org.springframework.http.HttpStatus;

/**
 * Base application exception.
 *
 * <p>Historically every domain error in iHub was signalled with a bare
 * {@code CustomException}, which the global handler always translated to HTTP 400.
 * The exception now carries the status it should map to, so callers can express
 * "not found", "forbidden" or "conflict" without the REST contract collapsing
 * into a single status code. The single-argument constructor keeps its original
 * meaning ({@link HttpStatus#BAD_REQUEST}) so existing call sites behave exactly
 * as before.</p>
 */
public class CustomException extends RuntimeException {

    private final HttpStatus status;

    public CustomException(String message) {
        this(message, HttpStatus.BAD_REQUEST);
    }

    public CustomException(String message, HttpStatus status) {
        super(message);
        this.status = status != null ? status : HttpStatus.BAD_REQUEST;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
