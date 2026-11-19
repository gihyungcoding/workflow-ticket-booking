package com.example.ticket_booking.service;

import org.springframework.stereotype.Component;

/** 계정 메일 네 종류의 문구와 링크(요구사항 §3-4). 링크는 FRONTEND_BASE_URL 로만 만든다 — 요청의 Host 를 쓰지 않는다. */
@Component
public class AccountMails {

  public MailMessage verification(String to, String rawToken) {
    throw new UnsupportedOperationException();
  }

  public MailMessage passwordReset(String to, String rawToken) {
    throw new UnsupportedOperationException();
  }

  public MailMessage passwordChanged(String to) {
    throw new UnsupportedOperationException();
  }

  public MailMessage socialAccountNotice(String to) {
    throw new UnsupportedOperationException();
  }
}
