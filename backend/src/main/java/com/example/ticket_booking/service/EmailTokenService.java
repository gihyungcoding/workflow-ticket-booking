package com.example.ticket_booking.service;

import com.example.ticket_booking.domain.EmailTokenPurpose;
import java.util.Optional;
import org.springframework.stereotype.Service;

/** 일회용 이메일 링크 토큰의 발급과 소모(ADR-0032). 원문은 돌려주기만 하고 저장하지 않는다 — DB 에는 SHA-256 해시만 있다. */
@Service
public class EmailTokenService {

  /** 같은 사용자·용도의 이전 토큰을 지우고 새 토큰을 발급해 원문(43자)을 돌려준다. */
  public String issue(Long userId, EmailTokenPurpose purpose) {
    throw new UnsupportedOperationException();
  }

  /** 유효한 토큰이면 한 번만 소모하고 그 사용자 id 를, 아니면 빈 결과를 돌려준다. */
  public Optional<Long> consume(String rawToken, EmailTokenPurpose purpose) {
    throw new UnsupportedOperationException();
  }
}
