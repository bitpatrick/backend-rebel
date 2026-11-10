package com.rebel.util;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_CONTENT;

import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.reactive.resource.NoResourceFoundException;

import com.rebel.api.exception.InvalidInputException;
import com.rebel.api.exception.NotFoundException;

import jakarta.validation.ConstraintViolationException;

@RestControllerAdvice
public class GlobalControllerExceptionHandler {

    private static final Logger LOG =
            LoggerFactory.getLogger(GlobalControllerExceptionHandler.class);

    /*
     * Resource not found (404)
     */
    @ResponseStatus(NOT_FOUND)
    @ExceptionHandler(NotFoundException.class)
    public HttpErrorInfo handleNotFoundExceptions(
            ServerHttpRequest request, NotFoundException ex) {

        LOG.warn("Resource not found - method: {}, path: {}, message: {}",
                request.getMethod(), getPath(request), ex.getMessage());

        return createHttpErrorInfo(NOT_FOUND, request, ex);
    }

    /*
     * Invalid business input (422)
     */
    @ResponseStatus(BAD_REQUEST)
    @ExceptionHandler(InvalidInputException.class)
    public HttpErrorInfo handleInvalidInputException(
            ServerHttpRequest request, InvalidInputException ex) {

        LOG.warn("Invalid input - method: {}, path: {}, message: {}",
                request.getMethod(), getPath(request), ex.getMessage());

        return createHttpErrorInfo(BAD_REQUEST, request, ex);
    }

    /*
     * Validation of @RequestBody (400)
     */
    @ResponseStatus(BAD_REQUEST)
    @ExceptionHandler(WebExchangeBindException.class)
    public HttpErrorInfo handleWebExchangeBindException(
            ServerHttpRequest request, WebExchangeBindException exception) {

        // Collect all validation errors, including object-level errors.
        String message = exception.getAllErrors()
                .stream()
                .map(error -> {
                    if (error instanceof org.springframework.validation.FieldError fieldError) {
                        return fieldError.getField() + ": " + fieldError.getDefaultMessage();
                    }
                    return error.getDefaultMessage();
                })
                .collect(Collectors.joining("; "));

        if (message.isBlank()) {
            message = "Validation failure";
        }

        LOG.warn("Request body validation failed - method: {}, path: {}, errors: {}",
                request.getMethod(), getPath(request), message);

        return createHttpErrorInfo(BAD_REQUEST, request, message);
    }

    /*
     * Method validation (400)
     */
    @ResponseStatus(BAD_REQUEST)
    @ExceptionHandler(HandlerMethodValidationException.class)
    public HttpErrorInfo handleValidationException(
            ServerHttpRequest request, HandlerMethodValidationException exception) {

        LOG.warn("Method validation failed - method: {}, path: {}, message: {}",
                request.getMethod(), getPath(request), exception.getMessage());

        return createHttpErrorInfo(BAD_REQUEST, request, exception);
    }

    /*
     * Constraint validation (400)
     */
    @ResponseStatus(BAD_REQUEST)
    @ExceptionHandler(ConstraintViolationException.class)
    public HttpErrorInfo handleConstraintViolation(
            ServerHttpRequest request, ConstraintViolationException exception) {

        String message = exception.getConstraintViolations()
                .stream()
                .map(violation ->
                        violation.getPropertyPath() + ": " + violation.getMessage())
                .collect(Collectors.joining("; "));

        LOG.warn("Constraint validation failed - method: {}, path: {}, errors: {}",
                request.getMethod(), getPath(request), message);

        return createHttpErrorInfo(BAD_REQUEST, request, message);
    }

    /*
     * Endpoint or static resource not found (404)
     */
    @ResponseStatus(NOT_FOUND)
    @ExceptionHandler(NoResourceFoundException.class)
    public HttpErrorInfo handleNoResourceFound(
            ServerHttpRequest request, NoResourceFoundException ex) {

        LOG.warn("Endpoint/resource not found - method: {}, path: {}",
                request.getMethod(), getPath(request));

        return createHttpErrorInfo(NOT_FOUND, request, ex);
    }

    /*
     * Unexpected exceptions (500)
     */
    @ResponseStatus(INTERNAL_SERVER_ERROR)
    @ExceptionHandler(Exception.class)
    public HttpErrorInfo handleGenericException(
            ServerHttpRequest request, Exception exception) {

        // Log the complete exception, including its stack trace.
        LOG.error("Unexpected error - method: {}, path: {}",
                request.getMethod(), getPath(request), exception);

        return createHttpErrorInfo(INTERNAL_SERVER_ERROR, request, exception);
    }

    /*
     * Shared response creation
     */
    private HttpErrorInfo createHttpErrorInfo(
            HttpStatus httpStatus, ServerHttpRequest request, Exception ex) {

        return createHttpErrorInfo(httpStatus, request, ex.getMessage());
    }

    private HttpErrorInfo createHttpErrorInfo(
            HttpStatus httpStatus, ServerHttpRequest request, String message) {

        LOG.debug("Returning HTTP {} - method: {}, path: {}, message: {}",
                httpStatus.value(), request.getMethod(), getPath(request), message);

        return new HttpErrorInfo(httpStatus, getPath(request), message);
    }

    private String getPath(ServerHttpRequest request) {
        return request.getPath().pathWithinApplication().value();
    }
}