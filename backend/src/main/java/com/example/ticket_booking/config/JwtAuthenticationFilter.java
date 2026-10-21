package com.example.ticket_booking.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Authorization: Bearer 토큰을 검증해 인증 정보를 SecurityContext 에 넣는다. 인증에 실패해도 요청을 막지 않는다 — 인증이 필요한 경로는 보안
 * 체인의 EntryPoint 가 401 로 끊고, permitAll 경로(공연 API 등)는 만료·위조 토큰이 실려 와도 그대로 통과한다.
 *
 * <p>빈으로 등록하지 않는다 — 서블릿 필터 빈은 Boot 가 보안 체인과 별도로 한 번 더 등록한다. SecurityConfig 가 체인에 직접 끼운다.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  /** 스킴은 대소문자를 구분하지 않고(RFC 7235), 스킴과 토큰 사이는 공백 한 개이며, 토큰 뒤에 다른 값이 붙을 수 없다. */
  private static final Pattern BEARER =
      Pattern.compile("^Bearer ([A-Za-z0-9\\-._~+/]+=*)$", Pattern.CASE_INSENSITIVE);

  private final JwtTokenProvider jwtTokenProvider;

  public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider) {
    this.jwtTokenProvider = jwtTokenProvider;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    String header = request.getHeader("Authorization");
    if (header != null) {
      Matcher matcher = BEARER.matcher(header);
      if (matcher.matches()) {
        jwtTokenProvider
            .verifyAccessToken(matcher.group(1))
            .ifPresent(
                userId ->
                    SecurityContextHolder.getContext()
                        .setAuthentication(
                            new UsernamePasswordAuthenticationToken(userId, null, List.of())));
      }
    }
    chain.doFilter(request, response);
  }
}
