package com.example.ticket_booking.support;

import com.example.ticket_booking.service.MailGateway;
import com.example.ticket_booking.service.MailMessage;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 보낸 메일을 순서대로 모으는 테스트용 MailGateway(TASK-027). {@link #failWith} 를 두면 보내기 시도는 기록하되 그 예외를 던진다 —
 * 시도({@link #attempts})와 성공({@link #sent})을 나눠 센다.
 */
public class CapturingMailGateway implements MailGateway {

  private final List<MailMessage> attempts = new CopyOnWriteArrayList<>();
  private final List<MailMessage> sent = new CopyOnWriteArrayList<>();
  private volatile RuntimeException failWith;

  @Override
  public void send(MailMessage message) {
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

  public void reset() {
    attempts.clear();
    sent.clear();
    failWith = null;
  }
}
