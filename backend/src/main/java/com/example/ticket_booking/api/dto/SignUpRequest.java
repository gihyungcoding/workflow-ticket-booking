package com.example.ticket_booking.api.dto;

import com.example.ticket_booking.domain.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SignUpRequest(
    @NotBlank
        @Email
        @Size(max = 255)
        @Pattern(regexp = NoLoneSurrogate.REGEX, message = NoLoneSurrogate.MESSAGE)
        String email,
    @NotBlank
        @Size(min = 8, max = 72)
        @Pattern(regexp = NoLoneSurrogate.REGEX, message = NoLoneSurrogate.MESSAGE)
        String password,
    @NotBlank String passwordConfirm,
    @NotBlank
        @Size(max = 100)
        @Pattern(regexp = "^[^\\p{Cc}\\p{Cs}]*$", message = "제어 문자나 짝 없는 서로게이트를 포함할 수 없습니다")
        String name,
    @NotNull UserRole role) {

  /** 이메일 앞뒤 공백은 검증 전에 걷어낸다 — @Email 이 공백이 낀 주소를 먼저 거부하지 않게 한다. */
  public SignUpRequest {
    email = email == null ? null : email.trim();
  }
}
