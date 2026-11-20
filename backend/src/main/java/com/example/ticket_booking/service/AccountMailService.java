package com.example.ticket_booking.service;

import com.example.ticket_booking.domain.EmailTokenPurpose;
import com.example.ticket_booking.domain.User;
import com.example.ticket_booking.repository.UserRepository;
import jakarta.annotation.PreDestroy;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 계정으로 나가는 메일의 발송(ADR-0031). 인증·재설정·소셜 안내는 같은 사용자에게 60초에 한 통만 보낸다 — 마지막 발송 시각을 조건부 UPDATE 한 문장으로
 * 판정·갱신해 동시 요청에도 한 통이다(요구사항 §3-3). 결과 true 는 보냄, false 는 건너뜀(예외 아님). 변경 알림은 유효한 토큰으로만 일어나므로 간격 없이
 * 보내고 발송 시각을 바꾸지 않는다.
 *
 * <p>메일은 커밋 뒤에 전용 스레드 하나에서 보낸다 — 롤백이면 보내지 않고, 응답과 응답 시간이 메일 서비스 상태와 무관하다. 대기열은 100통이고 넘치면 버린다. 종료
 * 때는 대기 중인 메일을 최대 10초 보낸다. 발송 실패·버림은 WARN 로그만 남긴다(받는 사람은 가리고 토큰·본문은 남기지 않는다).
 */
@Service
public class AccountMailService {

  private static final Logger log = LoggerFactory.getLogger(AccountMailService.class);
  private static final Duration INTERVAL = Duration.ofSeconds(60);

  private final UserRepository users;
  private final EmailTokenService tokens;
  private final AccountMails mails;
  private final MailGateway gateway;
  private final Clock clock;

  /** 발송 스레드 1개, 대기열 상한 — 메일 서비스가 멈춰도 메모리가 끝없이 늘지 않게(자체 리뷰 RC-2). */
  static final int QUEUE_CAPACITY = 100;

  /** 종료 때 대기 중인 메일을 보내며 기다리는 최대 시간. */
  static final Duration SHUTDOWN_WAIT = Duration.ofSeconds(10);

  private final ThreadPoolExecutor sender =
      new ThreadPoolExecutor(
          1,
          1,
          0L,
          TimeUnit.MILLISECONDS,
          new ArrayBlockingQueue<>(QUEUE_CAPACITY),
          runnable -> {
            Thread thread = new Thread(runnable, "account-mail");
            thread.setDaemon(true);
            return thread;
          },
          new ThreadPoolExecutor.AbortPolicy());

  public AccountMailService(
      UserRepository users,
      EmailTokenService tokens,
      AccountMails mails,
      MailGateway gateway,
      Clock clock) {
    this.users = users;
    this.tokens = tokens;
    this.mails = mails;
    this.gateway = gateway;
    this.clock = clock;
  }

  /** 간격 안이 아니면 인증 토큰(이전 것 삭제)을 발급해 인증 메일을 보낸다. */
  @Transactional
  public boolean sendVerificationMail(Long userId) {
    return claim(userId)
        .map(
            user -> {
              String raw = tokens.issue(user.getId(), EmailTokenPurpose.VERIFY_EMAIL);
              dispatchAfterCommit(mails.verification(user.getEmail(), raw));
              return true;
            })
        .orElse(false);
  }

  /** 간격 안이 아니면 재설정 토큰(이전 것 삭제)을 발급해 재설정 메일을 보낸다. */
  @Transactional
  public boolean sendPasswordResetMail(Long userId) {
    return claim(userId)
        .map(
            user -> {
              String raw = tokens.issue(user.getId(), EmailTokenPurpose.RESET_PASSWORD);
              dispatchAfterCommit(mails.passwordReset(user.getEmail(), raw));
              return true;
            })
        .orElse(false);
  }

  /** 간격 안이 아니면 소셜 계정 안내 메일을 보낸다(토큰 없음). */
  @Transactional
  public boolean sendSocialAccountNotice(Long userId) {
    return claim(userId)
        .map(
            user -> {
              dispatchAfterCommit(mails.socialAccountNotice(user.getEmail()));
              return true;
            })
        .orElse(false);
  }

  /** 비밀번호 변경 알림 — 간격 없이 보내고 마지막 발송 시각을 바꾸지 않는다. 없는 사용자면 아무것도 하지 않는다. */
  @Transactional
  public void sendPasswordChangedNotice(Long userId) {
    requireUserId(userId);
    users
        .findById(userId)
        .ifPresent(user -> dispatchAfterCommit(mails.passwordChanged(user.getEmail())));
  }

  /** 간격 제한 — 이번 발송 몫을 얻으면 그 사용자, 아니면(간격 안·없는 사용자·0 이하 id) 빈 결과. */
  private Optional<User> claim(Long userId) {
    requireUserId(userId);
    if (userId <= 0) {
      return Optional.empty();
    }
    Instant now = clock.instant();
    if (users.claimAccountMailSlot(userId, now, now.minus(INTERVAL)) != 1) {
      return Optional.empty();
    }
    return users.findById(userId);
  }

  private static void requireUserId(Long userId) {
    if (userId == null) {
      throw new IllegalArgumentException("userId 가 필요하다");
    }
  }

  private void dispatchAfterCommit(MailMessage message) {
    if (TransactionSynchronizationManager.isSynchronizationActive()) {
      TransactionSynchronizationManager.registerSynchronization(
          new TransactionSynchronization() {
            @Override
            public void afterCommit() {
              enqueue(message);
            }
          });
    } else {
      enqueue(message);
    }
  }

  /**
   * 발송 스레드에 맡긴다. 대기열이 가득 찼거나 종료 중이면 그 메일은 버리고 WARN 만 남긴다 — afterCommit 의 예외는 이미 커밋된 요청의 호출자에게 전해지므로
   * 던지지 않는다(RC-2).
   */
  private void enqueue(MailMessage message) {
    try {
      sender.execute(() -> send(message));
    } catch (RejectedExecutionException rejected) {
      log.warn("계정 메일 발송 건너뜀(대기열 가득 참 또는 종료 중) to={}", mask(message.to()));
    }
  }

  private void send(MailMessage message) {
    try {
      gateway.send(message);
    } catch (RuntimeException exception) {
      log.warn(
          "계정 메일 발송 실패 to={} cause={}", mask(message.to()), exception.getClass().getSimpleName());
    }
  }

  /** 받는 사람 가림 — 로컬 파트 첫 1글자 + "***" + "@" + 도메인. '@' 가 없으면 "***". */
  static String mask(String address) {
    if (address == null) {
      return "***";
    }
    int at = address.lastIndexOf('@');
    if (at <= 0) {
      return "***";
    }
    return address.substring(0, address.offsetByCodePoints(0, 1)) + "***" + address.substring(at);
  }

  /** 컨텍스트 닫기 — 새 메일은 받지 않고 대기 중인 메일을 최대 {@link #SHUTDOWN_WAIT} 동안 보낸다(RC-2). */
  @PreDestroy
  void shutdown() throws InterruptedException {
    sender.shutdown();
    if (!sender.awaitTermination(SHUTDOWN_WAIT.toMillis(), TimeUnit.MILLISECONDS)) {
      log.warn("계정 메일 발송기 종료 대기 초과 — 남은 메일 {}통을 버린다", sender.getQueue().size());
      sender.shutdownNow();
    }
  }
}
