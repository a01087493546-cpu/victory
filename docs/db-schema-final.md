# DB 테이블 최종 설계

프론트엔드(frontend/, game/) 완성 시점 코드를 기준으로 localStorage/sessionStorage 사용 현황을 전수조사하고, 이를 근거로 확정한 DB 스키마 설계 문서입니다.

## 타입/키 기준

- id 계열: BIGINT
- 짧은 문자열: VARCHAR
- 긴 글: TEXT
- 숫자: INT
- true/false: BOOLEAN
- 날짜/시간: DATETIME
- 복잡한 화면별 추가 데이터: JSON
- PK는 모든 테이블에서 `id`로 통일
- users.id를 학생으로 참조 → `student_id`
- users.id를 교사로 참조 → `teacher_id`
- 그 외 참조는 의미 있는 이름(`book_id`, `reading_record_id`, `question_id` 등)

## 설계 원칙 (조사 과정에서 확인된 것만 반영)

- 코드에 실제 근거가 없는 컬럼은 넣지 않음. 예: `dungeon_records.result_json`(전투 로그) — `game/js/dungeon-ui.js`의 전투 로그는 DOM에만 표시되고 어디에도 저장되지 않으며, `game-api.js` 주석이 미래 연동 시 스펙(`PUT /api/students/{id}/game-state`)을 이미 명시하고 있어 그때 정확히 설계하는 것이 낫다고 판단해 제외함.
- 근거가 있지만 예상과 다른 형태인 경우 실제 형태에 맞게 컬럼화함. 예: `dungeons.reward`는 "아이템/경험치"가 아니라 `game/index.html`의 `DUNGEON_INFO` 배열에 있는 단순 텍스트(`rewardMain`/`rewardNote`) + 능력치 리셋값이라 JSON이 아닌 개별 컬럼으로 처리.
- 죽은 키(정의만 되고 실사용 없는 것)는 설계에서 제외. 예: `individualAfterShareComments_` (individual-after-reading.html).
- 화면 UI 문구까지 확인해 "코드는 있지만 실제로는 폐기/오프라인 대체된 기능"과 "라이브 기능"을 구분함 (아래 "조사 근거" 참고).

---

## 1. 최종 테이블 목록

### 유지
users, classes, class_students, practice_progress, questions, dungeons, dungeon_records

### 수정
books, reading_records, responses, summaries, student_stats

### 신규 추가
student_stat_reward_log, content_likes, book_recommendations

### 삭제 (설계 검토 중 제외)
book_votes — book-select.html이 "투표는 오프라인에서 진행하므로 별도의 온라인 투표 기능은 사용하지 않습니다"(book-select.html:1196)라고 명시. class-reading.html의 "투표로 정해진 책" 라벨(:597)도 실제 기능 없는 잔존 텍스트. 대신 실제로 라이브인 friend-book-shelf.html의 "친구 추천 도서 + 좋아요" 기능을 `book_recommendations` + `content_likes`로 반영.

---

## 2. 테이블별 컬럼 구조

### users (유지)
**역할**: 학생/교사 계정 통합 관리

| 컬럼명 | 타입 | NULL | 키/제약조건 | 설명 |
|---|---|---|---|---|
| id | BIGINT | N | PK, AUTO_INCREMENT | |
| login_id | VARCHAR(50) | N | UNIQUE | 로그인 아이디 |
| password | VARCHAR(255) | N | | 해시된 비밀번호 |
| name | VARCHAR(50) | N | | 이름 |
| role | VARCHAR(20) | N | | student / teacher |
| has_seen_story_intro | BOOLEAN | N | DEFAULT FALSE | `hasSeenStoryIntro_` 대응 |
| created_at | DATETIME | N | DEFAULT CURRENT_TIMESTAMP | |

**관계**: 아래 모든 학생/교사 참조 테이블의 시작점
**JSON 가능 데이터**: 없음 / **반드시 컬럼**: 전체 (인증/조회 대상)

---

### classes (유지)
**역할**: 학급 정보

| 컬럼명 | 타입 | NULL | 키/제약조건 | 설명 |
|---|---|---|---|---|
| id | BIGINT | N | PK | |
| teacher_id | BIGINT | N | FK→users.id | 담당 교사 |
| class_name | VARCHAR(100) | N | | 학급명 |
| grade | INT | Y | | 학년 |
| created_at | DATETIME | N | | |

---

### class_students (유지)
**역할**: 학급-학생 매핑

| 컬럼명 | 타입 | NULL | 키/제약조건 | 설명 |
|---|---|---|---|---|
| id | BIGINT | N | PK | |
| class_id | BIGINT | N | FK→classes.id | |
| student_id | BIGINT | N | FK→users.id | |
| joined_at | DATETIME | N | | |
| | | | UNIQUE(class_id, student_id) | 중복 소속 방지 |

---

### practice_progress (유지)
**역할**: 연습읽기(온책읽기 준비단계) 진행 상태, 학생당 1행

| 컬럼명 | 타입 | NULL | 키/제약조건 | 설명 |
|---|---|---|---|---|
| id | BIGINT | N | PK | |
| student_id | BIGINT | N | FK→users.id, UNIQUE | |
| book_selected | BOOLEAN | N | DEFAULT FALSE | `practiceProgress_.book` |
| before_done | BOOLEAN | N | DEFAULT FALSE | |
| class_read_done | BOOLEAN | N | DEFAULT FALSE | |
| after_done | BOOLEAN | N | DEFAULT FALSE | |
| during_type_progress | JSON | Y | | `{direct, infer, opinion, connect}` 질문유형별 연습 완료여부 |
| updated_at | DATETIME | N | | |

**JSON 가능 데이터**: during_type_progress / **반드시 컬럼**: 4개 완료 플래그 (다른 화면의 잠금해제 분기에 직접 쓰임)

---

### questions (유지)
**역할**: 교사/시스템이 등록한 질문 풀

| 컬럼명 | 타입 | NULL | 키/제약조건 | 설명 |
|---|---|---|---|---|
| id | BIGINT | N | PK | |
| book_id | BIGINT | Y | FK→books.id | 특정 책 전용 질문 |
| class_id | BIGINT | Y | FK→classes.id | 학급 전용 출제 |
| teacher_id | BIGINT | Y | FK→users.id | 출제 교사 |
| question_type | VARCHAR(30) | N | | direct/infer/opinion/connect |
| stage | VARCHAR(20) | Y | | before/during/after |
| content | TEXT | N | | 질문 본문 |
| expected_answer | TEXT | Y | | 모범답안 |
| created_at | DATETIME | N | | |

---

### dungeons (유지, 컬럼 확정)
**역할**: 던전 콘텐츠(스테이지) 정의

| 컬럼명 | 타입 | NULL | 키/제약조건 | 설명 |
|---|---|---|---|---|
| id | BIGINT | N | PK | |
| name | VARCHAR(100) | N | | 초급/중급/고급 던전 |
| description | TEXT | Y | | |
| difficulty | VARCHAR(20) | Y | | "쉬움/보통/어려움" — 순수 표시용, 코드에 비교/정렬 로직 없음 확인 |
| required_books | INT | Y | | 입장 조건 - 등록 책 수 |
| required_stat_avg | INT | Y | | 입장 조건 - 능력치 평균 |
| time_limit_seconds | INT | Y | | 제한시간(180/300/420) |
| enemy_stats | JSON | Y | | `{maxHp, normalAtk, heavyAtk, normalAtkInterval, heavyAtkInterval}` |
| reward_title | VARCHAR(100) | Y | | "나의 힘 +10" / "지식창고 해방 · 문답책 최종 클리어" |
| reward_note | TEXT | Y | | 보상 설명 |
| reward_stat_reset_value | INT | Y | | 클리어 후 리셋되는 능력치 평균값(10/15), 최종 스테이지는 NULL |
| created_at | DATETIME | N | | |

> ⚠️ **연동 시 정리 필요**: 던전 정의가 현재 `game/js/game-api.js`의 `DUNGEONS` 배열(적 전투 스탯 등)과 `game/index.html`의 `DUNGEON_INFO` 배열(표시용 name/desc/req/reward/difficulty/locked)에 **중복 하드코딩**되어 있음. 이 테이블로 이관 시 두 데이터셋을 하나로 통합해야 하며, 프론트/게임 연동 작업에서 반드시 정리 필요.

---

### dungeon_records (유지, 컬럼 확정)
**역할**: 학생별 던전 플레이 기록

| 컬럼명 | 타입 | NULL | 키/제약조건 | 설명 |
|---|---|---|---|---|
| id | BIGINT | N | PK | |
| student_id | BIGINT | N | FK→users.id | |
| dungeon_id | BIGINT | N | FK→dungeons.id | |
| result | VARCHAR(20) | N | | victory/defeat/timeout — `endBattle(result)` 3분기와 대응 |
| played_at | DATETIME | N | | |

> 참고: 실시간 전투 상태(스킬 쿨타임 등)와 전투 로그는 코드상 어디에도 저장되지 않고 클라이언트 런타임/DOM 표시로만 존재함이 확인되어(`game-api.js`, `dungeon-ui.js` 주석 참고) `result_json`/`score` 컬럼은 설계에서 제외함.

---

### books (수정)
**역할**: 학생이 읽는 책 — 교사 지정 온책읽기 도서 + 학생 개인 자유도서 통합

| 컬럼명 | 타입 | NULL | 키/제약조건 | 설명 |
|---|---|---|---|---|
| id | BIGINT | N | PK | |
| title | VARCHAR(200) | N | | |
| author | VARCHAR(100) | Y | | |
| cover_image | TEXT | Y | | base64 이미지 또는 URL |
| book_type | VARCHAR(30) | Y | | 이야기책/정보책/주장책 |
| source | VARCHAR(20) | N | DEFAULT 'individual' | class(온책읽기 지정) / individual(자유도서) |
| class_id | BIGINT | Y | FK→classes.id | source='class'일 때만 값 존재 |
| registered_by | BIGINT | Y | FK→users.id | 등록한 교사 또는 학생 |
| reading_range | VARCHAR(200) | Y | | 온책읽기 읽기 범위 안내 |
| created_at | DATETIME | N | | |

---

### reading_records (수정)
**역할**: 학생의 책 단위 읽기 진행 기록 (개별읽기 기준)

| 컬럼명 | 타입 | NULL | 키/제약조건 | 설명 |
|---|---|---|---|---|
| id | BIGINT | N | PK | |
| student_id | BIGINT | N | FK→users.id | |
| book_id | BIGINT | N | FK→books.id | |
| current_stage | VARCHAR(20) | N | DEFAULT 'before' | before/during/after |
| before_done | BOOLEAN | N | DEFAULT FALSE | |
| during_done | BOOLEAN | N | DEFAULT FALSE | |
| after_done | BOOLEAN | N | DEFAULT FALSE | |
| current_page | INT | Y | | |
| total_pages | INT | Y | | |
| rating | INT | Y | | 완독 후 별점 |
| represent_response_id | BIGINT | Y | FK→responses.id | 완독 팝업 대표질문 |
| finished_at | DATETIME | Y | | |
| created_at | DATETIME | N | | |
| | | | UNIQUE(student_id, book_id) | |

> ⚠️ **순환 참조 주의**: `reading_records.represent_response_id → responses.id` 와 `responses.reading_record_id → reading_records.id` 가 서로를 참조함. 두 컬럼 모두 NULL 허용이라 생성 자체는 문제 없으나, 실제 DDL 적용 시 테이블 생성 순서(또는 FK를 나중에 ALTER TABLE로 추가)를 고려해야 함.

---

### responses (수정)
**역할**: 질문-답변 + 학급/개별 책수다방 콘텐츠(글/댓글/대댓글/심화질문) 전체 통합

| 컬럼명 | 타입 | NULL | 키/제약조건 | 설명 |
|---|---|---|---|---|
| id | BIGINT | N | PK | |
| student_id | BIGINT | N | FK→users.id | |
| book_id | BIGINT | Y | FK→books.id | |
| reading_record_id | BIGINT | Y | FK→reading_records.id | |
| question_id | BIGINT | Y | FK→questions.id | 등록된 질문에 대한 답변인 경우 |
| parent_id | BIGINT | Y | FK→responses.id (자기참조) | 대댓글/답글 |
| mode | VARCHAR(20) | N | | class / individual |
| content_type | VARCHAR(30) | N | | answer/thought/chat_post/deep_question/reply |
| stage | VARCHAR(20) | Y | | before/during/after |
| content | TEXT | Y | | 본문 텍스트 |
| passed | BOOLEAN | Y | | 질문 통과 판정 |
| status | VARCHAR(20) | N | DEFAULT 'approved' | pending/approved/rejected |
| extra_data | JSON | Y | | 모드별 추가 필드 (아래 참조) |
| created_at | DATETIME | N | | |

**extra_data 예시 (mode='class', 학급 책수다방)**
```json
{
  "reactionType": "similar",
  "quoteFromBook": "3장에서 주인공이 친구를 도와주는 장면이요",
  "reasonText": "저도 비슷하게 친구를 도와준 경험이 있어서 공감이 됐어요"
}
```
`reactionType`: similar(비슷해요) / different(다르게 생각해요) / found_in_book(책에서 찾았어요)

**extra_data 예시 (mode='individual', 개별읽기 밸런스 게임)**
```json
{
  "scene": "주인공이 위험에 빠진 친구를 구하러 갈지, 안전한 곳에서 도움을 요청할지 고민하는 장면",
  "optionA": "직접 구하러 간다",
  "optionB": "어른에게 알린다",
  "choice": "B",
  "choiceReason": "혼자 가는 건 더 위험해질 수 있어서 어른에게 알리는 게 맞다고 생각했어요"
}
```

---

### summaries (수정)
**역할**: 간추리기(요약) 결과 저장

| 컬럼명 | 타입 | NULL | 키/제약조건 | 설명 |
|---|---|---|---|---|
| id | BIGINT | N | PK | |
| student_id | BIGINT | N | FK→users.id | |
| book_id | BIGINT | Y | FK→books.id | |
| reading_record_id | BIGINT | Y | FK→reading_records.id | |
| book_type | VARCHAR(30) | Y | | 요약 당시 책 유형 스냅샷 |
| summary_text | TEXT | N | | |
| is_shared | BOOLEAN | N | DEFAULT FALSE | |
| status | VARCHAR(20) | N | DEFAULT 'approved' | pending/approved/rejected |
| created_at | DATETIME | N | | |

---

### student_stats (수정)
**역할**: 학생 능력치 현재값

| 컬럼명 | 타입 | NULL | 키/제약조건 | 설명 |
|---|---|---|---|---|
| id | BIGINT | N | PK | |
| student_id | BIGINT | N | FK→users.id, UNIQUE | |
| magic | INT | N | DEFAULT 8 | 0~100 |
| stamina | INT | N | DEFAULT 8 | 0~100 |
| wisdom | INT | N | DEFAULT 8 | 0~100 |
| courage | INT | N | DEFAULT 8 | 0~100 |
| updated_at | DATETIME | N | | |

---

### student_stat_reward_log (신규)
**역할**: 능력치 보상 중복 지급 방지 이력

| 컬럼명 | 타입 | NULL | 키/제약조건 | 설명 |
|---|---|---|---|---|
| id | BIGINT | N | PK | |
| student_id | BIGINT | N | FK→users.id | |
| reward_type | VARCHAR(60) | N | | 예: individual_before_complete |
| stat_type | VARCHAR(20) | N | | magic/stamina/wisdom/courage |
| amount | INT | N | | 지급량 |
| granted_at | DATETIME | N | | |
| | | | UNIQUE(student_id, reward_type) | 중복 지급 방지 |

---

### content_likes (신규)
**역할**: 요약/책수다글/추천도서 좋아요

| 컬럼명 | 타입 | NULL | 키/제약조건 | 설명 |
|---|---|---|---|---|
| id | BIGINT | N | PK | |
| student_id | BIGINT | N | FK→users.id | |
| content_type | VARCHAR(30) | N | | summary / response / book_recommendation |
| content_id | BIGINT | N | | content_type에 따라 summaries.id/responses.id/book_recommendations.id 참조 |
| created_at | DATETIME | N | | |
| | | | UNIQUE(student_id, content_type, content_id) | |

> ⚠️ `content_id`는 다형성(polymorphic) 참조라 DB 레벨 FK 제약 불가 — 애플리케이션에서 무결성 관리 필요.

---

### book_recommendations (신규, book_votes 대체)
**역할**: 친구에게 추천하는 책 (우리 반 추천 책장)

| 컬럼명 | 타입 | NULL | 키/제약조건 | 설명 |
|---|---|---|---|---|
| id | BIGINT | N | PK | |
| student_id | BIGINT | N | FK→users.id | 추천한 학생 |
| title | VARCHAR(200) | N | | |
| author | VARCHAR(100) | Y | | |
| reason | TEXT | N | | 추천 이유 |
| teaser_response_ids | JSON | Y | | "이 책이 궁금해지는 질문" 최대 3개, responses.id 배열 |
| like_count | INT | N | DEFAULT 0 | 조회 성능용 캐시(실집계는 content_likes) |
| created_at | DATETIME | N | | |

---

## 3. 전체 FK 관계 요약

```
users.id
 ├─ classes.teacher_id
 ├─ class_students.student_id
 ├─ practice_progress.student_id
 ├─ questions.teacher_id
 ├─ books.registered_by
 ├─ reading_records.student_id
 ├─ responses.student_id
 ├─ summaries.student_id
 ├─ student_stats.student_id
 ├─ student_stat_reward_log.student_id
 ├─ content_likes.student_id
 ├─ book_recommendations.student_id
 └─ dungeon_records.student_id

classes.id
 ├─ class_students.class_id
 ├─ questions.class_id
 └─ books.class_id

books.id
 ├─ questions.book_id
 ├─ reading_records.book_id
 ├─ responses.book_id
 └─ summaries.book_id

reading_records.id
 ├─ responses.reading_record_id
 ├─ summaries.reading_record_id
 └─ reading_records.represent_response_id → responses.id (순환 참조, 위 주의사항 참고)

questions.id
 └─ responses.question_id

responses.id
 ├─ responses.parent_id (자기참조, 대댓글)
 ├─ reading_records.represent_response_id
 ├─ content_likes.content_id (content_type='response'일 때, 다형성)
 └─ book_recommendations.teaser_response_ids (JSON 배열 내부, 논리적 참조)

summaries.id
 └─ content_likes.content_id (content_type='summary'일 때, 다형성)

book_recommendations.id
 └─ content_likes.content_id (content_type='book_recommendation'일 때, 다형성)

dungeons.id
 └─ dungeon_records.dungeon_id
```

---

## 4. 조사 근거 (1단계 재조사에서 확인된 주요 사실)

- **book-select.html**: "투표는 오프라인에서 진행하므로 별도의 온라인 투표 기능은 사용하지 않습니다" (line 1196) → book_votes 삭제 근거
- **class-reading.html**: "투표로 정해진 책" 라벨(line 597)은 기능 없는 잔존 텍스트
- **friend-book-write.html**: "지금은 화면 확인용이라 서버에는 저장되지 않아. 다음 단계에서 우리 반 추천 책장과 연결해줄게." (line 1178-1179) → 이번 설계가 그 "다음 단계"
- **책수다방 2가지 모드**: 학급(book-chat.html) "생각 나누기"/"책 퀴즈" + 대댓글 반응(비슷해요/다르게 생각해요/책에서 찾았어요) vs 개별(individual-during-reading.html) "밸런스 선택" 활동(optionA/optionB/scene/choice) — responses.extra_data로 흡수
- **던전**: `game/js/game-api.js`, `dungeon-ui.js` 주석에 "나중에 Spring Boot 연동 시 fetch 호출로 교체할 부분", "현재는 UI 표시만 하고 실제 데이터는 증가하지 않음" 등이 명시되어 있어 전투 로그/스코어는 설계에서 제외, 보상은 `game/index.html`의 DUNGEON_INFO 실제 텍스트에 맞춰 컬럼화
- **죽은 키**: `individualAfterShareComments_` (individual-after-reading.html) — 정의만 있고 실사용 없음, 설계 제외

## 5. 미확정/추후 확인 필요 항목

- `users.has_seen_story_intro` 컬럼 위치는 임시 확정 — 이견 있으면 조정 가능
- `content_likes.content_id` 다형성 참조는 애플리케이션 레벨 무결성 관리 필요
- `reading_records` ↔ `responses` 순환 참조는 실제 DDL 적용 시 생성 순서 고려 필요
- 던전 정의 중복(`game-api.js` DUNGEONS vs `index.html` DUNGEON_INFO)은 게임 연동 단계에서 정리 필요

---

**주의**: 이 문서는 설계 문서이며, 실제 DB에 테이블을 생성하는 SQL은 실행하지 않았습니다.
