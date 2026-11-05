package com.example.ticket_booking.service;

import com.example.ticket_booking.repository.UserRepository;
import java.util.List;
import java.util.random.RandomGenerator;
import org.springframework.stereotype.Component;

/**
 * 티켓온 아이디를 만든다 — 단어 목록의 단어 하나 + 숫자 4자리(0 채움). 아이디는 일행에게 공유하는 값이라 이메일·이름을 쓰지 않는다
 * (booking-ux-01-auth.md §3-2). 이미 있는 아이디가 뽑히면 다시 뽑고, {@link #MAX_ATTEMPTS} 번 모두 겹치면 실패한다.
 *
 * <p>한 번 뽑기는 {@code nextInt(WORDS.size())} 다음 {@code nextInt(10000)} 이다 — 테스트가 이 순서로 난수원을 고정한다.
 */
@Component
public class HandleGenerator {

  /** V5__add_users_handle.sql 의 단어 목록과 같아야 한다. */
  static final List<String> WORDS =
      List.of(
          "wave", "star", "stage", "encore", "aria", "beat", "chord", "drum", "echo", "flute",
          "harp", "jazz", "lyric", "melody", "note", "opera", "piano", "rhythm", "solo", "tempo",
          "tune", "verse", "viola", "waltz", "cello", "choir", "disco", "folk", "hymn", "oboe",
          "lute", "gig");

  static final int MAX_ATTEMPTS = 10;
  private static final int NUMBER_BOUND = 10_000;

  private final UserRepository userRepository;
  private final RandomGenerator random;

  public HandleGenerator(UserRepository userRepository, RandomGenerator random) {
    this.userRepository = userRepository;
    this.random = random;
  }

  public String generate() {
    for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
      String word = WORDS.get(random.nextInt(WORDS.size()));
      String candidate = word + String.format("%04d", random.nextInt(NUMBER_BOUND));
      if (!userRepository.existsByHandle(candidate)) {
        return candidate;
      }
    }
    throw new HandleGenerationFailedException();
  }
}
