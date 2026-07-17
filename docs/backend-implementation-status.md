# 백엔드 저장 연동 현황 — "테이블 설계 완료" vs "실제 저장 연동"

`docs/db-schema-audit.md`/`dashboard-metrics-audit.md`/`game-system-audit.md`에서 결정한 내용을 16개 Entity 클래스(`src/main/java/com/victory/entity/`)로 만든 작업과, 그 테이블에 실제로 데이터가 쌓이도록 Repository/Service/Controller를 연결하고 프론트가 그 API를 호출하는 작업은 **완전히 다른 단계**입니다. 이 문서는 그 경계를 명확히 구분합니다.

**코드 수정 없음** — 현재 상태 확인 및 문서 정리만 했습니다.

---

## 0. 결론 먼저

| 영역 | 테이블 설계 | 실제 저장 연동(Repository/Service/Controller) | 프론트가 그 API를 호출하는가 |
|---|---|---|---|
| 연습읽기 진행 상태(`practice_progress`) | ✅ 완료 | ✅ **완료** — 유일하게 다 되어 있음 | ✅ 호출함(`during-reading-practice.html`, `after-read.html`) |
| 그 외 독서활동 전부(`reading_records`, `reading_progress_logs`, `responses`, `summaries`, `books`, `classes`, `class_students` 등 13개) | ✅ 완료 | ❌ **없음** | ❌ 해당 없음(호출할 API 자체가 없음) |
| 게임 결과(`dungeon_records`, `student_stats`) | ✅ 완료 | ❌ **없음** | ❌ `game/`의 `toSaveData()`는 여전히 어디서도 호출되지 않는 죽은 코드 |

**한 줄 요약**: 프로젝트 전체에서 "학생이 활동하면 실제로 DB에 저장되는" 테이블은 `practice_progress` **단 하나**뿐입니다. 나머지 15개 테이블은 이번 작업으로 "저장할 그릇"만 만들어진 상태이고, 그 그릇에 데이터를 담는 코드(Repository/Service/Controller)와 프론트가 그 코드를 호출하는 부분은 전부 다음 단계 작업입니다.

---

## 1. 학생 독서활동(연습읽기/개별읽기) 자동저장

### `practice_progress` — 유일하게 실제 연동됨

`src/main/java/com/victory/controller/PracticeProgressController.java`에 `GET/POST /api/students/{studentId}/practice-progress`가 실제로 구현되어 있고, `PracticeProgressService` → `PracticeProgressRepository` → DB까지 이어지는 전체 경로가 존재합니다. 프론트(`during-reading-practice.html`의 `saveProgress()`/`loadProgress()`, `after-read.html`의 `saveSummary()`)도 실제로 이 API를 `fetch()`로 호출합니다. **이 테이블만큼은 "설계-구현-연동"이 전부 끝난 상태입니다.**

### 나머지 독서활동 테이블 — 테이블 설계만 완료, 저장 로직 없음

`reading_records`, `reading_progress_logs`, `responses`, `summaries`, `books`, `classes`, `class_students`, `content_likes`, `book_recommendations` — 이번에 만든 Entity 클래스는 있지만, 이 9개 테이블 각각에 대응하는 **Repository/Service/Controller가 하나도 없습니다**(`ls src/main/java/com/victory/repository/`로 재확인: `PracticeProgressRepository`, `UserRepository` 딱 2개뿐).

즉 지금 이 9개 테이블은:
- 자바 클래스(Entity)는 존재 → DB에 실제 테이블을 만들면(마이그레이션 실행 시) 스키마 자체는 생성될 수 있음
- 하지만 그 테이블에 데이터를 넣고 빼는 API가 전혀 없음 → 프론트가 아무리 요청을 보내도 받아줄 곳이 없음
- 프론트 쪽도 여전히 예전 방식(`localStorage`/`sessionStorage`에 직접 저장) 그대로임 — 이번 작업으로 프론트 코드는 하나도 안 건드렸으므로 당연한 상태

`docs/dashboard-metrics-audit.md`/`db-schema-audit.md`에서 확인했던 "프론트가 백엔드로 안 보내고 로컬에만 저장한다"는 문제들(예: 읽기 전 질문/답이 전송 안 됨, 책수다방이 localStorage에만 있음, 나의 책 진행 상황이 세션에만 저장됨)은 **이번 작업으로 하나도 해결되지 않았습니다.** 테이블을 준비한 것과 그 문제를 실제로 고치는 것은 별개입니다.

---

## 2. 게임(던전) 결과 자동저장

### `dungeon_records` / `student_stats` — 테이블 설계만 완료, 게임 쪽 문제는 그대로

`game-system-audit.md`에서 확인한 핵심 문제 3가지를 다시 확인합니다:

1. `game/js/game-state.js`의 `toSaveData()`(백엔드 전송용으로 준비된 함수)는 **여전히 어디서도 호출되지 않는 죽은 코드**입니다 — 이번 작업에서 `game/` 디렉터리의 파일은 한 줄도 건드리지 않았으므로 당연히 그대로입니다.
2. `dungeon_records`/`student_stats`에 대응하는 Repository/Service/Controller도 아직 없습니다 — 설사 `toSaveData()`가 호출되도록 프론트를 고치더라도, 지금은 그 요청을 받아서 DB에 저장해 줄 백엔드 API 자체가 없는 상태입니다.
3. `GameAPI.getInitialPlayerState(studentId)`가 `studentId`를 무시하고 고정값을 반환하는 문제도 그대로입니다 — `GET /api/students/{studentId}/stats` API가 아직 없어서, 설사 프론트를 고치려 해도 호출할 대상이 없습니다.

**즉 game-system-audit.md에서 지적한 "결과 저장 함수가 호출되지 않는 죽은 코드" 문제는 이번 테이블 설계 작업으로 전혀 해결되지 않았습니다.** 테이블(그릇)만 준비됐을 뿐, 그 문제를 실제로 고치려면 여전히 3가지가 다 필요합니다: ① 백엔드 API(Repository/Service/Controller) 신규 작성, ② `game/js/*.js`의 하드코딩된 함수들을 그 API를 호출하도록 교체, ③ 던전 입장 조건의 "테스트용으로 무조건 통과" 코드 제거.

---

## 3. "테이블 설계" 작업과 "실제 저장 연동" 작업의 경계

| 이번 작업(완료) | 다음 단계 작업(남음) |
|---|---|
| Entity 클래스 16개 작성(`@Entity`, 컬럼, FK, 인덱스, UNIQUE 제약) | 각 테이블마다 Repository 인터페이스 작성 |
| 컬럼 타입/제약조건 설계 | 각 기능마다 Service(비즈니스 로직: 검증, 계산식 적용 등) 작성 |
| JPA 관계 매핑(`@ManyToOne`, `@OneToOne`, 자기참조 등) | 각 기능마다 Controller(REST API 엔드포인트) 작성 |
| — | 프론트 코드가 `localStorage`/`sessionStorage` 대신 그 API를 `fetch()`로 호출하도록 교체 |
| — | 실제 DB에 테이블을 만드는 마이그레이션 실행(`ddl-auto` 또는 별도 마이그레이션 스크립트) |

이번 작업은 표의 왼쪽 열까지만입니다. 오른쪽 열은 전부 아직 시작 전입니다.

---

## 4. 다음 단계에서 우선순위를 매긴다면 (참고용, 이번엔 실행 안 함)

`practice_progress`가 이미 끝난 것을 참고 삼아, 같은 패턴(Repository → Service → Controller → 프론트 fetch 교체)을 반복하면 되는 작업들입니다. 특히 이미 다른 감사 문서에서 "이것부터 연결하면 바로 효과가 크다"고 짚었던 것들:

- `responses`: 연습읽기 읽기 전 질문/답 저장(`db-schema-audit.md` §5-1에서 지적된, 지금 그냥 버려지는 데이터)부터 연결하면 여러 화면(질문 이해도 집계, AI 판정 결과 저장)이 동시에 풀림
- `reading_records` + `reading_progress_logs`: "나의 책 진행 상황"이 세션 저장만 되는 문제(`dashboard-metrics-audit.md` §9) 해결
- `student_stats` + `dungeon_records`: 게임 순환 구조(`game-system-audit.md` §10) 전체가 이 두 테이블 연동에 달려 있음

우선순위 결정과 실제 착수는 다음 단계에서 별도로 요청해주시면 됩니다.
