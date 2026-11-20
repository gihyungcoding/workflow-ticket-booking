package com.example.ticket_booking.support;

import com.example.ticket_booking.service.MailGateway;
import com.example.ticket_booking.service.MailMessage;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * 보낸 메일을 순서대로 모으는 테스트용 MailGateway(TASK-027). {@link #failWith} 를 두면 보내기 시도는 기록하되 그 예외를 던진다 —
 * 시도({@link #attempts})와 성공({@link #sent})을 나눠 센다. {@link #block()}·{@link #delay(long)} 로 발송 스레드를
 * 막거나 늦춘다.
 */
public class CapturingMailGateway implements MailGateway {

  private final List<MailMessage> attempts = new CopyOnWriteArrayList<>();
  private final List<MailMessage> sent = new CopyOnWriteArrayList<>();
  private volatile RuntimeException failWith;
  private volatile CountDownLatch gate;
  private volatile long delayMillis;

  @Override
  public void send(MailMessage message) {
    CountDownLatch blocking = gate;
    if (blocking != null) {
      await(blocking);
    }
    if (delayMillis > 0) {
      sleep(delayMillis);
    }
    attempts.add(message);
    RuntimeException failure = failWith;
    if (failure != null) {
      throw failure;
    }
    sent.add(message);
  }

  public List<MailMessage> attempts() {
    return List.copyOf(attempts);
  }

  public List<MailMessage> sent() {
    return List.copyOf(sent);
  }

  /** 받는 사람이 {@code to} 인 보낸 메일. */
  public List<MailMessage> sentTo(String to) {
    return sent.stream().filter(message -> to.equals(message.to())).toList();
  }

  public void failWith(RuntimeException failure) {
    this.failWith = failure;
  }

  /** 이후 send 가 {@link #release()} 될 때까지 멈춘다(발송 스레드가 막힌 상태 — TASK-027 SC-23). */
  public void block() {
    gate = new CountDownLatch(1);
  }

  public void release() {
    CountDownLatch blocking = gate;
    gate = null;
    if (blocking != null) {
      blocking.countDown();
    }
  }

  /** send 한 번에 걸리는 시간(TASK-027 SC-24). */
  public void delay(long millis) {
    this.delayMillis = millis;
  }

  public void reset() {
    release();
    attempts.clear();
    sent.clear();
    failWith = null;
    delayMillis = 0;
  }

  private static void await(CountDownLatch latch) {
    try {
      latch.await(60, TimeUnit.SECONDS);
    } catch (InterruptedException interrupted) {
      Thread.currentThread().interrupt();
    }
  }

  private static void sleep(long millis) {
    try {
      Thread.sleep(millis);
    } catch (InterruptedException interrupted) {
      Thread.currentThread().interrupt();
    }
  }
}
