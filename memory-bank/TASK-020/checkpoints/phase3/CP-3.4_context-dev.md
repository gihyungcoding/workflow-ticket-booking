---
status: ACTIVE
saved_at: 2026-11-16T00:00:00Z
summary: "Green — 전체 1029 통과·0 실패(시나리오 39건 + 자체 리뷰 추가 16), 린트 error 0, 자체 리뷰 2라운드(결함 3건 수정, 1건 미해결 기록)"

next_steps:
  - "Phase 4 — wf-verify. 시안 대조(375px)는 Phase 3 에서 했다: 상세·시트 일치, 타임라인 연결선·줄바꿈·여백 수정. 시각 값(한국 시간)은 이 브라우저가 시간대를 무시해 Node 테스트로만 확인했다 — 사람에게 알린다"
  - "Phase 4 리뷰어에 pre_review.root_causes·unresolved(시트 history 첫 칸 no-op)·deferred(첫 묶음 라벨 폭)를 넘긴다. 승인받지 않은 추가 테스트 16케이스(task020.text·sheet-close)와 승인 테스트 수정 5곳(clock 3·task024 2)을 보여준다"

decisions:
  - decision: "구현 순서 — 순수 함수(datetime·zones·time API·parseDetail) → useServerClock → AppBar·Sheet → ZoneMap·시트·대기열 → 상세 → App 레이아웃"
    rationale: "스켈레톤 8개를 아래에서 위로 채워 한 번에 하나의 테스트 묶음을 통과시켰다"
  - decision: "상세 시계는 UPCOMING 일 때만 켜고, openAt 에 닿으면 opened 를 기억해 시계를 끈다"
    rationale: "전환 뒤 재측정·타이머 불필요(SC-14 g), 한 방향 전환(ADR-0026)"
  - decision: "0초 전환은 1초 interval 이 아니라 deadline 에 맞춘 setTimeout(2^31-1ms 분할)"
    rationale: "interval 에 기대면 기기마다 최대 1초 어긋난다(PLAN unresolved)"
  - decision: "탭 복귀·측정 성공 때 오프셋이 같아도 bump 로 다시 그린다"
    rationale: "SC-14 e·f — 1초 주기를 기다리지 않는다. 구현 결함을 테스트가 잡았다"
  - decision: "리팩토링은 최소 — 좌표 상수화·zones 헬퍼 공유 외에는 하지 않았다"
    rationale: "화면 한 장·부품 신규라 중복이 적다"
  - decision: "승인된 테스트 5곳 수정(AskUserQuestion 승인) — clock 하네스 3(SC-11·SC-30 e·SC-14 i), task024 2(SC-16 b·SC-17 a)"
    rationale: "task024 2곳은 시나리오의 승인 테스트 변경 표에서 빠졌던 행이다(상세 헤더 제거의 영향) — Phase 4 에 알린다"
  - decision: "CtaBar 는 고치지 않았다"
    rationale: "시안 대조에서 흰 바탕 CTA 뒤 회색 띠가 보이지 않았다(PLAN 조건부 target)"
  - decision: "시안 대조용 모의 서버·Vite 는 PID 로 종료했고, 5173 의 다른 프로세스(PID 44592)는 건드리지 않았다"
    rationale: "자동 권한 분류기·다른 프로세스 보호(screen-verification 지침)"

attempts: []
---
