package com.example.ticket_booking.repository;

import com.example.ticket_booking.domain.EmailToken;
import com.example.ticket_booking.domain.EmailTokenPurpose;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EmailTokenRepository extends JpaRepository<EmailToken, Long> {

  /** 같은 사용자·용도의 토큰을 모두 지운다 — 새 토큰을 발급하기 전에 부른다(이전 링크 무효). */
  @Modifying(flushAutomatically = true, clearAutomatically = true)
  @Query("DELETE FROM EmailToken t WHERE t.userId = :userId AND t.purpose = :purpose")
  int deleteByUserIdAndPurpose(
      @Param("userId") Long userId, @Param("purpose") EmailTokenPurpose purpose);

  /**
   * 유효한 토큰이면 사용 시각을 채운다 — 바뀐 행 수(0 또는 1). 판정과 갱신이 한 문장이라 동시에 같은 토큰을 써도 한 쪽만 1 이다(나중 쪽은 앞 쪽의 행 잠금이
   * 풀린 뒤 used_at 을 다시 보고 0 이 된다). 만료 시각 정각은 만료다(expires_at > now).
   */
  @Modifying(flushAutomatically = true, clearAutomatically = true)
  @Query(
      "UPDATE EmailToken t SET t.usedAt = :now WHERE t.tokenHash = :hash AND t.purpose = :purpose"
          + " AND t.usedAt IS NULL AND t.expiresAt > :now")
  int markUsed(
      @Param("hash") String tokenHash,
      @Param("purpose") EmailTokenPurpose purpose,
      @Param("now") Instant now);

  @Query("SELECT t.userId FROM EmailToken t WHERE t.tokenHash = :hash")
  Optional<Long> findUserIdByTokenHash(@Param("hash") String tokenHash);
}
