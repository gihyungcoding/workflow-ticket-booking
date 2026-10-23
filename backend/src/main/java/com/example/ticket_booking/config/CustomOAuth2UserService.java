package com.example.ticket_booking.config;

import com.example.ticket_booking.service.SocialProfile;
import java.util.Map;

/** Red 단계 컴파일용 스켈레톤 — 제공자별 attributes 정규화는 Phase 3 에서 구현한다. */
public class CustomOAuth2UserService {

  /**
   * registrationId(kakao·naver·google)와 제공자 attributes 로 정규화된 프로필을 만든다. 이상하면
   * OAuth2AuthenticationException.
   */
  public static SocialProfile toProfile(String registrationId, Map<String, Object> attributes) {
    throw new UnsupportedOperationException();
  }
}
