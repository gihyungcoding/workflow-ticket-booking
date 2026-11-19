package com.example.ticket_booking.service;

import org.springframework.stereotype.Service;

/**
 * 계정으로 나가는 메일의 발송(ADR-0031). 인증·재설정·소셜 안내는 같은 사용자에게 60초에 한 통만 보낸다(간격 제한) — 결과 true 는 보냄, false 는
 * 건너뜀. 변경 알림은 간격 없이 보낸다. 메일은 커밋 뒤 비동기로 나가고 발송 실패는 로그만 남긴다.
 */
@Service
public class AccountMailService {

  public boolean sendVerificationMail(Long userId) {
    throw new UnsupportedOperationException();
  }

  public boolean sendPasswordResetMail(Long userId) {
    throw new UnsupportedOperationException();
  }

  public boolean sendSocialAccountNotice(Long userId) {
    throw new UnsupportedOperationException();
  }

  public void sendPasswordChangedNotice(Long userId) {
    throw new UnsupportedOperationException();
  }
}
