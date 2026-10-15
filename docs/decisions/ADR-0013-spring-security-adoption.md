# ADR-0013: Spring Security를 전체 도입한다 (OAuth2 Client + 커스텀 JWT 인증 필터)

- **상태**: 채택됨
- **날짜**: 2026-10-14
- **관련**: ADR-0012, `docs/product/features/user-authentication.md`

---

## 맥락

이메일 회원가입/로그인만 지원한다면 `Authorization` 헤더를 읽어 JWT를 검증하는
가벼운 커스텀 필터 하나로 충분했다. 그러나 이 기능은 카카오·네이버·구글·애플
4개 제공자의 OAuth2 Authorization Code Flow를 함께 지원해야 한다
([user-authentication.md](../product/features/user-authentication.md)).

각 제공자는 인가 URL 생성, `state` 파라미터를 통한 CSRF 방지, 콜백에서의 토큰
교환, 사용자 정보 조회라는 같은 모양의 흐름을 가지지만 세부 스펙(응답 구조,
클라이언트 시크릿 발급 방식)이 다르다. 특히 애플은 Client Secret 자체를 ES256
서명 JWT로 직접 생성해야 하고 콜백이 `form_post` 방식이라 다른 세 곳과 다르게
동작한다.

## 검토한 선택지

### A. 커스텀 구현 (OAuth2 흐름을 직접 작성)

- 장점: 의존성이 가볍고, 필요한 부분만 구현해 동작을 완전히 통제할 수 있다
- 단점: 제공자 4개 × (인가 URL 생성, `state` 검증, 토큰 교환, 사용자 정보 조회)를
  각각 구현·유지보수해야 한다. `state`/CSRF 검증을 놓치기 쉽고, 애플처럼 까다로운
  제공자(ES256 서명, `form_post`)는 구현 실수가 곧 보안 취약점으로 이어진다

### B. Spring Security (OAuth2 Client + 커스텀 JWT 인증 필터)

- 장점: `ClientRegistration` 설정만으로 인가 URL 생성, `state` 검증, 토큰 교환까지
  표준으로 처리된다. 제공자별 커스터마이징은 `CustomOAuth2UserService` 하나만
  작성하면 돼 반복 구현이 줄어든다
- 단점: 필터 체인 전체가 모든 요청 경로에 끼어든다 — 기존에 인증 없이 열려 있던
  공연 API(`/api/performances/**`)를 명시적으로 `permitAll` 처리하지 않으면
  전부 막혀버린다(회귀 위험). 프레임워크 학습 비용과 설정 복잡도가 생긴다

## 결정

Spring Security를 전체 도입한다 — OAuth2 Client로 소셜 로그인 흐름을 처리하고,
그 위에 커스텀 JWT 인증 필터(`JwtAuthenticationFilter`)를 필터 체인에 등록해
Bearer 토큰 검증을 맡긴다.

### 이유

제공자 4개의 OAuth2 흐름을 직접 구현하면 보안 버그(특히 `state` 미검증으로 인한
CSRF, 애플의 서명 처리 실수)가 날 가능성이 구현·유지보수 비용보다 더 큰 리스크
라고 판단했다. 커스텀 구현(A)은 제공자가 1~2개일 때는 고려할 만했지만, 4개로
늘어나며 반복 비용이 Spring Security 학습 비용을 넘어섰다.

## 결과

- 좋아지는 것: 제공자별 OAuth2 흐름(`state` 검증, 토큰 교환)을 직접 구현하지
  않아도 되고, 새 제공자를 추가할 때도 `ClientRegistration` 설정과 속성 매핑만
  추가하면 된다
- 감수하는 것: 모든 API가 기본적으로 Security 필터 체인을 통과하게 되어, 기존
  공연 API를 `permitAll`에 등록하는 걸 잊으면 조용히 막혀버리는 회귀가 생길 수
  있다(→ `user-authentication.md` Task A 완료 조건에 회귀 테스트 포함). 설정
  파일(`SecurityConfig`)이 프로젝트 전체 엔드포인트 접근 정책을 통제하는 단일
  지점이 된다
- 되돌리려면: `SecurityConfig`와 OAuth2 관련 설정을 제거하고 커스텀 필터로
  되돌려야 한다 — 소셜 로그인 4종을 유지한 채 되돌리려면 그 흐름을 전부
  재구현해야 해 비용이 높다

## 검사 가능한 제약

- 없음 (새 컨트롤러 추가 시 `SecurityConfig`의 접근 정책 반영을 빠뜨리는 실수는
  정적 검사보다 코드 리뷰로 잡는 편이 맞다고 판단했다. 패턴이 반복되면
  `architecture-doc` 스킬로 재검토한다)
