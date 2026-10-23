package com.example.ticket_booking.service;

/** 가입 완료 시점에 같은 제공자 계정이 이미 다른 사용자에게 연결돼 있다. */
public class SocialAccountAlreadyLinkedException extends RuntimeException {

  public SocialAccountAlreadyLinkedException() {
    super("이미 다른 계정에 연결된 소셜 계정입니다");
  }
}
