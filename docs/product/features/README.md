# 기능 요구사항 문서

마지막 재생성: 2026-11-12 · 총 7건

> 이 파일은 `scripts/rebuild_doc_index.py` 가 생성합니다. 직접 편집하지 마세요 — 다음 재생성 때 사라집니다.

새 기능은 `/wf-feature <이름>`. 문서의 `## 1-2. 태스크 분리` 표를 채운 뒤 `/wf-tasks-from-doc <경로>` 로 태스크를 만듭니다.

| 문서 | 제목 | 상태 | 태스크 | 갱신 |
|---|---|---|---|---|
| [booking-ux-01-auth.md](booking-ux-01-auth.md) | [관객] 흐름 1 — 로그인·가입을 시안대로 (booking-ux-01) | 확정 | TASK-016, TASK-017, TASK-018 | 2026-11-04 (아이디 생성 규칙 변경) |
| [booking-ux-02-browse.md](booking-ux-02-browse.md) | [관객] 흐름 2 — 공연 찾기와 오픈 전 대기를 시안대로 (booking-ux-02) | 확정 | TASK-019, TASK-020, TASK-021 | 2026-11-10 |
| [email-verification-password-reset.md](email-verification-password-reset.md) | [관객/주최자] 이메일 인증과 비밀번호 찾기 | 초안 | 5건 (미생성) | 2026-11-12 |
| [organizer-flow.md](organizer-flow.md) | [주최자] 주최자 흐름 — 진입·내 공연 관리·공개 전환 | 초안 | TASK-022, TASK-023, TASK-024, TASK-025, TASK-026 | 2026-11-11 |
| [performance-availability.md](performance-availability.md) | [관객] 공연 목록과 예매 상태 | 확정 | TASK-001, TASK-002 | 2026-09-02 |
| [performance-registration.md](performance-registration.md) | [주최자] 공연 등록 | 초안 | TASK-004, TASK-005 | 2026-09-11 |
| [user-authentication.md](user-authentication.md) | [관객/주최자] 회원가입과 로그인 | 초안 | TASK-009, TASK-010, TASK-011, TASK-012, TASK-013 | 2026-10-14 |

## 태스크 미생성

아래 문서는 태스크 분리 표가 있지만 아직 태스크가 만들어지지 않았습니다.

- `email-verification-password-reset.md` — `/wf-tasks-from-doc docs/product/features/email-verification-password-reset.md`
