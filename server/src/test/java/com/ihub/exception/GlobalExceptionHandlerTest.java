package com.ihub.exception;

import com.ihub.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Every domain exception used to collapse into HTTP 400. These assertions pin the
 * status each one now maps to, so the REST contract cannot silently regress.
 */
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();
    private final HttpServletRequest request = new MockHttpServletRequest("GET", "/api/test");

    @Test
    @DisplayName("a bare CustomException keeps its historical 400")
    void customExceptionDefaultsToBadRequest() {
        assertStatus(handler.handleCustomException(new CustomException("bad input"), request), HttpStatus.BAD_REQUEST);
    }

    @Test
    void notFoundMapsTo404() {
        assertStatus(handler.handleCustomException(new NotFoundException("gone"), request), HttpStatus.NOT_FOUND);
    }

    @Test
    void forbiddenMapsTo403() {
        assertStatus(handler.handleCustomException(new ForbiddenException("nope"), request), HttpStatus.FORBIDDEN);
    }

    @Test
    void unauthorizedMapsTo401() {
        assertStatus(handler.handleCustomException(new UnauthorizedException("who?"), request), HttpStatus.UNAUTHORIZED);
    }

    @Test
    void conflictMapsTo409() {
        assertStatus(handler.handleCustomException(new ConflictException("already running"), request), HttpStatus.CONFLICT);
    }

    @Test
    void unprocessableMapsTo422() {
        assertStatus(handler.handleCustomException(new UnprocessableEntityException("bid too low"), request),
                HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @Test
    void emptyResultMapsTo404() {
        assertStatus(handler.handleNotFound(new EmptyResultDataAccessException(1), request), HttpStatus.NOT_FOUND);
    }

    @Test
    void malformedBodyMapsTo400() {
        assertStatus(handler.handleUnreadable(new HttpMessageNotReadableException("bad json"), request),
                HttpStatus.BAD_REQUEST);
    }

    @Test
    void integrityViolationMapsTo409() {
        assertStatus(handler.handleIntegrity(
                new DataIntegrityViolationException("Duplicate entry 'x' for key 'users.email'"), request),
                HttpStatus.CONFLICT);
    }

    @Test
    @DisplayName("internal failures never leak the underlying exception message")
    void genericErrorIsOpaque() {
        ResponseEntity<ErrorResponse> response = handler.handleGeneric(
                new IllegalStateException("connection string user=root password=hunter2"), request);

        assertStatus(response, HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("An unexpected error occurred");
        assertThat(response.getBody().getMessage()).doesNotContain("password");
    }

    @Test
    @DisplayName("a database integrity message is not echoed back to the client")
    void integrityViolationIsOpaque() {
        ResponseEntity<ErrorResponse> response = handler.handleIntegrity(
                new DataIntegrityViolationException("Duplicate entry 'secret@x.com' for key 'users.email'"), request);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).doesNotContain("secret@x.com");
    }

    private void assertStatus(ResponseEntity<ErrorResponse> response, HttpStatus expected) {
        assertThat(response.getStatusCode()).isEqualTo(expected);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(expected.value());
        assertThat(response.getBody().getPath()).isEqualTo("/api/test");
    }
}
