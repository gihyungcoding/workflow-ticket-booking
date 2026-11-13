package com.example.ticket_booking.repository;

import com.example.ticket_booking.domain.Performance;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PerformanceRepository
    extends JpaRepository<Performance, Long>, PerformanceRepositoryCustom {

  /**
   * 쓰기 경로용 조회 — 행을 잠근 채 읽는다. 같은 공연에 쓰기가 겹치면 먼저 온 쪽이 끝날 때까지 기다리고, 기다린 뒤의 최신 행으로 검사·저장한다. 잠금 없이 읽고 전
   * 컬럼을 저장하면 다른 요청의 변경(취소·공개·제목)을 되돌린다. 트랜잭션 안에서만 쓴다.
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT p FROM Performance p WHERE p.id = :id")
  Optional<Performance> findByIdForUpdate(@Param("id") Long id);
}
