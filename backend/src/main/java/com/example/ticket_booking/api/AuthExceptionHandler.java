package com.example.ticket_booking.api;

import com.example.ticket_booking.api.dto.ErrorResponse;
import com.example.ticket_booking.service.EmailAlreadyExistsException;
import com.example.ticket_booking.service.InvalidCredentialsException;
import com.example.ticket_booking.service.UnauthenticatedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** 인증 API 의 409·401 만 담당한다. 요청 검증 실패(400)는 전역인 PerformanceExceptionHandler 가 이미 같은 형식으로 처리한다. */
@RestControllerAdvice
public class AuthExceptionHandler {

  @ExceptionHandler(EmailAlreadyExistsException.class)
  public ResponseEntity<ErrorResponse> handleEmailAlreadyExists(
      EmailAlreadyExistsException exception) {
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(new ErrorResponse("EMAIL_ALREADY_EXISTS", exception.getMessage()));
  }

  @ExceptionHandler(InvalidCredentialsException.class)
  public ResponseEntity<ErrorResponse> handleInvalidCredentials(
      InvalidCredentialsException exception) {
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
        .body(new ErrorResponse("INVALID_CREDENTIALS", exception.getMessage()));
  }

  @ExceptionHandler(UnauthenticatedException.class)
  public ResponseEntity<ErrorResponse> handleUnauthenticated(UnauthenticatedException exception) {
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
        .body(new ErrorResponse("UNAUTHORIZED", exception.getMessage()));
  }
}
