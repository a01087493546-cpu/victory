# DB 테이블 감사(Audit) — 프론트엔드 기준 GAP 분석

기존 설계 문서(`docs/db-schema-final.md`, `docs/api-endpoints.md`)를 프론트엔드 실제 코드(`frontend/student/*.html`, `frontend/teacher/*.html`, `frontend/js/*.js`, `game/`)와 다시 대조해서 확인한 감사 결과입니다.

**조사 방법**: `sessionStorage`/`localStorage`의 `setItem`/`getItem` 호출을 프론트 전체에서 전수조사하고(약 220개 호출 지점, 24개 파일), 각 키가 실제로 어떤 화면에서 쓰이는지, 어떤 모양의 데이터를 담는지 확인한 뒤 `docs/db-schema-final.md`의 테이블/컬럼과 1:1로 대조했습니다. 백엔드는 `src/main/java/com/victory/` 기준으로 Entity/Repository/Controller가 **전혀 없는 상태**(FeedbackAiController 1개만 존재, DB 미연동)임을 재확인했습니다.

**코드 수정 없음** — 이 문서는 분석/제안만 담고 있습니다.

---

## 0. 먼저 짚어야 할 문서 간 불일치 (테이블 설계 이전 문제)

- `docs/api-endpoints.md`가 참조하는 프론트 파일 중 **`frontend/student/book-vote.html`, `before-reading-share.html`, `question-bundle.html`은 현재 저장소에 존재하지 않습니다** (`find` 결과 없음). 반면 `docs/db-schema-final.md`는 `book-select.html:1196`("투표는 오프라인에서 진행하므로 별도의 온라인 투표 기능은 사용하지 않습니다")을 근거로 `book_votes` 테이블을 **삭제**하기로 결정했습니다. 즉 API 문서(43~49개 엔드포인트 중 투표 관련 4개 포함)와 DB 스키마 문서가 서로 다른 시점의 프론트 상태를 근거로 작성되어 있습니다 — **DB 스키마 문서 쪽 결론(투표 기능 제외)이 현재 코드와 일치**하므로, `api-endpoints.md`의 투표 관련 4개 엔드포인트(`book-vote/status` GET/PATCH, `book-vote` POST, `book-vote/records` GET)는 재검토가 필요합니다.
- `game/` 디렉터리는 `frontend/` 하위가 아니라 **저장소 최상위**(`./game/`)에 존재합니다(`game/index.html`, `game/js/game-api.js`, `game/js/dungeon-ui.js`, `game/js/game-state.js`). `individual-reading.html`의 "던전 입장" 버튼(`../../game/index.html`)은 정상적으로 이 경로를 가리키며 **죽은 링크가 아닙니다.** `dungeons`/`dungeon_records` 테이블 설계는 이 디렉터리의 실제 코드를 근거로 이미 잘 되어 있습니다(아래 표 참고).

---

## 1. 유지해야 할 기존 테이블

프론트 데이터와 대조해 컬럼 변경 없이 그대로 써도 되는 테이블입니다.

### users
- **역할**: 학생/교사 계정 통합 관리
- **표**:

| 컬럼명 | 타입 | 설명 | 비고 |
|---|---|---|---|
| id | BIGINT | PK | 유지 |
| login_id | VARCHAR(50) | 로그인 아이디, UNIQUE | 유지 — `auth.js`의 `loginId` 대응 |
| password | VARCHAR(255) | 해시된 비밀번호 | 유지 |
| name | VARCHAR(50) | 이름 | 유지 |
| role | VARCHAR(20) | student / teacher | 유지 |
| has_seen_story_intro | BOOLEAN | `hasSeenStoryIntro_<studentId>` 대응 | 유지 |
| created_at | DATETIME | | 유지 |

> ⚠️ 이 테이블은 **표 3**에서 `school` 컬럼 추가가 필요합니다 (아래 참고). 나머지 컬럼은 그대로 유지.

### classes
- **역할**: 학급 정보
- **표**:

| 컬럼명 | 타입 | 설명 | 비고 |
|---|---|---|---|
| id | BIGINT | PK | 유지 |
| teacher_id | BIGINT | FK→users.id | 유지 |
| class_name | VARCHAR(100) | 학급명 | 유지 |
| class_number | INT | 반 번호(teacher-register.html classSelect) | 유지 |
| grade | INT | 학년 | 유지 |
| created_at | DATETIME | | 유지 |

### practice_progress
- **역할**: 연습읽기(온책읽기) 진행 상태, 학생당 1행. `practice.html`이 이미 `GET/POST /api/students/{studentId}/practice-progress`로 실제 fetch 호출을 하고 있어(문서상 유일하게 프론트가 실제로 연동을 시도하는 영역), 프론트 쪽 sessionStorage 캐시(`duringReadingPracticeProgress_<id>`)는 쓰기만 되고 읽히지 않는 죽은 캐시임을 확인했습니다 — 백엔드 API가 진짜 소스여야 합니다.
- **표**:

| 컬럼명 | 타입 | 설명 | 비고 |
|---|---|---|---|
| id | BIGINT | PK | 유지 |
| student_id | BIGINT | FK→users.id, UNIQUE | 유지 |
| book_selected | BOOLEAN | | 유지 |
| before_done | BOOLEAN | | 유지 |
| class_read_done | BOOLEAN | | 유지 |
| after_done | BOOLEAN | | 유지 |
| during_type_progress | JSON | `{direct, infer, opinion, connect, review}` | 유지 — `duringReadingPracticeProgress_<id>`와 일치. `review`는 "총 복습" 화면(`finishReview()`) 완료 여부를 기록하는 정식 필드로 확인됨(스키마 설명에 `review` 추가 필요). 단, 이 값이 다른 화면 흐름을 게이트하지는 않음 — 완료 여부 기록용 |
| updated_at | DATETIME | | 유지 |

### questions
- **역할**: 교사/시스템이 등록한 질문 풀
- **표**:

| 컬럼명 | 타입 | 설명 | 비고 |
|---|---|---|---|
| id | BIGINT | PK | 유지 |
| book_id | BIGINT | FK→books.id | 유지 |
| class_id | BIGINT | FK→classes.id | 유지 |
| teacher_id | BIGINT | FK→users.id | 유지 |
| question_type | VARCHAR(30) | direct/infer/opinion/connect | 유지 |
| stage | VARCHAR(20) | before/during/after | 유지 |
| content | TEXT | 질문 본문 | 유지 |
| expected_answer | TEXT | 모범답안 | 유지 |
| created_at | DATETIME | | 유지 |

### dungeons
- **역할**: 던전 콘텐츠(스테이지) 정의. `game/index.html`의 `DUNGEON_INFO` 배열, `game/js/game-api.js`의 `DUNGEONS` 배열과 대조 확인함 — 실제로 두 곳에 중복 하드코딩되어 있는 상태(정상 존재하는 코드 기준, §0 참고).
- **표**:

| 컬럼명 | 타입 | 설명 | 비고 |
|---|---|---|---|
| id | BIGINT | PK | 유지 |
| name | VARCHAR(100) | 초급/중급/고급 던전 | 유지 |
| description | TEXT | | 유지 |
| difficulty | VARCHAR(20) | 표시용 텍스트, 비교/정렬 로직 없음 확인 | 유지 |
| required_books | INT | 입장 조건 - 등록 책 수 | 유지 |
| required_stat_avg | INT | 입장 조건 - 능력치 평균 | 유지 |
| time_limit_seconds | INT | 제한시간 | 유지 |
| enemy_stats | JSON | `{maxHp, normalAtk, heavyAtk, normalAtkInterval, heavyAtkInterval}` | 유지 |
| reward_title | VARCHAR(100) | | 유지 |
| reward_note | TEXT | | 유지 |
| reward_stat_reset_value | INT | | 유지 |
| created_at | DATETIME | | 유지 |

> ⚠️ 연동 시 `game-api.js`의 `DUNGEONS`와 `index.html`의 `DUNGEON_INFO` 중복을 이 테이블로 통합 필요(기존 문서 지적 그대로 유효).

### dungeon_records
- **역할**: 학생별 던전 플레이 기록. `game/js/game-state.js`가 `sessionStorage.getItem('studentId')`만 읽고 있고, 전투 로그/스킬 쿨타임은 DOM 표시로만 존재(저장 안 됨) — 재확인함.
- **표**:

| 컬럼명 | 타입 | 설명 | 비고 |
|---|---|---|---|
| id | BIGINT | PK | 유지 |
| student_id | BIGINT | FK→users.id | 유지 |
| dungeon_id | BIGINT | FK→dungeons.id | 유지 |
| result | VARCHAR(20) | victory/defeat/timeout | 유지 |
| played_at | DATETIME | | 유지 |

### student_stats
- **역할**: 학생 능력치 현재값. `frontend/js/individual-power.js`의 `individualPower_v2_<studentId>` (localStorage, `{magic,stamina,wisdom,courage}`, 0~100, 기본값 8)와 정확히 1:1로 대응함을 재확인.
- **표**:

| 컬럼명 | 타입 | 설명 | 비고 |
|---|---|---|---|
| id | BIGINT | PK | 유지 |
| student_id | BIGINT | FK→users.id, UNIQUE | 유지 |
| magic | INT | 0~100, 기본 8 | 유지 |
| stamina | INT | 0~100, 기본 8 | 유지 |
| wisdom | INT | 0~100, 기본 8 | 유지 |
| courage | INT | 0~100, 기본 8 | 유지 |
| updated_at | DATETIME | | 유지 |

> ⚠️ 프론트에 **동일 키를 두 경로로 읽고 쓰는 버그성 코드**가 있습니다: `individual-during-reading.html`(9663~9715행 부근)이 `individual-power.js`와 별도로 자체 `readIndividualPowerState()`/`saveIndividualPowerState()`를 정의해서 같은 `individualPower_v2_<studentId>` 값을 **localStorage와 sessionStorage 양쪽에 씁니다**(개별읽기.js는 localStorage만 사용). 백엔드 연동 시 이 두 경로가 하나의 API 호출로 합쳐져야 하며, 그렇지 않으면 클라이언트 캐시 불일치가 재현될 수 있습니다. (테이블 설계 자체엔 영향 없음, 프론트 리팩터링 메모로 남김)

### student_stat_reward_log (신규 테이블이지만 이미 db-schema-final.md에서 제안됨 — 재확인 결과 그대로 유지)
- **역할**: 능력치 보상 중복 지급 방지. `individual-power.js`의 `individualRewardHistory_v2_<studentId>`(localStorage, 보상 키 문자열 배열, 예: `"individual_before_complete_3"`, `"book_chat_post_book-chat-1234"`)와 `REWARD_PRESETS`(12종: `practice_all_complete`, `individual_question_created`, `individual_during_questions_complete`, `individual_before_complete`, `individual_feedback_pass`, `individual_after_questions_pass`, `individual_after_summary_pass`, `individual_friend_book_recommend`, `individual_book_chat_post`, `individual_question_shared`, `individual_summary_shared`, `individual_thought_comment`, `individual_share_success`, `individual_after_complete`)를 대조 확인함 — 설계 그대로 유효.
- **표**: (아래 §2 신규 테이블 항목 참고 — db-schema-final.md 원안 그대로)

---

## 2. 신규 추가가 필요한 테이블

### student_stat_reward_log
- **역할**: 보상 1건(예: `individual_after_complete`)이 여러 능력치(체력+마법력+지혜)를 동시에 올리므로, 능력치별로 1행씩 남겨서 중복 지급을 막는 이력 테이블
- **표**:

| 컬럼명 | 타입 | 설명 | 비고 |
|---|---|---|---|
| id | BIGINT | PK | 신규 |
| student_id | BIGINT | FK→users.id | 신규 |
| reward_type | VARCHAR(60) | 예: `individual_before_complete` — `REWARD_PRESETS` 키와 대응 | 신규 |
| stat_type | VARCHAR(20) | magic/stamina/wisdom/courage | 신규 |
| amount | INT | 지급량 | 신규 |
| granted_at | DATETIME | | 신규 |
| — | — | UNIQUE(student_id, reward_type) | 신규, 중복 지급 방지 |

### content_likes
- **역할**: 요약(summaries)/책수다글(responses)/추천도서(book_recommendations) 좋아요. `individualFriendBookLikedBooks_<studentId>`(친구 추천 책 좋아요), `afterReadSummaryLikes_<...>`(연습읽기 간추리기 좋아요) 등 여러 화면에서 각자 다른 키로 흩어져 있던 "좋아요" 기능을 통합
- **표**:

| 컬럼명 | 타입 | 설명 | 비고 |
|---|---|---|---|
| id | BIGINT | PK | 신규 |
| student_id | BIGINT | FK→users.id | 신규 |
| content_type | VARCHAR(30) | summary / response / book_recommendation | 신규 |
| content_id | BIGINT | 다형성 참조(DB 레벨 FK 불가, 앱에서 무결성 관리) | 신규 |
| created_at | DATETIME | | 신규 |
| — | — | UNIQUE(student_id, content_type, content_id) | 신규 |

### book_recommendations
- **역할**: 친구 추천 책장. `individualFriendBookRecommendations_<studentId>`(sessionStorage, `{id,title,author,reason,teaserQuestions:[...],recommender,likes,mine:true,createdAt}`)와 대응
- **표**:

| 컬럼명 | 타입 | 설명 | 비고 |
|---|---|---|---|
| id | BIGINT | PK | 신규 |
| student_id | BIGINT | 추천한 학생 | 신규 |
| title | VARCHAR(200) | | 신규 |
| author | VARCHAR(100) | | 신규 |
| reason | TEXT | 추천 이유 | 신규 |
| teaser_response_ids | JSON | "궁금해지는 질문" 최대 3개, responses.id 배열(추정) | 신규 — **확인 필요**: 프론트의 `teaserQuestions` 필드가 실제로 기존 responses(질문) ID를 참조하는지, 아니면 텍스트를 새로 복사해 저장하는지 `friend-book-write.html`의 `getQuestionCandidates()`/제출 로직만으로는 완전히 확정하지 못했음. 텍스트 복사 방식이라면 이 컬럼은 `JSON`(텍스트 배열)로 바뀌어야 함 |
| like_count | INT | 캐시(실집계는 content_likes) | 신규 |
| created_at | DATETIME | | 신규 |

---

## 3. 제거 또는 수정이 필요한 기존 테이블

### book_votes — 제거 (db-schema-final.md 원안 유지, 재확인 완료)
- **역할(기존)**: 책 투표 기능용으로 설계되었던 테이블(현재 스키마 문서엔 이미 삭제 결정됨, 코드에는 애초에 없음)
- **문제점**: `book-select.html:1196`에 "투표는 오프라인에서 진행하므로 별도의 온라인 투표 기능은 사용하지 않습니다"라고 명시되어 있고, `class-reading.html`의 "투표로 정해진 책" 문구도 실제 기능 없는 잔존 텍스트임을 재확인. `book-vote.html` 자체가 저장소에 없음(§0).
- **제안**: 테이블 생성 대상에서 제외 유지. **`docs/api-endpoints.md`의 투표 관련 엔드포인트 4개(`book-vote/status` GET·PATCH, `book-vote` POST, `book-vote/records` GET)도 함께 삭제 검토 필요** — 이번 감사에서 새로 발견한 문서 불일치.

### users — 컬럼 추가 필요
- **역할**: 학생/교사 계정 통합 관리
- **문제점**: `teacher-register.html`이 실제로 제출하는 데이터 `{teacher:{name, school, id, password}, ...}`에 `school`(학교명) 필드가 있고, `docs/api-endpoints.md`의 `POST /api/teachers/register` 요청 스펙에도 동일하게 `school`이 명시되어 있는데, `users` 테이블에는 이를 담을 컬럼이 없음.
- **표**:

| 컬럼명 | 타입 | 설명 | 비고 |
|---|---|---|---|
| school | VARCHAR(100) | 교사 소속 학교명. `teacherRegisterData.teacher.school`, `api-endpoints.md`의 `POST /api/teachers/register` 요청 필드 대응 | **신규 컬럼 추가**, NULL 허용(학생은 값 없음) |

### class_students — 컬럼 추가 필요
- **역할**: 학급-학생 매핑
- **문제점**: `teacher-register.html`의 학생 등록 데이터가 `students:[{number, name, id, password}]` 형태로 학급 내 "번호"를 갖고 있고, `docs/api-endpoints.md`의 `GET /api/classes/{classId}/students` 응답 스펙도 `[{studentId, name, number}]`로 번호를 포함하는데, `class_students`에는 이 컬럼이 없음(테이블의 `class_number`는 "반 번호"이지 "학급 내 학생 번호"가 아님 — 혼동 주의).
- **표**:

| 컬럼명 | 타입 | 설명 | 비고 |
|---|---|---|---|
| student_number | INT | 학급 내 학생 출석번호. `students[].number` 대응 | **신규 컬럼 추가**, NULL 허용 |

### responses — 컬럼 추가 필요 (3건)
- **역할**: 질문-답변 + 학급/개별 책수다방 콘텐츠 통합
- **문제점 1 — 검수 이력 없음**: `frontend/teacher/book-chat-manage.html`, `individual-book-chat-manage.html`의 모더레이션 큐 객체가 `{..., status, reviewedAt, reviewedText, rejectReason}` 형태로 검수 시각/검수자 코멘트/반려 사유를 담고 있는데, `responses` 테이블은 `status`(pending/approved/rejected)만 있고 **검수 관련 컬럼이 전혀 없음**.
- **문제점 2 — 학급 단위 조회 불가 위험**: 교사가 "우리 반 책수다방 검수 큐"를 조회하려면 학급으로 필터링해야 하는데, `responses`에는 `class_id`가 없고 `book_id`(nullable)를 거쳐야만 학급을 알 수 있음. `mode='class'` 콘텐츠인데 `book_id`가 비어있는 경우(추정: 책과 무관한 순수 채팅형 콘텐츠가 있다면) 학급 조회가 불가능해짐 — 확인 필요.
- **문제점 3 — 이미지 데이터 컬럼 없음**: `individualBookChatPosts_<studentId>`(개별읽기 책수다방, localStorage)에 `imageData`(FileReader로 읽은 base64 dataURL)가 들어가는데, `responses.extra_data`(JSON)의 개별읽기 예시 스키마엔 이미지 필드가 없고, JSON 컬럼에 큰 base64 문자열을 넣는 것도 비효율적임.
- **표**:

| 컬럼명 | 타입 | 설명 | 비고 |
|---|---|---|---|
| reviewed_by | BIGINT | FK→users.id, 검수한 교사 | **신규 컬럼 추가**, NULL 허용 |
| reviewed_at | DATETIME | 검수 시각. `reviewedAt` 대응 | **신규 컬럼 추가**, NULL 허용 |
| reject_reason | TEXT | 반려 사유. `rejectReason` 대응 | **신규 컬럼 추가**, NULL 허용 |
| class_id | BIGINT | FK→classes.id, mode='class'일 때 직접 조회용 | **신규 컬럼 추가**, NULL 허용 — book_id 경유 조회의 대안/보강 |
| image_data | LONGTEXT | base64 이미지. `books.cover_image`와 동일 방식 | **신규 컬럼 추가**, NULL 허용. 향후 실제 파일 업로드 전환 시 URL 컬럼으로 교체 권장 |

### books, reading_records, summaries — 컬럼 변경 없음(기존 "수정" 결정 재확인만)
- 세 테이블 모두 db-schema-final.md의 기존 설계가 프론트 데이터와 정확히 대응함을 재확인했고(예: `books.reading_range` ↔ `wholeReadingRange`/`book-manage.html`, `books.cover_image` ↔ `selectedClassBook.cover`, `reading_records` ↔ 개별읽기 stage 플래그들, `summaries.status` ↔ 연습읽기/개별읽기 공유 승인 흐름), **추가로 발견된 컬럼 변경 사항은 없습니다.** 기존 문서의 순환참조 주의사항(`reading_records.represent_response_id` ↔ `responses.reading_record_id`)도 그대로 유효합니다.

---

## 4. 테이블 관계(FK) 요약

```
users.id
 ├─ classes.teacher_id
 ├─ class_students.student_id
 ├─ practice_progress.student_id
 ├─ questions.teacher_id
 ├─ books.registered_by
 ├─ reading_records.student_id
 ├─ responses.student_id
 ├─ responses.reviewed_by          ← 신규
 ├─ summaries.student_id
 ├─ student_stats.student_id
 ├─ student_stat_reward_log.student_id
 ├─ content_likes.student_id
 ├─ book_recommendations.student_id
 └─ dungeon_records.student_id

classes.id
 ├─ class_students.class_id
 ├─ questions.class_id
 ├─ books.class_id
 └─ responses.class_id             ← 신규

books.id
 ├─ questions.book_id
 ├─ reading_records.book_id
 ├─ responses.book_id
 └─ summaries.book_id

reading_records.id
 ├─ responses.reading_record_id
 ├─ summaries.reading_record_id
 └─ reading_records.represent_response_id → responses.id (순환 참조, 생성 순서 주의)

questions.id
 └─ responses.question_id

responses.id
 ├─ responses.parent_id (자기참조, 대댓글)
 ├─ reading_records.represent_response_id
 ├─ content_likes.content_id (content_type='response', 다형성)
 └─ book_recommendations.teaser_response_ids (JSON 배열 내부, 논리적 참조 — 확인 필요)

summaries.id
 └─ content_likes.content_id (content_type='summary', 다형성)

book_recommendations.id
 └─ content_likes.content_id (content_type='book_recommendation', 다형성)

dungeons.id
 └─ dungeon_records.dungeon_id
```

---

## 5. 스키마 문제는 아니지만 함께 트래킹해야 할 프론트-백엔드 연동 갭

테이블 설계와는 별개로, "테이블은 있어도 프론트가 아직 그 데이터를 백엔드로 보내지 않는" 지점들입니다. 스키마 수정 대상은 아니라서 위 표에서 제외했지만, 실제 연동 작업 순서를 잡을 때 필요해서 남겨둡니다.

1. **연습읽기 읽기 전 질문/답 내용이 아예 전송되지 않음**: `before-reading.html`의 `answers` 배열(4개 질문/답)은 메모리에만 있다가 `completeAndGoPractice()`가 백엔드로 `{bookSelected:true, beforeDone:true}`만 보내고 실제 질문/답 텍스트는 버림. `questions`/`responses` 테이블은 이미 이 데이터를 받을 준비가 되어 있음 — 프론트 쪽 API 연동만 빠진 상태.
2. **교사 계정/학급/학생 등록이 완전히 로컬 목업**: `teacher-register.html`은 `fetch` 호출이 전혀 없고 `teacherRegisterData`/`registeredAccountIds`(localStorage)에만 저장함. `POST /api/teachers/register`는 문서화되어 있으나 프론트에서 호출하지 않음 — 로그인만 실제 백엔드를 쓰고 계정 생성은 아직 가짜.
3. **선정 도서 키 불일치(버그)**: 교사용 `book-select.html`은 `localStorage["selectedClassBook"]`에 저장하는데, 학생용 `before-reading.html`/`read-before-book-intro.html`은 `sessionStorage["selectedBook"]`을 읽음 — 스토리지 종류와 키 이름이 모두 달라 교사가 고른 책이 학생 화면에 절대 반영되지 않음. 스키마상 `books`(source='class') 하나로 흡수되면 자동 해결되는 문제지만, 연동 순서상 짚어둘 필요.
4. **월별 완독 기록 / "나의 힘" 그래프가 완전히 하드코딩**: `individual-reading.html`의 능력치 4개 수치(마법력/체력/지혜/용기 각 `11/100` 등)와 12개월 막대그래프가 전부 정적 마크업이며 `individualPower_v2_<studentId>`나 `reading_records.finished_at` 기반 집계를 전혀 읽지 않음. 스키마(`student_stats`, `reading_records.finished_at`)는 이미 이 화면을 지원할 수 있음 — 프론트 연동만 빠진 상태.
5. **개별읽기 보관함(`individual-reading-archive.html`)이 다권 누적을 지원하지 않음**: `individualReadingArchive_<studentId>` 키는 코드에서 참조는 되지만 **어디서도 실제로 쓰이지(setItem) 않음** — 현재는 "방금 완료한 책 1권"만 조합해서 보여주거나, 그마저 없으면 하드코딩 예시로 대체함. `reading_records`+`summaries`+`responses` 조합 쿼리로 실제 다권 보관함을 만들 수 있으나 아직 프론트/API 연동이 없음.

---

## 6. 확인 필요 항목 정리 (코드만으로 완전히 확정 못한 것)

- `book_recommendations.teaser_response_ids`가 기존 responses.id 참조인지, 텍스트 복사본인지 — `friend-book-write.html` 제출 로직 추가 확인 필요
- `responses`에 `class_id`를 넣을지, 아니면 `mode='class'` 콘텐츠는 항상 `book_id`가 채워진다는 전제로 갈지 — 기획 확인 필요
- `content_likes`/`book_recommendations`의 실제 좋아요 수 집계 방식(캐시 컬럼 `like_count` vs 매번 COUNT) — 성능 요구사항에 따라 결정 필요
