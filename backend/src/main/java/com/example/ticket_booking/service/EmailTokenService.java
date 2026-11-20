package com.example.ticket_booking.service;

import com.example.ticket_booking.domain.EmailToken;
import com.example.ticket_booking.domain.EmailTokenPurpose;
import com.example.ticket_booking.repository.EmailTokenRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 일회용 이메일 링크 토큰의 발급과 소모(ADR-0032). 원문은 32바이트 난수의 URL-safe Base64(패딩 없음, 43자)이고 돌려주기만 한다 — DB 에는
 * SHA-256 16진 소문자 해시만 있다. 만료는 용도별 TTL, 정각은 만료다. 소모는 조건부 UPDATE 한 문장이라 동시에 써도 한 번만 성공한다.
 */
@Service
public class EmailTokenService {

  private static final int RAW_BYTES = 32;
  private static final Pattern RAW_FORMAT = Pattern.compile("[A-Za-z0-9_-]{43}");

  private final EmailTokenRepository tokens;
  private final Clock clock;
  private final SecureRandom random = new SecureRandom();

  public EmailTokenService(EmailTokenRepository tokens, Clock clock) {
    this.tokens = tokens;
    this.clock = clock;
  }

  /**
   * 같은 사용자·용도의 이전 토큰을 지우고 새 토큰을 발급해 원문(43자)을 돌려준다. userId 가 null·0 이하이거나 purpose 가 null 이면 호출자 버그다
   * — DB 에 닿기 전에 IllegalArgumentException. 없는 사용자면 외래 키 위반(DataIntegrityViolationException)이다.
   *
   * <p><b>호출 규칙</b>: 같은 사용자·용도로 동시에 부르면 이전 토큰 삭제와 새 토큰 저장이 겹쳐 유효 토큰이 둘 남을 수 있다(수용한 위험 — PLAN, 자체 리뷰
   * RC-3). 메일과 함께 발급할 때는 {@link AccountMailService} 를 거친다 — 간격 제한 UPDATE 가 사용자 행을 먼저 잠가 직렬화한다. 직접
   * 부르는 호출자는 같은 트랜잭션에서 그 사용자 행을 먼저 잠근다.
   */
  @Transactional
  public String issue(Long userId, EmailTokenPurpose purpose) {
    if (userId == null || userId <= 0) {
      throw new IllegalArgumentException("userId 는 양수여야 한다: " + userId);
    }
    if (purpose == null) {
      throw new IllegalArgumentException("purpose 가 필요하다");
    }
    tokens.deleteByUserIdAndPurpose(userId, purpose);
    byte[] bytes = new byte[RAW_BYTES];
    random.nextBytes(bytes);
    String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    tokens.saveAndFlush(new EmailToken(userId, purpose, sha256Hex(raw), clock.instant()));
    return raw;
  }

  /**
   * 유효한 토큰이면 한 번만 소모하고 그 사용자 id 를, 아니면 빈 결과를 돌려준다 — 없음·만료·이미 사용·다른 용도·형식이 다른 문자열(공백·잘림·정규화 변형 포함)을
   * 구분하지 않는다. purpose 가 null 이면 호출자 버그다(IllegalArgumentException).
   */
  @Transactional
  public Optional<Long> consume(String rawToken, EmailTokenPurpose purpose) {
    if (purpose == null) {
      throw new IllegalArgumentException("purpose 가 필요하다");
    }
    if (rawToken == null || !RAW_FORMAT.matcher(rawToken).matches()) {
      return Optional.empty();
    }
    String hash = sha256Hex(rawToken);
    if (tokens.markUsed(hash, purpose, clock.instant()) != 1) {
      return Optional.empty();
    }
    return tokens.findUserIdByTokenHash(hash);
  }

  static String sha256Hex(String raw) {
    try {
      byte[] digest =
          MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(digest);
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 을 쓸 수 없다", exception);
    }
  }
}
