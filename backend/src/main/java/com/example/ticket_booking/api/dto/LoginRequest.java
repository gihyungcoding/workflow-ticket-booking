package com.example.ticket_booking.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {

  /** 이메일 앞뒤 공백은 검증 전에 걷어낸다 — 가입 때와 같은 표기로 로그인할 수 있게 한다. */
  public LoginRequest {
    email = email == null ? null : email.trim();
  }
}
