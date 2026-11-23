package com.example.ticket_booking.repository;

import com.example.ticket_booking.domain.User;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {

  Optional<User> findByEmail(String email);

  boolean existsByEmail(String email);

  boolean existsByHandle(String handle);

  /**
   * 계정 메일 간격 제한(TASK-027) — 마지막 발송이 없거나 {@code threshold} 보다 앞이면 발송 시각을 {@code now} 로 바꾸고 1 을, 아니면
   * 0 을 돌려준다. 판정과 갱신이 한 문장이라 동시 요청에도 한 쪽만 1 이다. 엔티티에 이 열을 매핑하지 않는다 — 불러 둔 User 를 저장할 때 옛 값으로 덮어쓰지
   * 않게 한다.
   */
  @Modifying(flushAutomatically = true)
  @Query(
      value =
          "UPDATE users SET account_mail_sent_at = :now WHERE id = :id"
              + " AND (account_mail_sent_at IS NULL OR account_mail_sent_at < :threshold)",
      nativeQuery = true)
  int claimAccountMailSlot(
      @Param("id") Long id, @Param("now") Instant now, @Param("threshold") Instant threshold);
}
