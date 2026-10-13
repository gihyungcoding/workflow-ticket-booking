package com.example.ticket_booking.api;

import com.example.ticket_booking.api.dto.ErrorResponse;
import com.example.ticket_booking.service.DuplicateSeatRangeException;
import com.example.ticket_booking.service.EmptySectionsException;
import com.example.ticket_booking.service.InvalidRequestException;
import com.example.ticket_booking.service.InvalidSectionException;
import com.example.ticket_booking.service.InvalidTimeOrderException;
import com.example.ticket_booking.service.PerformanceNotFoundException;
import com.example.ticket_booking.service.RegistrationAlreadyOpenException;
import com.example.ticket_booking.service.SeatLimitExceededException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class PerformanceExceptionHandler {

  private static final String SECTION_ELEMENT_FIELD_PREFIX = "sections[";

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
  public ResponseEntity<ErrorResponse> handleSeatLimitExceeded(
      SeatLimitExceededException exception) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(new ErrorResponse("SEAT_LIMIT_EXCEEDED", exception.getMessage()));
  }

  @ExceptionHandler(EmptySectionsException.class)
  public ResponseEntity<ErrorResponse> handleEmptySections(EmptySectionsException exception) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(new ErrorResponse("EMPTY_SECTIONS", exception.getMessage()));
  }

  @ExceptionHandler(DuplicateSeatRangeException.class)
  public ResponseEntity<ErrorResponse> handleDuplicateSeatRange(
      DuplicateSeatRangeException exception) {
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(new ErrorResponse("DUPLICATE_SEAT_RANGE", exception.getMessage()));
  }

  @ExceptionHandler(RegistrationAlreadyOpenException.class)
  public ResponseEntity<ErrorResponse> handleRegistrationAlreadyOpen(
      RegistrationAlreadyOpenException exception) {
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(new ErrorResponse("REGISTRATION_ALREADY_OPEN", exception.getMessage()));
  }

  @ExceptionHandler(InvalidRequestException.class)
  public ResponseEntity<ErrorResponse> handleInvalidRequest(InvalidRequestException exception) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(new ErrorResponse("INVALID_REQUEST", exception.getMessage()));
  }

  @ExceptionHandler(InvalidSectionException.class)
  public ResponseEntity<ErrorResponse> handleInvalidSection(InvalidSectionException exception) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(new ErrorResponse("INVALID_SECTION", exception.getMessage()));
  }

  /**
   * 최상위 필드 위반을 구역 필드 위반보다 우선한다 — 둘을 동시에 어긴 요청이 수동 검증 시절(validateRequired 가 validateSection 보다 먼저
   * 실행)과 같은 코드를 받도록 맞춘 것이다.
   */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleValidationFailure(
      MethodArgumentNotValidException exception) {
    List<FieldError> fieldErrors = exception.getBindingResult().getFieldErrors();
    FieldError topLevelViolation =
        fieldErrors.stream()
            .filter(error -> !isSectionElementField(error))
            .findFirst()
            .orElse(null);
    if (topLevelViolation == null && !fieldErrors.isEmpty()) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST)
          .body(new ErrorResponse("INVALID_SECTION", describe(fieldErrors.get(0))));
    }
    String message = topLevelViolation == null ? "요청 형식이 올바르지 않습니다" : describe(topLevelViolation);
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(new ErrorResponse("INVALID_REQUEST", message));
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ErrorResponse> handleUnreadableBody(
      HttpMessageNotReadableException exception) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(new ErrorResponse("INVALID_REQUEST", "요청 본문을 읽을 수 없습니다"));
  }

  /** 구역 배열 원소의 필드만 해당한다 — sections 자체에 대한 위반은 최상위 필드로 본다. */
  private static boolean isSectionElementField(FieldError error) {
    return error.getField().startsWith(SECTION_ELEMENT_FIELD_PREFIX);
  }

  private static String describe(FieldError error) {
    return error.getField() + ": " + error.getDefaultMessage();
  }
}
