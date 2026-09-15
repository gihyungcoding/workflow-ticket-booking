package com.example.ticket_booking.api;

import com.example.ticket_booking.api.dto.ErrorResponse;
import com.example.ticket_booking.service.DuplicateSeatRangeException;
import com.example.ticket_booking.service.EmptySectionsException;
import com.example.ticket_booking.service.InvalidTimeOrderException;
import com.example.ticket_booking.service.PerformanceNotFoundException;
import com.example.ticket_booking.service.RegistrationAlreadyOpenException;
import com.example.ticket_booking.service.SeatLimitExceededException;
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

  @ExceptionHandler(InvalidTimeOrderException.class)
  public ResponseEntity<ErrorResponse> handleInvalidTimeOrder(InvalidTimeOrderException exception) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(new ErrorResponse("INVALID_TIME_ORDER", exception.getMessage()));
  }

  @ExceptionHandler(SeatLimitExceededException.class)
  public ResponseEntity<ErrorResponse> handleSeatLimitExceeded(SeatLimitExceededException exception) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(new ErrorResponse("SEAT_LIMIT_EXCEEDED", exception.getMessage()));
  }

  @ExceptionHandler(EmptySectionsException.class)
  public ResponseEntity<ErrorResponse> handleEmptySections(EmptySectionsException exception) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(new ErrorResponse("EMPTY_SECTIONS", exception.getMessage()));
  }

  @ExceptionHandler(DuplicateSeatRangeException.class)
  public ResponseEntity<ErrorResponse> handleDuplicateSeatRange(DuplicateSeatRangeException exception) {
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(new ErrorResponse("DUPLICATE_SEAT_RANGE", exception.getMessage()));
  }

  @ExceptionHandler(RegistrationAlreadyOpenException.class)
  public ResponseEntity<ErrorResponse> handleRegistrationAlreadyOpen(
      RegistrationAlreadyOpenException exception) {
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(new ErrorResponse("REGISTRATION_ALREADY_OPEN", exception.getMessage()));
  }
}
