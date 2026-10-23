package com.example.ticket_booking.support;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

/**
 * 제공자가 돌려준 사용자 정보(attributes)를 그대로 담는 테스트용 OAuth2User. DefaultOAuth2User 는 이름 속성이 attributes 에 없으면
 * 생성부터 막아, 필수 값이 빠진 응답을 만들 수 없다.
 */
public class FakeOAuth2User implements OAuth2User {

  private final Map<String, Object> attributes;

  public FakeOAuth2User(Map<String, Object> attributes) {
    this.attributes = attributes;
  }

  @Override
  public Map<String, Object> getAttributes() {
    return attributes;
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return List.of();
  }

  @Override
  public String getName() {
    return "fake";
  }
}
