package com.example.ticket_booking.service;

public class EmptySectionsException extends RuntimeException {

  public EmptySectionsException() {
    super("구역이 1개 이상 필요합니다");
  }
}
