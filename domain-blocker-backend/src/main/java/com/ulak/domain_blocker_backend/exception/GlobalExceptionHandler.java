package com.ulak.domain_blocker_backend.exception;

import com.ulak.domain_blocker_backend.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final String ERR_ALREADY_EXISTS = "ERROR_ALREADY_EXISTS";
    private static final String ERR_INVALID_FORMAT = "INVALID_DOMAIN_FORMAT";
    private static final String ERR_INTERNAL_SERVER = "INTERNAL_SERVER_ERROR";

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        String errorKey = ex.getMessage();
        String logMessage;

        if (ERR_ALREADY_EXISTS.equals(errorKey)) {
            logMessage = "Duplicate Entry Attempt: The domain is already blocked.";
        } else if (ERR_INVALID_FORMAT.equals(errorKey)) {
            logMessage = "Invalid Format: User entered an invalid domain format.";
        } else {
            logMessage = "User Error Details: " + errorKey;
        }

        log.warn(logMessage);

        ErrorResponse error = new ErrorResponse(errorKey, System.currentTimeMillis());
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGlobalException(Exception ex) {

        log.error("An unexpected error occurred in the system", ex);
        ErrorResponse error = new ErrorResponse(ERR_INTERNAL_SERVER, System.currentTimeMillis());
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}