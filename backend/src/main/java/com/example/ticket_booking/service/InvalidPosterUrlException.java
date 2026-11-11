package com.example.ticket_booking.service;

public class InvalidPosterUrlException extends RuntimeException {

  public InvalidPosterUrlException() {
    super("posterUrl 은 500자 이하의 https 절대 URL 이어야 합니다");
  }
}
