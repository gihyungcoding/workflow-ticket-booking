package com.example.ticket_booking.api;

import com.example.ticket_booking.api.dto.LoginRequest;
import com.example.ticket_booking.api.dto.SignUpRequest;
import com.example.ticket_booking.service.AuthService;
import com.example.ticket_booking.service.LoginResponse;
import com.example.ticket_booking.service.UserResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

  private final AuthService authService;

  public AuthController(AuthService authService) {
    this.authService = authService;
  }

  @PostMapping("/signup")
  public ResponseEntity<UserResponse> signUp(@Valid @RequestBody SignUpRequest request) {
    UserResponse response =
        authService.signUp(
            request.email(),
            request.password(),
            request.passwordConfirm(),
            request.name(),
            request.role());
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @PostMapping("/login")
  public LoginResponse login(@Valid @RequestBody LoginRequest request) {
    return authService.login(request.email(), request.password());
  }

  /** 보안 체인이 토큰을 검증해 principal(사용자 id)을 넣어 둔다 — 인증 없이는 여기까지 오지 않는다. */
  @GetMapping("/me")
  public UserResponse me(Authentication authentication) {
    return authService.getMe((Long) authentication.getPrincipal());
  }
}
