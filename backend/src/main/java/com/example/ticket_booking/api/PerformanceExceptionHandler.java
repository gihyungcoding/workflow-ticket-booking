package com.example.ticket_booking.api;

import com.example.ticket_booking.api.dto.ErrorResponse;
import com.example.ticket_booking.service.PerformanceNotFoundException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class PerformanceExceptionHandler {

    @ExceptionHandler(PerformanceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(PerformanceNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("PERFORMANCE_NOT_FOUND", exception.getMessage()));
    }
}
