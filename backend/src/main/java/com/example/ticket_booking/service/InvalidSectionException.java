package com.example.ticket_booking.service;

public class InvalidSectionException extends RuntimeException {

  public InvalidSectionException(String message) {
    super(message);
  }
}
