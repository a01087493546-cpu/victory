# 대시보드 지표 산정 기준 감사 및 설계

교사/학생 대시보드에 이미 적혀 있는 평가 기준 문구("참여도", "이해도", "독서 실천도", "기록 완성도" 등)를 실제로 만족시키려면 어떤 데이터를 어떻게 조합해야 하는지 설계한 문서입니다. `docs/db-schema-final.md`/`docs/db-schema-audit.md`/`docs/api-endpoints.md`와 겹치는 내용은 참조만 하고, 이번 조사에서 새로 발견한 사실과 산출 방식 설계 위주로 작성했습니다.

**조사 방법**: 프론트 10개 화면(교사 2개 대시보드 + 학생 개별읽기/연습읽기 화면 다수)을 코드 레벨로 전수조사(약 25개 함수, 4000줄 이상 실제 읽음)해서 각 수치가 하드코딩인지 실제 계산식이 있는지 확인했습니다. 백엔드는 여전히 `FeedbackAiController` 1개만 존재(DB 미연동)임을 재확인했습니다.

**코드 수정 없음** — 이 문서는 분석/설계안만 담고 있습니다.

---

## 0. 전체 결론 먼저

10개 항목 중 **8개가 완전 하드코딩(정적 리터럴, 계산 로직 0건)**, 1개는 프론트 로컬 저장만 되고 백엔드 연동이 없는 상태, 1개(개별읽기 "읽은 책 종류")만 실제로 데이터를 세는 계산 로직이 존재합니다. 특히 교사용 두 대시보드(`book-manage.html` 온책읽기 / `individual-reading-manage.html` 개별읽기)는 **학생 이름·수치·문구까지 동일한 mock 데이터를 복사-붙여넣기**한 상태라는 것을 이번에 확인했습니다(예: 두 파일 모두 "김수현"이 "확인이 필요한 학생" 1번, 두 파일 모두 "76%"/"7명").

---

## 1. 교사 "읽을 범위 설정" → 학생 화면 연동

**현재 상태: 완전 단절 (연동 안 됨)**

- 교사 측 저장(`frontend/teacher/book-manage.html:3012-3013, 3219-3258`): `saveReadingRange()`가 `localStorage["wholeReadingRange"] = {range, savedAt}`로 저장. `fetch` 호출 없음, 브라우저 로컬 전용.
- 학생 측(`frontend/student/class-reading.html:601-604`): "현재 읽는 구간" 영역이 `<strong>현재 읽는 구간</strong>`라는 라벨=값 placeholder 텍스트 그대로 남아 있고, 이 요소에 `id`조차 없어 스크립트가 채울 훅 자체가 없음. `wholeReadingRange` 문자열은 프로젝트 전체에서 `book-manage.html` 단 한 곳에만 존재.
- 새로 발견한 사실: `docs/api-endpoints.md` §C에 이미 `GET/POST /api/classes/{classId}/reading-range`가 설계되어 있고(43-44행), `docs/db-schema-final.md`의 `books` 테이블에도 이미 `reading_range VARCHAR(200)` 컬럼이 있습니다(163-177행). 즉 **테이블/API 설계는 이미 준비돼 있고, 프론트가 그 설계를 아예 쓰지 않고 있는 상태**입니다(로컬 mock 구현을 하면서 실제 스펙을 참고하지 않은 것으로 보임).

**산출 방식 설계**
1. 교사: `saveReadingRange()`를 `POST /api/classes/{classId}/reading-range` 호출로 교체(또는 더 직접적으로 `PATCH /api/books/{bookId}` 로 해당 학급의 `source='class'` 도서 행의 `reading_range`를 갱신).
2. 학생: `class-reading.html` 진입 시 `GET /api/classes/{classId}/reading-range` (또는 학급 지정 도서 조회 API)를 호출해서 `#currentReadingRange`(id 부여 필요)에 실제 값을 채움.

**필요한 신규 테이블/컬럼**: 없음 (`books.reading_range` 기존 컬럼으로 충분).
**확인 필요**: "학급당 지정 도서가 항상 1권"이라는 전제가 맞는지(그래야 `class_id`만으로 어느 책의 `reading_range`인지 특정 가능) — `books.class_id` + `source='class'`가 유일하게 존재한다는 보장이 코드상 없음, 기획 확인 필요.
**AI 판단 필요**: 없음. **일 단위 스냅샷 필요**: 없음(현재값만 필요, 이력 불필요).

---

## 2. 연습읽기 "우리 반 진행 상황 68%"

**현재 상태: 하드코딩** (위치는 `practice.html`이 아니라 `frontend/student/class-reading.html:592-618`)

```html
<div class="progress-ring">68%</div>
<div class="progress-title">우리 반 68% 완료</div>
<div class="progress-caption">총 25명 참여 중</div>
```
`progress-ring`/`progress-title`/`progress-caption` 클래스는 파일 전체에서 CSS 정의 외에 JS로 다뤄지는 곳이 전혀 없음(계산 함수 0건). 같은 화면의 "다음 읽기 단계로 넘어가기" 버튼(`completeClassReading()`)은 실제로 `POST .../practice-progress {classReadDone:true}`를 호출하는 진짜 연동 코드인데, 바로 위의 진행률 표시는 그 결과를 전혀 반영하지 않는 별개의 정적 요소라는 점이 대비됩니다.

**산출 방식 설계**: 사용자가 제안한 "학급 전체 학생 수 대비 특정 활동 완료 학생 수" 방식이 기존 스키마로 바로 계산 가능합니다.
```sql
-- 진행률
ROUND(
  COUNT(pp.student_id) / (SELECT COUNT(*) FROM class_students WHERE class_id=?)
  * 100
)
FROM practice_progress pp
JOIN class_students cs ON cs.student_id = pp.student_id
WHERE cs.class_id = ? AND pp.class_read_done = TRUE
```
- 기준 활동: `practice_progress.class_read_done`(학급 읽기 완료 플래그, 이미 존재) — 이 화면 자체가 "학급 읽기"를 완료 처리하는 화면이므로 가장 자연스러운 기준.
- "총 25명 참여 중" = `COUNT(class_students WHERE class_id=?)`.

**필요한 신규 테이블/컬럼**: 없음(`practice_progress.class_read_done`, `class_students` 기존 컬럼으로 충분). 신규 API `GET /api/classes/{classId}/reading-progress`만 추가하면 됨.
**AI 판단 필요**: 없음. **일 단위 스냅샷 필요**: 없음(실시간 COUNT로 충분, 반 인원이 많지 않아 부하 문제 없음).

---

## 3, 9. 개별읽기 "나의 힘" / "월별 완독 기록" / "나의 책 진행 상황"

먼저 중요한 구조적 사실: 사용자가 언급한 대시보드 홈 `frontend/student/individual-reading.html`에는 **"나의 힘"과 "월별 완독 기록"만 있고**, "나의 책 진행 상황"(9번 질문)과 "읽은 책 종류"(10번 질문)는 이 파일에 없습니다. 이 둘은 "오늘의 독서 모험" 진입 후의 `frontend/student/individual-during-reading.html`에 구현되어 있습니다. 즉 학생이 실제로 활동하는 화면(during-reading)에는 진짜 계산 로직이 있는데, 그 결과를 요약해서 보여줘야 할 홈 대시보드(`individual-reading.html`)에는 전혀 연결이 안 되어 있는 상태입니다.

### 3-A. "나의 힘" — student_stats 연동 가능한 구조인가

**현재 상태: 하드코딩, 그러나 실제 시스템은 이미 존재함(단순 배선 누락)**

`individual-reading.html:868-904`는 마법력/체력/지혜/용기를 `11/11/11/10` 리터럴로 표시하고, `individual-power.js`의 `<script src>` 태그 자체가 이 파일에 없습니다(주석에만 언급, 코드 호출 0건).

반면 `frontend/js/individual-power.js`는 다른 10개 화면에서 실제로 쓰이는 완성된 시스템입니다(`db-schema-audit.md`에서 이미 `student_stats` 테이블과 1:1 대응 확인됨): `localStorage["individualPower_v2_<studentId>"] = {magic,stamina,wisdom,courage}`, `getIndividualPowerState()`/`openIndividualPowerModal()` 등 API가 이미 구현돼 있습니다.

**산출 방식 설계**: `individual-reading.html`에 `<script src="../js/individual-power.js">`를 추가하고, 홈 진입 시 `getIndividualPowerState()`(백엔드 연동 후에는 `GET /api/students/{studentId}/stats`, 이미 `docs/api-endpoints.md` §H에 설계됨)로 값을 받아 `.ir-power-row` 4줄을 렌더링하면 끝입니다. **`student_stats` 테이블은 이미 이 화면을 지원할 수 있는 구조이며, 순수 프론트 배선 문제**입니다.

**필요한 신규 테이블/컬럼**: 없음. **AI 판단**: 없음. **스냅샷**: 불필요(현재값 표시).

### 3-B. "월별 완독 기록" 12개월 막대그래프

**현재 상태: 완전 하드코딩** (`individual-reading.html:919-949`, 12개 `<div class="ir-bar">`의 높이/숫자가 전부 인라인 리터럴)

**산출 방식 설계**: `reading_records.finished_at`(이미 존재하는 컬럼) 기준 월별 카운트.
```sql
SELECT MONTH(finished_at) AS month, COUNT(*) AS count
FROM reading_records
WHERE student_id = ? AND finished_at IS NOT NULL AND YEAR(finished_at) = ?
GROUP BY MONTH(finished_at)
```
프론트는 1~12월을 순회하며 위 결과에 없는 달은 0으로 채워 12칸을 완성. 신규 API: `GET /api/students/{studentId}/monthly-completions?year=`.

**중요 발견**: 이 그래프가 의미 있으려면 학생이 책 1권을 끝낼 때마다 `reading_records` 행이 실제로 하나씩 쌓여야 하는데, 현재 프론트(`individual-during-reading.html`)의 진행 기록은 §9에서 확인하듯 **책 1권 분량을 세션 단일 값으로만 관리**하고 있어 "여러 권 완독"이 데이터로 남지 않습니다. 이는 `db-schema-audit.md` §5-5("개별읽기 보관함이 다권 누적을 지원하지 않는 문제")와 **동일한 근본 원인**입니다 — 각 항목이 별개 버그가 아니라 "책 완독 시 reading_records에 새 행을 insert하는 흐름 자체가 프론트에 아직 없다"는 하나의 원인에서 파생된 증상들입니다.

**필요한 신규 테이블/컬럼**: 없음(`reading_records.finished_at` 기존 컬럼으로 충분). **AI 판단**: 없음. **스냅샷**: 불필요(월별 GROUP BY로 매번 재계산 가능).

### 9. "나의 책 진행 상황" (진행률 %, 전체 쪽수, 오늘 읽은 쪽) — 신규 항목

**현재 상태: 저장은 되지만 (a) 백엔드 미연동 (b) 날짜별 이력 없이 매번 덮어쓰기**

위치: `frontend/student/individual-during-reading.html:7684-7719`(UI), `9522-9557`(`saveProgressData()`).

```js
sessionStorage.setItem(STORAGE_KEYS.totalPages, String(totalPages));
sessionStorage.setItem(STORAGE_KEYS.currentPage, String(currentPage));
```
- `fetch` 호출이 전혀 없음 — 순수 `sessionStorage`(브라우저 세션 한정, 탭/브라우저 종료 시 소멸)에 **단일 숫자 문자열**로 저장. "3일차 35쪽, 4일차 50쪽" 같은 날짜별 이력이 전혀 없고 매번 최신 값으로 덮어씀.
- 진행률 %는 하드코딩이 아니라 저장값 기반 실시간 계산(`renderProgress()`, 9503-9520행)이라 그 부분은 이미 요구사항을 만족합니다.

**요구사항 재정리**: (1) 오늘 입력값이 그래프에 자동 반영 — 이미 만족. (2) 저장값이 "오늘 하루치 기록"으로 남아야 함 — 미지원. (3) 다음날 재방문 시 전날 기록 유지 + 그날 새 진행률 이어서 입력 — 미지원(하루 단위로 쌓이는 구조 자체가 없음).

**기존 스키마 확인 결과**: `db-schema-final.md`의 `reading_records`에 이미 `current_page`/`total_pages` 컬럼이 있지만(181-199행), 이 역시 "현재 스냅샷" 단일 컬럼이라 일별 이력을 담을 수 없습니다. **일별 진행 기록 테이블이 새로 필요합니다.**

**신규 테이블 설계: `reading_progress_logs`**

| 컬럼명 | 타입 | NULL | 키/제약조건 | 설명 |
|---|---|---|---|---|
| id | BIGINT | N | PK | |
| student_id | BIGINT | N | FK→users.id | |
| reading_record_id | BIGINT | N | FK→reading_records.id | 어느 책의 진행 기록인지 |
| log_date | DATE | N | | 기록 날짜 |
| cumulative_page | INT | N | | 그날 기준 누적 읽은 쪽수 (= 화면의 "오늘 읽은 곳") |
| total_pages | INT | N | | 그 시점의 전체 쪽수 (책마다 다를 수 있어 스냅샷으로 보관) |
| progress_percent | INT | N | | 저장 시점에 계산해서 캐시: `ROUND(cumulative_page/total_pages*100)` |
| created_at | DATETIME | N | | |
| — | — | | UNIQUE(student_id, reading_record_id, log_date) | 하루에 여러 번 저장해도 그날 값은 덮어쓰기(UPSERT), 날짜가 바뀌면 새 행 |

- 저장 시 `INSERT ... ON DUPLICATE KEY UPDATE`(또는 `MERGE`) 패턴으로 그날 값을 갱신. 자정 배치가 필요한 게 **아니라**, 학생이 "나의 진행률 저장하기"를 누르는 **이벤트 시점**에 그날 날짜로 upsert하면 됩니다.
- `reading_records.current_page`/`total_pages`는 그대로 "최신 스냅샷"(빠른 조회용)으로 유지하고, `reading_progress_logs`가 이력을 담당하는 이중 구조를 제안합니다(다른 화면들이 이미 `reading_records.current_page`를 참조할 가능성을 대비해 하위호환 유지).
- 진행률 공식: `progress_percent = MIN(100, ROUND(cumulative_page / total_pages * 100))`.

**AI 판단 필요**: 없음. **일 단위 스냅샷 필요**: **예 — 바로 이 테이블 자체가 그 스냅샷**입니다.

---

## 4. 교사 대시보드 "학생별 달성도" (참여도 50% + 이해도 50%)

**현재 상태: 완전 하드코딩, 계산 로직 0건**

`book-manage.html:2929-3010`과 `individual-reading-manage.html:2158-2255`에 각각 10명/12명짜리 mock 배열이 있고, `total`/`participation`(또는 `readingPractice`)/`understanding`(또는 `recordCompletion`) 모두 리터럴 숫자입니다. 두 파일 모두 렌더 함수(`renderStudentPreview`/`renderStudentDetail`)는 실제로 배열을 순회하며 바 너비(`style="width: ${student.total}%"`) 등을 계산하지만, **그 뒤에 넣는 원본 숫자 자체를 만드는 공식이 없습니다**. (참고: `total` 값들이 우연히 `round((참여도+이해도)/2)`와 거의 일치하지만 반올림이 `.5`에서 위/아래로 들쭉날쭉해 자동 계산이 아니라 손으로 타이핑한 값으로 보입니다.)

`docs/api-endpoints.md` §J도 이 항목을 "현재 하드코딩된 mock 구조 기반... 실제 계산식/저장 방식은 다음 설계 단계에서 확정"이라고 명시해 뒀는데, 이 문서가 바로 그 "다음 설계 단계"입니다.

### 참여도 공식 설계

팝업 문구: "읽기 활동과 질문 만들기, 생각 나누기 활동에 얼마나 꾸준하고 적극적으로 참여했는지" — 3개 하위 요소로 분해합니다.

| 하위 요소 | 데이터 소스 | 계산 |
|---|---|---|
| 읽기 활동 완료율 | `practice_progress` | 완료 플래그 9개(`book_selected`, `before_done`, `class_read_done`, `after_done`, `during_type_progress`의 `direct/infer/opinion/connect/review` 5개) 중 TRUE 개수 / 9 × 100 |
| 질문 만들기 참여율 | `questions` + `responses` | 해당 학급/책에 등록된 `questions` 중 학생이 실제로 답변(`responses.question_id`)한 개수 / 등록된 질문 총 개수 × 100 |
| 생각 나누기 참여율 | `responses` | `MIN(100, COUNT(responses WHERE content_type IN ('chat_post','reply') AND mode='class' AND student_id=?) / 학급 평균 횟수 × 100)` — 학급 평균 대비 상대 평가(교사가 매번 "기대 횟수"를 설정하지 않아도 되는 방식) |

```
참여도 = ROUND(읽기활동완료율 × 0.4 + 질문만들기참여율 × 0.3 + 생각나누기참여율 × 0.3)
```
가중치(0.4/0.3/0.3)는 "읽기 활동"이 3개 하위 활동을 아우르는 전제조건 성격이 강해 약간 더 높게 둔 제안값이며, 기획 확정 필요.

### 이해도 공식 설계 (AI 판단 필요 — 기존 FeedbackAiController로 연동 가능)

**연동 가능 여부**: 가능합니다. `FeedbackAiController`(`POST /api/feedback/ai-review`)의 `SYSTEM_PROMPT_QUESTION`이 정확히 "질문이 책 유형에 맞는 방식으로 작성되었는지", "답이 질문과 논리적으로 연결되는지"를 `good`/`need`로 판정하도록 이미 프롬프트가 짜여 있어, 팝업 문구("이해도 = 질문과 답을 책의 내용에 맞게 작성했는지")와 정확히 일치합니다. **새 프롬프트를 만들 필요 없이 기존 프롬프트/엔드포인트를 그대로 재사용**하면 됩니다.

**현재 갭**: 이 AI 피드백은 지금 학생이 질문을 만들 때 "루미"의 즉석 피드백 용도로만 쓰이고(`requestAiFeedback()`, `frontend/js/ai-feedback.js`), 그 판정 결과(`good`/`need`)가 DB에 저장되지 않습니다. `responses` 테이블에 이미 `passed BOOLEAN` 컬럼이 있으므로(`db-schema-final.md`), 이 컬럼에 AI 판정 결과를 저장하기만 하면 집계가 가능합니다. 다만 이는 `db-schema-audit.md` §5-1("읽기 전 질문/답 내용이 아예 전송되지 않음")과 같은 계열의 문제로, **`responses` 저장 자체가 먼저 연동돼야** 이 지표도 계산 가능합니다.

```
이해도 = ROUND(COUNT(responses WHERE passed=TRUE AND question_id IS NOT NULL AND student_id=?)
              / COUNT(responses WHERE question_id IS NOT NULL AND student_id=?) × 100)
```

**보안 관련 별도 발견**(이 항목 조사 중 우연히 확인, 대시보드 지표와는 무관하지만 중요도가 높아 기록): `src/main/resources/application-secret.properties`에 실제 값이 채워진 것으로 보이는 OpenAI API 키(`sk-proj-...`)가 커밋되어 있습니다. 이 파일이 저장소에 커밋되어서는 안 되며(`.gitignore` 등록 여부 확인 필요), 키 로테이션을 권장합니다. 이 문서 범위 밖이라 별도로 조치가 필요합니다.

### 하루 단위 그래프 — 일별 스냅샷 테이블 필요

**필요합니다.** 참여도/이해도 둘 다 "현재 누적 상태"를 실시간 쿼리로 구할 수는 있지만, "일별 추이 그래프"를 그리려면 그날그날의 값을 별도로 남겨야 합니다.

**신규 테이블 설계: `student_daily_metrics`**

| 컬럼명 | 타입 | NULL | 키/제약조건 | 설명 |
|---|---|---|---|---|
| id | BIGINT | N | PK | |
| student_id | BIGINT | N | FK→users.id | |
| class_id | BIGINT | Y | FK→classes.id | 온책읽기 기준. 개별읽기 집계 시 NULL 가능 |
| metric_date | DATE | N | | 스냅샷 날짜 |
| participation_score | INT | N | | 0~100 |
| understanding_score | INT | N | | 0~100 |
| total_score | INT | N | | `ROUND(participation_score*0.5 + understanding_score*0.5)` |
| activity_count_today | INT | N | DEFAULT 0 | 그날 발생한 활동(응답/요약 등) 건수 — §5 "오늘 참여율" 계산에 재사용 |
| created_at | DATETIME | N | | |
| — | — | | UNIQUE(student_id, metric_date) | |

**스냅샷 시점 설계**: 매일 자정 배치(Spring `@Scheduled(cron = "0 0 0 * * *")`)로 그날까지의 **누적** 참여도/이해도를 계산해서 그날 날짜로 INSERT. "그날 하루치만"이 아니라 "그날 기준 누적 총점"을 매일 스냅샷으로 남기는 방식을 제안합니다 — 그래야 그래프가 우상향하는 성장 곡선으로 표현되어 교육적으로 의미 있고, 개별읽기의 "독서 실천도"(§7, 날짜 카운트가 핵심)와도 자연스럽게 연결됩니다.

---

## 5. 교사 온책읽기 대시보드 "오늘 참여율" / "추가 지원 필요" / "확인이 필요한 학생"

**현재 상태: 완전 하드코딩** (`book-manage.html:2525-2650`)

`wr-today-value`(76%, 7명)와 `wr-support-item`(김수현/이재훈/정우진/박서연/최지우/한지민/윤태호 7명, 각자 사유 텍스트) 모두 정적 HTML이며 JS 계산 코드가 전혀 없습니다. **결정적 근거**: 같은 파일의 `students` mock 배열에서 "지원 필요"/"집중 지원" 상태인 학생은 5명(이재훈·김수현·박서연·최지우·정우진)뿐인데, "확인이 필요한 학생" 목록에는 그 배열에서 "우수"/"보통" 상태인 한지민·윤태호까지 포함된 7명이 나열되어 있습니다. 즉 목록이 `students` 배열을 필터링해서 만들어진 게 아니라 **완전히 별도로 손으로 작성된 독립적인 하드코딩**입니다.

**산출 방식 설계**

1. **오늘 참여율**: "오늘 활동을 1건이라도 한 학생 수 / 학급 전체 학생 수". "활동"의 정의:
```sql
COUNT(DISTINCT student_id)
FROM responses
WHERE class_id = ? AND DATE(created_at) = CURDATE()
-- (온책읽기이므로 mode='class' 조건 포함, §6 확인 필요 항목의 responses.class_id 신규 컬럼 활용)
```
   위 값을 `class_students` 전체 인원으로 나눠 %. 이 지표는 실시간 COUNT로 충분해 별도 스냅샷 불필요(당일 값만 필요).

2. **추가 지원 필요 N명**: §4에서 설계한 `student_daily_metrics.total_score`의 **최신 스냅샷** 기준 임계값 판정.
   - 상태 라벨 구간 제안(기존 mock 데이터의 총점 분포 92/87/82→우수계열, 73→보통, 68/54/47/36/28→지원필요/집중지원 패턴에서 역산):
     - `total_score >= 90`: 매우 우수
     - `80~89`: 우수
     - `70~79`: 보통
     - `50~69`: 지원 필요
     - `< 50`: 집중 지원
   - "추가 지원 필요" 카운트 = `COUNT(student_daily_metrics WHERE class_id=? AND metric_date=최신 AND total_score < 70)`.

3. **확인이 필요한 학생 목록 + 사유**: 위 조건에 해당하는 학생을 뽑은 뒤, 참여도의 3개 하위 지표(읽기활동/질문만들기/생각나누기) 중 **가장 낮은 항목**을 사유로 자동 매핑합니다.
```
읽기활동완료율이 최저 → "읽기 활동 참여 부족" / "읽기 진도 지연"
질문만들기참여율이 최저 → "질문 미제출"
생각나누기참여율이 최저 → "생각 나누기 참여 부족"
이해도점수가 낮음(별도 임계값, 예: understanding_score < 60) → "이해도 낮음"
```
   이 매핑 자체는 DB 컬럼이 아니라 API 응답을 만들 때의 서버 로직(문구 하드코딩은 불가피하나, 판정 근거인 숫자는 실제 계산값)으로 처리하는 것을 제안합니다.

**필요한 신규 테이블/컬럼**: `student_daily_metrics`(§4에서 이미 설계, 재사용), `responses.class_id`(이미 `db-schema-audit.md`에서 신규 제안됨, 재확인).
**AI 판단 필요**: 이해도 관련 사유 판정에 한해 필요(§4와 동일 메커니즘 재사용).
**일 단위 스냅샷**: "확인 필요 학생" 판정은 스냅샷(최신 `student_daily_metrics`) 필요, "오늘 참여율"은 실시간 계산으로 충분.

---

## 6. 책수다방 검수(승인/거절) 기능

**현재 상태: 교사 화면은 실제로 존재하고 동작하지만, 완전히 로컬(localStorage) 전용이며 학생 화면과 100% 단절**

`frontend/teacher/book-chat-manage.html`, `individual-book-chat-manage.html` 둘 다 승인/거절 버튼과 실제 동작하는 JS(`approvePost`/`rejectPost`/`movePost`)가 있지만, `fetch()` 호출이 파일 전체에 **0건**입니다. 승인/거절은 `localStorage`의 세 배열(`bookChatPendingPosts`/`ApprovedPosts`/`RejectedPosts`, 개별읽기는 `individualBookChat*` 접두어)을 서로 옮기는 것뿐입니다.

**결정적 확인**: 이 localStorage 키들을 학생 측 파일(`frontend/student/book-chat.html`, `individual-during-reading.html`)에서 참조하는 코드가 **전무**합니다. 학생 화면의 게시글 렌더 함수(`buildQuestionData()`, `renderBookChatRoom()`)는 학생 자신의 원본 제출 데이터(`mySharedQuestions`, `individualBookChatPosts_<id>`)를 상태값 검사 없이 그대로 보여줍니다. 즉 **교사가 "거절"을 눌러도 학생 화면에서 그 글은 사라지지 않고 계속 보입니다.**

- 승인 시에도 `teacherNote`(교사 코멘트)를 남길 수 있는데, `db-schema-audit.md`의 기존 제안 컬럼(`reviewed_by`, `reviewed_at`, `reject_reason`)에는 **이 필드가 빠져 있습니다.** 새로 발견한 갭입니다.
- `reviewedText`는 `getTodayText()`(사람이 읽기 좋은 날짜 문자열)로, `reviewed_at`을 프론트에서 포맷팅하면 대체 가능해 별도 컬럼 불필요.

**db-schema-audit.md 재확인 결과**: 기존 제안(`reviewed_by`, `reviewed_at`, `reject_reason`, `class_id`, `image_data`)은 대부분 요구사항과 일치하나, 위에서 발견한 `teacher_note`(승인 시 코멘트) 컬럼 추가가 필요합니다.

| 컬럼명(추가 제안) | 타입 | 설명 |
|---|---|---|
| teacher_note | TEXT | 승인 시 교사가 남기는 코멘트. `teacherNote` 대응, NULL 허용 |

**학생 화면 반영 UI 설계 제안**
- `book-chat.html`/`individual-during-reading.html`의 게시글 목록 렌더 함수에 `status` 필터 추가: `approved`만 다른 학생에게 공개, `pending`은 작성자 본인에게만 "검수중" 배지로 표시, `rejected`는 작성자 본인에게만 "반려됨: {reject_reason}" 표시 후 목록에서 숨김.
- 이 필터 없이는 검수 기능 자체가 학생 입장에서 무의미(승인 안 해도 이미 다 보이므로).

**AI 판단 필요**: 없음(사람이 승인/거절). **일 단위 스냅샷**: 불필요.

---

## 7. 개별읽기 평가 기준 (독서 실천도 / 기록 완성도)

**현재 상태**: `individual-reading-manage.html`도 §4와 동일한 패턴 — `readingPractice`/`recordCompletion` 필드를 가진 12명 mock 배열, `total`은 손으로 taped된 근사 평균, 계산 로직 없음. 팝업 문구는 %가 아니라 "개별읽기 활동 참여"/"독서 기록과 활동 결과"라는 카테고리 라벨만 있어 §4(50%/50% 명시)보다 가중치가 덜 명확합니다 — 기획 확인 시 정확한 가중치를 확정하는 것을 제안합니다(이 문서에서는 §4와 동일하게 50/50 가정).

### 독서 실천도 = 독서 일수 + 활동 참여 횟수

| 하위 요소 | 데이터 소스 | 계산 |
|---|---|---|
| 독서 일수 | `reading_progress_logs`(§9 신규 테이블) | `COUNT(DISTINCT log_date)` (기간 내, 예: 이번 달) |
| 활동 참여 횟수 | `responses`(mode='individual') + `summaries` | `COUNT(responses WHERE mode='individual' AND student_id=?) + COUNT(summaries WHERE student_id=?)` (기간 내) |

```
독서실천도 = ROUND(MIN(100, 독서일수/기대일수 × 50) + MIN(100, 활동참여횟수/기대횟수 × 50))
```
`기대일수`/`기대횟수`는 학급 평균 또는 교사가 설정하는 기준값(예: 기대일수=한 달 15일, 기대횟수=10회)으로 정규화 — 정확한 기준값은 기획 확인 필요.

### 기록 완성도 = 기록 내용 적합성(AI 판단) + 활동 완료 여부

| 하위 요소 | 데이터 소스 | 계산 |
|---|---|---|
| 활동 완료율 | `reading_records` | `(before_done + during_done + after_done 중 TRUE 개수) / 3 × 100` |
| 기록 내용 적합성 | `summaries` + AI 판정 | 아래 참고 |

**AI 연동 방식**: §4와 동일 메커니즘. `FeedbackAiController`의 `SYSTEM_PROMPT_SUMMARY`가 정확히 "간추리기가 책 유형 기준(사건 흐름/중심 내용/주장과 이유)에 맞는 핵심 요소를 담고 있는지"를 `good`/`need`로 판정하도록 이미 존재합니다 — **이 프롬프트도 새로 만들 필요 없이 그대로 재사용 가능**합니다. 다만 `summaries` 테이블에는 이 AI 판정 결과를 담을 컬럼이 없습니다(기존 `status` 컬럼은 교사 검수용 pending/approved/rejected이지 AI good/need 판정용이 아님).

**신규 컬럼**: `summaries.ai_passed BOOLEAN NULL` — AI 피드백 API 호출 시 `good`이면 TRUE, `need`면 FALSE 저장.

```
기록내용적합성 = ROUND(COUNT(summaries WHERE ai_passed=TRUE AND student_id=?) 
                    / COUNT(summaries WHERE ai_passed IS NOT NULL AND student_id=?) × 100)
기록완성도 = ROUND(활동완료율 × 0.5 + 기록내용적합성 × 0.5)
```

**필요한 신규 테이블/컬럼**: `summaries.ai_passed`(신규 컬럼), `reading_progress_logs`(§9와 공유). **AI 판단 필요**: 예(기존 `SYSTEM_PROMPT_SUMMARY` 재사용). **일 단위 스냅샷**: §4의 `student_daily_metrics`를 `class_id NULL` 케이스로 확장해서 개별읽기에도 재사용(아래 §8 참고).

---

## 8. 개별읽기 대시보드도 §5(오늘 참여율)·§6(책수다방 관리)이 필요한가

**직접 확인 결과: 이미 둘 다 존재하며, 온책읽기 쪽과 거의 완전히 동일한 패턴으로 복사-붙여넣기 되어 있습니다.**

- `individual-reading-manage.html:1774-1819`에도 "오늘 참여율 76%"/"추가 지원 필요 7명"/"확인이 필요한 학생"(첫 번째가 동일하게 "김수현") 위젯이 그대로 존재 — 사유 텍스트만 "오늘 독서 기록 미작성" 등으로 다를 뿐, 숫자(76%, 7명)까지 온책읽기와 완전히 동일합니다. 즉 §5와 동일한 문제, 동일한 해법이 필요합니다.
- `individual-book-chat-manage.html`도 §6에서 조사한 대로 `book-chat-manage.html`과 동일한 승인/거절 로컬 전용 구조를 갖고 있습니다.

**연습읽기(온책읽기) 로직을 그대로 공유해도 되는가**: 계산 **함수**는 공유 가능하지만, 활동의 **정의**는 갈라져야 합니다.

| 구분 | 온책읽기(§5) | 개별읽기 |
|---|---|---|
| "오늘 활동" 판정 기준 | `responses WHERE class_id=? AND mode='class'` | `responses WHERE mode='individual' AND student_id=?` + `reading_progress_logs WHERE log_date=오늘` (책이 학생마다 달라 `book_id` 대신 `student_id` 기준으로만 집계) |
| 참여도/이해도 대신 | 참여도+이해도 | 독서실천도+기록완성도(§7) |
| 지원 필요 사유 매핑 문구 | "질문 미제출", "읽기 진도 지연" 등 학급 공통 활동 기준 | "오늘 독서 기록 미작성" 등 개인 진행 기준 |

**설계 제안**: `student_daily_metrics` 테이블(§4)의 `class_id`를 NULL 허용으로 설계해뒀으므로, 개별읽기는 `class_id=NULL` + `participation_score`/`understanding_score` 자리에 `reading_practice_score`/`record_completion_score`를 의미상 매핑(또는 컬럼명을 범용화해 `score_a`/`score_b`로 두고 `metric_type` 컬럼으로 온책읽기/개별읽기를 구분)해서 **동일 테이블·동일 배치 로직을 재사용**하고, "오늘 활동"의 SQL WHERE절과 "지원 필요 사유" 매핑 함수만 온책읽기/개별읽기 두 갈래로 분기하는 구조를 제안합니다.

---

## 10. "읽은 책 종류" 그래프 (이야기책/정보책/주장책/그 밖의 책)

**현재 상태: 이번 조사에서 유일하게 실제 계산 로직이 있는 항목** (`individual-during-reading.html:9940-10016`)

```js
const bookTypes = [
  { key: "story", label: "📖 이야기 책" },
  { key: "info", label: "🔍 정보를 담은 책" },
  { key: "opinion", label: "📣 주장을 담은 책" },
  { key: "etc", label: "✨ 그 밖의 책" }
];
```
4개 카테고리는 하드코딩된 마크업이 아니라 JS 상수 배열이고, `getBookTypeCounts()`가 `sessionStorage["individualBookTypeHistory_<id>"]`(책 등록 시 쌓이는 배열)를 순회해 실제로 카운트합니다. 장르 값은 `individual-before-reading.html`의 `selectBookType()`(사용자가 직접 클릭해서 고르는 4개 버튼)에서 실제로 캡처됩니다.

**남은 갭**: (1) `sessionStorage` 기반이라 세션이 끝나면 소실 — 백엔드 미연동. (2) `db-schema-final.md`의 `books.book_type` 설명이 "이야기책/정보책/주장책"(224행 근처) **3개만** 언급하고 있어 4번째 값 `etc`(그 밖의 책)가 스키마 문서에 누락돼 있음 — 이번에 새로 발견.
(3) §3-B와 동일한 이유로, 다권 완독이 실제로 `reading_records`에 쌓이지 않는 한 이 그래프도 서버 집계로 전환할 수 없습니다 — `db-schema-audit.md` §5-5(보관함 다권 누적 미지원)와 **동일 원인**.

**산출 방식(서버 전환 시)**:
```sql
SELECT b.book_type, COUNT(*) AS count
FROM reading_records rr
JOIN books b ON rr.book_id = b.id
WHERE rr.student_id = ? AND rr.finished_at IS NOT NULL
GROUP BY b.book_type
```
% = `count / SUM(count) × 100`.

**필요한 신규 테이블/컬럼**: 새 테이블 불필요. `books.book_type`의 허용값 문서에 `etc`(그 밖의 책) 추가만 필요(스키마 문서 보강, 컬럼 자체는 이미 VARCHAR라 값 추가에 DDL 변경 불필요).
**AI 판단**: 없음. **일 단위 스냅샷**: 불필요(완독 시점 기준 실시간 GROUP BY로 충분).

---

## 11. 신규 테이블/컬럼 총정리

### 신규 테이블

**`reading_progress_logs`** (§9 — 일별 독서 진행 기록)

| 컬럼명 | 타입 | NULL | 키/제약조건 | 설명 |
|---|---|---|---|---|
| id | BIGINT | N | PK | |
| student_id | BIGINT | N | FK→users.id | |
| reading_record_id | BIGINT | N | FK→reading_records.id | |
| log_date | DATE | N | | |
| cumulative_page | INT | N | | |
| total_pages | INT | N | | |
| progress_percent | INT | N | | |
| created_at | DATETIME | N | | |
| — | — | | UNIQUE(student_id, reading_record_id, log_date) | |

**`student_daily_metrics`** (§4, §5, §8 공용 — 일별 참여도/이해도 스냅샷)

| 컬럼명 | 타입 | NULL | 키/제약조건 | 설명 |
|---|---|---|---|---|
| id | BIGINT | N | PK | |
| student_id | BIGINT | N | FK→users.id | |
| class_id | BIGINT | Y | FK→classes.id | 개별읽기 집계 시 NULL |
| metric_type | VARCHAR(20) | N | | 'class_reading' / 'individual_reading' |
| metric_date | DATE | N | | |
| score_a | INT | N | | 온책읽기: 참여도 / 개별읽기: 독서실천도 |
| score_b | INT | N | | 온책읽기: 이해도 / 개별읽기: 기록완성도 |
| total_score | INT | N | | `ROUND(score_a*0.5 + score_b*0.5)` |
| activity_count_today | INT | N | DEFAULT 0 | |
| created_at | DATETIME | N | | |
| — | — | | UNIQUE(student_id, metric_type, metric_date) | |

### 기존 테이블 컬럼 추가

| 테이블 | 컬럼 | 타입 | 설명 |
|---|---|---|---|
| responses | teacher_note | TEXT | §6 — 승인 시 교사 코멘트, `db-schema-audit.md` 기존 제안에서 누락됐던 필드 |
| summaries | ai_passed | BOOLEAN NULL | §7 — AI 요약 적합성 판정 결과 저장 |

### 문서 보강(DDL 불필요)

- `books.book_type`의 허용값 목록에 `etc`(그 밖의 책) 추가 (§10)

---

## 12. 종합 표

| 지표명 | 현재 상태 | 산출 방식 요약 | 필요한 신규 테이블/컬럼 | AI 판단 필요 |
|---|---|---|---|---|
| 1. 읽을 범위 설정→학생 연동 | 완전 단절(로컬 전용) | `books.reading_range` 조회/저장 API 연동만 하면 됨 | 없음 (기존 컬럼 재사용) | 아니오 |
| 2. 우리 반 진행 상황 % | 하드코딩("68%") | 학급 학생 수 대비 `class_read_done=TRUE` 학생 비율 | 없음 | 아니오 |
| 3-A. 나의 힘 | 하드코딩(배선 누락) | `individual-power.js` 연결만 하면 됨 | 없음 | 아니오 |
| 3-B. 월별 완독 기록 | 하드코딩 | `reading_records.finished_at` 월별 GROUP BY | 없음(단, 다권 완독 insert 흐름 선행 필요) | 아니오 |
| 4. 학생별 달성도(참여도/이해도) | 완전 하드코딩(mock) | 참여도=읽기활동+질문+생각나누기 가중합, 이해도=AI good/need 비율 | `student_daily_metrics` 신규 | 예(기존 `SYSTEM_PROMPT_QUESTION` 재사용) |
| 5. 오늘 참여율/지원필요/확인학생 | 완전 하드코딩 | 오늘 활동 학생 비율(실시간), 지원필요=total_score<70(스냅샷) | `student_daily_metrics`, `responses.class_id` | 사유 판정에 일부 필요 |
| 6. 책수다방 검수 | 로컬 전용, 학생화면 미반영 | 승인/거절을 실제 API로, 학생 렌더에 status 필터 추가 | `responses.teacher_note`(신규), 기존 `reviewed_by/at/reject_reason` | 아니오(사람 검수) |
| 7. 독서실천도/기록완성도 | 완전 하드코딩(mock) | 실천도=독서일수+활동횟수, 완성도=활동완료율+AI적합성 | `reading_progress_logs`, `summaries.ai_passed` | 예(기존 `SYSTEM_PROMPT_SUMMARY` 재사용) |
| 8. 개별읽기용 5/6 필요 여부 | 이미 존재(복붙 하드코딩) | §5/§6과 동일 로직, 활동 정의만 개별읽기용으로 분기 | §5/§6과 공유 | §5/§6과 동일 |
| 9. 나의 책 진행 상황(일별) | 저장은 됨(세션 단일값, 이력 없음) | 저장 시점에 그날 날짜로 upsert | `reading_progress_logs` 신규 | 아니오 |
| 10. 읽은 책 종류 | **실제 계산 로직 존재**(세션 한정) | `books.book_type` × `reading_records` GROUP BY | 없음(문서에 `etc` 값 보강만) | 아니오 |

---

## 13. 확인 필요 항목 정리

- §1: "학급당 지정 도서가 항상 1권"이라는 전제가 맞는지 — `books.class_id`+`source='class'`가 유일함을 보장하는 제약이 코드/스키마에 없음.
- §4/§7: 참여도(0.4/0.3/0.3), 참여도·이해도(0.5/0.5), 독서실천도·기록완성도(0.5/0.5로 가정) 가중치는 모두 이번 조사에서 **제안값**이며 기획 확정 필요. `individual-reading-manage.html`의 "평가 기준" 팝업은 %를 명시하지 않아 §4(book-manage.html, 50%/50% 명시)만큼 근거가 확실하지 않음.
- §4/§5/§7: "기대일수"/"기대횟수"/"학급 평균" 같은 정규화 기준값을 고정 상수로 할지, 학급별 동적 평균으로 할지 결정 필요.
- §11: `student_daily_metrics`를 온책읽기/개별읽기 공용(`metric_type` 컬럼으로 구분)으로 설계했는데, 두 도메인이 향후 크게 갈라질 경우 별도 테이블 분리가 나을 수 있음 — 초기엔 공용으로 시작하고 필요시 분리하는 것을 제안.
- (별도, 대시보드 지표와 무관) `application-secret.properties`에 실제 값으로 보이는 OpenAI API 키가 커밋되어 있음 — 로테이션 및 `.gitignore` 처리 필요.
