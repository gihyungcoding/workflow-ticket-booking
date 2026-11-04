package com.example.ticket_booking.support;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.random.RandomGenerator;

/**
 * 아이디 생성의 난수원을 시나리오의 「뽑힌 값」(단어 번호, 숫자) 순서로 고정한다 (TASK-016).
 *
 * <p>하네스 계약: 아이디 한 번 뽑기는 {@code nextInt(32)} 로 단어 번호, 이어서 {@code nextInt(10000)} 으로 숫자를 얻는다. 다른 범위로
 * 부르거나 준비한 값보다 많이 부르면 바로 실패시킨다 — 계약을 벗어난 구현이 우연히 통과하지 않게 하기 위해서다.
 */
public class SequenceRandom implements RandomGenerator {

  static final int WORD_BOUND = 32;
  static final int NUMBER_BOUND = 10_000;

  private final Deque<int[]> queue = new ArrayDeque<>();

  /** 뽑힌 값 (단어 번호, 숫자) 한 쌍을 순서대로 쌓는다. */
  public synchronized void enqueue(int word, int number) {
    queue.addLast(new int[] {WORD_BOUND, word});
    queue.addLast(new int[] {NUMBER_BOUND, number});
  }

  /** 같은 쌍을 times 번 쌓는다. */
  public synchronized void enqueue(int word, int number, int times) {
    for (int i = 0; i < times; i++) {
      enqueue(word, number);
    }
  }

  public synchronized void clear() {
    queue.clear();
  }

  @Override
  public synchronized int nextInt(int bound) {
    int[] next = queue.pollFirst();
    if (next == null) {
      throw new IllegalStateException("준비한 뽑힌 값이 더 없다 (bound=" + bound + ")");
    }
    if (next[0] != bound) {
      throw new IllegalStateException(
          "하네스 계약 위반 — nextInt(" + next[0] + ") 차례에 nextInt(" + bound + ")");
    }
    return next[1];
  }

  @Override
  public long nextLong() {
    throw new IllegalStateException("하네스 계약 위반 — 아이디 생성은 nextInt(bound) 만 쓴다");
  }
}
