package com.example.ticket_booking.config;

import com.example.ticket_booking.api.dto.ErrorResponse;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * 접근 정책의 단일 지점(ADR-0013). 공연 API 는 permitAll 로 유지한다 — 소유권 검사는 이 태스크의 범위 밖이고 쓰기 가드(ADR-0017)가 그 사이의
 * 방어다. CORS 는 서블릿 필터 수준(CorsFilter, 최우선)이라 이 체인보다 먼저 처리된다(ADR-0016).
 */
@Configuration
public class SecurityConfig {

  private static final ObjectMapper OBJECT_MAPPER = JsonMapper.builder().build();

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  /** JWT_SECRET 이 비었거나 짧으면 여기서 기동이 실패한다. */
  @Bean
  public JwtTokenProvider jwtTokenProvider(@Value("${JWT_SECRET:}") String secret, Clock clock) {
    return JwtTokenProvider.fromEnv(secret, clock);
  }

  @Bean
  public SecurityFilterChain securityFilterChain(
      HttpSecurity http, JwtTokenProvider jwtTokenProvider) throws Exception {
    http.csrf(csrf -> csrf.disable())
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .formLogin(form -> form.disable())
        .httpBasic(basic -> basic.disable())
        .logout(logout -> logout.disable())
        .authorizeHttpRequests(
            authorize ->
                authorize
                    .requestMatchers("/api/performances/**", "/api/auth/signup", "/api/auth/login")
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .exceptionHandling(handling -> handling.authenticationEntryPoint(unauthorizedEntryPoint()))
        .addFilterBefore(
            new JwtAuthenticationFilter(jwtTokenProvider),
            UsernamePasswordAuthenticationFilter.class);
    return http.build();
  }

  /** 인증이 필요한 경로에 유효한 토큰 없이 들어오면 기존 ErrorResponse 형식으로 401 을 돌려준다. */
  private static AuthenticationEntryPoint unauthorizedEntryPoint() {
    return (request, response, exception) -> {
      response.setStatus(HttpStatus.UNAUTHORIZED.value());
      response.setContentType("application/json");
      response.setCharacterEncoding(StandardCharsets.UTF_8.name());
      response
          .getWriter()
          .write(
              OBJECT_MAPPER.writeValueAsString(
                  new ErrorResponse("UNAUTHORIZED", "인증이 필요합니다. 토큰이 없거나 올바르지 않습니다")));
    };
  }
}
