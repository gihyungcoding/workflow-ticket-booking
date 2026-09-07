package com.example.ticket_booking.service;

public class PerformanceNotFoundException extends RuntimeException {

  public PerformanceNotFoundException(Long id) {
    super("Performance not found: " + id);
  }
}
