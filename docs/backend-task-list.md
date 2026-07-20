# 백엔드 개발 작업 목록 (기능/화면 단위)

16개 테이블 설계를 바탕으로, 학생이 실제로 프로그램을 쓰는 순서대로 "뭘 만들어야 하는지" 계획만 정리한 문서입니다. **코드는 작성하지 않았습니다.**

테이블 번호는 이전에 정리한 "최종 테이블 설계" 문서의 번호(1.users ~ 16.student_daily_metrics)를 그대로 씁니다.

---

## 작업 순서 요약표

| 순서 | 기능 이름 | 왜 이 순서인지 |
|---|---|---|
| 1 | 로그인/회원가입 | 이게 안 되면 다른 기능을 테스트할 방법 자체가 없음 |
| 2 | 연습읽기 진행 | 모든 학생이 제일 먼저 겪는 활동. 완료해야 개별읽기가 열림 |
| 3 | 개별읽기 진행 | 연습읽기가 열어주는 다음 단계, 능력치가 본격적으로 쌓이기 시작하는 지점 |
| 4 | 책수다방 | 연습읽기/개별읽기 양쪽에 걸쳐 있는 공통 기능, 검수 흐름까지 포함 |
| 5 | 나의 독서보관함 | 개별읽기로 쌓인 완독 기록을 "보여주기만" 하는 화면이라 3번 이후 |
| 6 | 친구 추천 책장 | 개별읽기의 부가 기능, 필수 경로는 아님 |
| 7 | 나의 힘(능력치)+보상 | 읽기 활동 전반에 걸쳐 필요하지만, 게임(8번)의 전제조건이라 그 앞에 배치 |
| 8 | 게임(던전) | 능력치가 쌓인 다음에야 의미가 생기는 마지막 학생용 기능 |
| 9 | 교사 대시보드 | 학생 활동 데이터가 어느 정도 쌓여야 통계 화면이 의미 있음 |
| 10 | 일별 스냅샷 배치 | 9번 대시보드가 그대로 의존하는 배치 작업이라 9번과 묶어서 진행 |

---

### 1. 로그인/회원가입
**뭐 하는 기능인지**: 학생/교사가 로그인하고, 교사가 학급을 만들어 학생 계정을 한 번에 발급하는 기능

**필요한 테이블**: 1. users, 2. classes, 3. class_students

**필요한 API**:
| 뭘 하는 API인지 | 메서드/URL | 상태 |
|---|---|---|
| 로그인 | POST /api/auth/login | ✅ 이미 있음 |
| 단일 계정 가입 | POST /api/auth/register | ✅ 있지만 개인 1명만 가입, 아래와는 다른 기능 |
| 교사+학급+학생 일괄 등록 | POST /api/teachers/register | ❌ 신규 필요 |
| 학급 학생 명단 조회 | GET /api/classes/{classId}/students | ❌ 신규 필요 |

**계산 공식/규칙**: 없음

**프론트도 같이 손봐야 하는지**: **O** — `teacher-register.html`이 `fetch` 호출 자체가 없이 `localStorage`에만 저장 중이라, 신규 API를 만든 뒤 프론트 연동도 필요.

---

### 2. 연습읽기 진행 (읽기전/중/후, 총복습)
**뭐 하는 기능인지**: 반 전체가 같은 책으로 읽기 전 → 읽기 중 → 읽기 후 → 총복습까지 진행하는 흐름

**필요한 테이블**: 4. practice_progress, 7. books, 9. responses, 10. summaries

**필요한 API**:
| 뭘 하는 API인지 | 메서드/URL | 상태 |
|---|---|---|
| 진행 상태 조회/저장 | GET/POST /api/students/{id}/practice-progress | ✅ 이미 있음 (review 필드까지 반영됐는지 재확인 필요) |
| 읽기 전 질문/답 저장 | POST /api/students/{id}/before-reading-answers | ❌ 신규 필요 |
| 학급 선정도서/읽기범위 조회 | GET /api/classes/{classId}/reading-range | ❌ 신규 필요 |
| 학급 선정도서/읽기범위 저장 | POST /api/classes/{classId}/reading-range | ❌ 신규 필요 |
| 간추리기 저장 | POST /api/students/{id}/after-read-summaries | ❌ 신규 필요 |

**계산 공식/규칙**: 없음(완료 플래그만 관리)

**프론트도 같이 손봐야 하는지**: **O** — `before-reading.html`의 질문/답 텍스트가 지금 아예 전송 안 됨. `book-select.html`(교사)과 `before-reading.html`(학생)의 선정도서 저장 키가 서로 달라 절대 연동 안 되는 버그도 같이 고쳐야 함.

---

### 3. 개별읽기 진행 (오늘의 독서모험 읽기전/중/후, 나의 책 진행상황)
**뭐 하는 기능인지**: 학생이 자기가 고른 책으로 읽기 전 → 읽기 중 → 읽기 후를 진행하고, 매일 몇 쪽 읽었는지 기록하는 기능

**필요한 테이블**: 7. books, 8. reading_records, 9. responses, 10. summaries, 15. reading_progress_logs

**필요한 API**:
| 뭘 하는 API인지 | 메서드/URL | 상태 |
|---|---|---|
| 개별읽기 읽기 전 저장(책 등록+질문) | POST /api/students/{id}/individual-reading/before | ❌ 신규 필요 |
| 개별읽기 읽기 중 저장 | POST /api/students/{id}/individual-reading/during | ❌ 신규 필요 |
| 개별읽기 읽기 후 저장 | POST /api/students/{id}/individual-reading/after | ❌ 신규 필요 |
| 오늘 읽은 쪽수 저장 | POST /api/students/{id}/reading-progress-logs | ❌ 신규 필요 |
| 진행 기록 조회 | GET /api/students/{id}/reading-progress-logs | ❌ 신규 필요 |

**계산 공식/규칙**:
- 진행률(%) = MIN(100, ROUND(누적읽은쪽수 ÷ 전체쪽수 × 100))

**프론트도 같이 손봐야 하는지**: **O** — 전부 `localStorage`/`sessionStorage`에만 저장 중. 특히 "오늘 읽은 쪽수"는 세션 단일 값이라 날짜별 기록 자체가 없음(하루 지나면 덮어써짐).

---

### 4. 책수다방 (연습읽기/개별읽기 공통 — 작성, 승인/거절, 삭제, 노출 규칙)
**뭐 하는 기능인지**: 학생이 글을 쓰면 교사가 승인/거절하고, 승인된 글만 다른 학생에게 보이는 기능

**필요한 테이블**: 9. responses

**필요한 API**:
| 뭘 하는 API인지 | 메서드/URL | 상태 |
|---|---|---|
| 글 목록 조회 | GET /api/classes/{classId}/book-chat/posts?status= | ❌ 신규 필요 |
| 글 작성 | POST /api/classes/{classId}/book-chat/posts | ❌ 신규 필요 |
| 승인/거절 처리 | PATCH /api/responses/{id}/status | ❌ 신규 필요 (reject_reason/teacher_note 같이 받음) |
| 글 삭제 | DELETE /api/responses/{id} | ❌ 신규 필요 (실제 삭제 아님, deleted_at만 채움) |
| 개별읽기 책수다 목록/작성 | 위와 동일 패턴, mode='individual'로 구분 | ❌ 신규 필요 |

**계산 공식/규칙**:
- 다른 학생에게 보이는 조건: `status='approved' AND deleted_at IS NULL`
- 본인 글은 status와 상관없이 항상 보이되, 배지(확인 중/게시됨/반려됨)로 상태 표시

**프론트도 같이 손봐야 하는지**: **O** — 지금 `localStorage`에 대기중/승인됨/거절됨 3개 배열로만 관리 중이라, 교사가 승인/거절해도 학생 화면에 전혀 반영 안 되는 버그가 이미 확인됨.

---

### 5. 개별읽기 나의 독서보관함
**뭐 하는 기능인지**: 학생이 지금까지 완독한 책들을 모아서 보는 화면

**필요한 테이블**: 8. reading_records, 9. responses, 10. summaries

**필요한 API**:
| 뭘 하는 API인지 | 메서드/URL | 상태 |
|---|---|---|
| 완독 기록 목록 조회 | GET /api/students/{id}/individual-reading/archive | ❌ 신규 필요 |

**계산 공식/규칙**: 없음(조회 위주)

**프론트도 같이 손봐야 하는지**: **O** — 지금은 "방금 완료한 책 1권"만 보여주거나 하드코딩 예시로 대체 중이라, 여러 권이 쌓이는 진짜 보관함이 아직 없음.

---

### 6. 친구 추천 책장
**뭐 하는 기능인지**: 친구들에게 책을 추천하는 글을 쓰고 좋아요를 누르는 기능

**필요한 테이블**: 14. book_recommendations, 13. content_likes

**필요한 API**:
| 뭘 하는 API인지 | 메서드/URL | 상태 |
|---|---|---|
| 추천 목록 조회 | GET /api/students/{id}/friend-book-recommendations | ❌ 신규 필요 |
| 추천 글 작성 | POST /api/students/{id}/friend-book-recommendations | ❌ 신규 필요 |
| 좋아요 토글 | POST /api/book-recommendations/{id}/likes | ❌ 신규 필요 |

**계산 공식/규칙**:
- 좋아요 개수 = 캐시 컬럼 없이 `content_likes`를 매번 COUNT

**프론트도 같이 손봐야 하는지**: **O** — `friend-book-write.html`이 "지금은 화면 확인용이라 서버에는 저장 안 됨"이라고 스스로 밝히고 있음.

---

### 7. 나의 힘(능력치) + 보상 시스템
**뭐 하는 기능인지**: 읽기 활동을 하나 끝낼 때마다 능력치(마법력/체력/지혜/용기)가 오르는 기능

**필요한 테이블**: 11. student_stats, 12. student_stat_reward_log

**필요한 API**:
| 뭘 하는 API인지 | 메서드/URL | 상태 |
|---|---|---|
| 능력치 조회 | GET /api/students/{id}/stats | ❌ 신규 필요 (8번 게임에서도 그대로 재사용) |
| 보상 지급(중복 방지 포함) | POST /api/students/{id}/stats/rewards | ❌ 신규 필요 |

**계산 공식/규칙**:
- 보상 종류 8가지(개별읽기 7가지 + 연습읽기 완료 다리 1가지), 각 보상마다 정해진 능력치를 정해진 양만큼 증가
- 모든 능력치는 0~100 사이로 자동 제한(클램프)
- 중복 지급 방지: `UNIQUE(student_id, reward_type, instance_id)`

**프론트도 같이 손봐야 하는지**: **O** — `individual-power.js`가 전부 `localStorage`로만 관리 중. 특히 "책수다방 생각 남기기" 보상은 중복 방지 키가 매번 새로 생성되는 버그가 있어 API 연동과 함께 고쳐야 함.

---

### 8. 게임(던전) — 입장조건, 전투, 결과저장
**뭐 하는 기능인지**: 능력치를 갖고 던전에 들어가 몬스터와 싸우고, 이긴 만큼 능력치가 줄어들며 다음 던전이 열리는 기능

**필요한 테이블**: 5. dungeons, 6. dungeon_records, 11. student_stats

**필요한 API**:
| 뭘 하는 API인지 | 메서드/URL | 상태 |
|---|---|---|
| 학생별 던전 목록 + 입장 가능 여부 | GET /api/students/{id}/dungeons | ❌ 신규 필요 |
| 전투 시작 시 능력치 조회 | GET /api/students/{id}/stats | (7번과 동일 API 재사용) |
| 전투 결과 저장 | POST /api/students/{id}/dungeon-records | ❌ 신규 필요 (승리 시에만 능력치도 같이 갱신) |
| 클리어 이력 조회 | GET /api/students/{id}/dungeon-records | ❌ 신규 필요 |

**계산 공식/규칙**:
- 입장 가능 = 완독 권수 ≥ required_books AND 능력치 평균 ≥ required_stat_avg AND (선행 던전을 이미 클리어했음)
- 승리 시: 능력치 각각 = MIN(현재값, reward_stat_reset_value) — 즉 클리어할수록 다시 읽기로 채워야 함

**프론트도 같이 손봐야 하는지**: **O (범위 큼)** — `game/js/game-api.js`가 학생 ID를 무시하고 고정 능력치를 씀, 결과 저장 함수가 아예 호출 안 되는 죽은 코드, 입장 조건은 "테스트용으로 항상 통과"하게 되어 있음 — 전부 고쳐야 함.

---

### 9. 교사 대시보드 (참여도/이해도, 독서실천도/기록완성도, 오늘참여율, 확인필요학생)
**뭐 하는 기능인지**: 교사가 반 학생들이 얼마나 잘 하고 있는지 한눈에 보는 화면

**필요한 테이블**: 16. student_daily_metrics, 9. responses, 10. summaries, 4. practice_progress, 8. reading_records

**필요한 API**:
| 뭘 하는 API인지 | 메서드/URL | 상태 |
|---|---|---|
| 학생별 달성도 목록 조회 | GET /api/classes/{classId}/students/achievement?type= | ❌ 신규 필요 (기존 문서 초안을 온책읽기/개별읽기 구분하도록 보강) |
| 학생별 일별 추이 조회 | GET /api/students/{id}/daily-metrics?type=&from=&to= | ❌ 신규 필요 |
| 오늘 참여율/확인필요학생 조회 | GET /api/classes/{classId}/today-status | ❌ 신규 필요 |
| 교사 코멘트 저장 | PATCH /api/students/{id}/daily-metrics/{date}/comment | ❌ 신규 필요 |

**계산 공식/규칙**:
- 참여도 = 읽기활동완료율×0.4 + 질문만들기참여율×0.3 + 생각나누기참여율×0.3
- 이해도 = AI(good) 판정 비율
- 독서실천도 = 독서일수 점수 + 활동참여횟수 점수 (각각 최대 50점)
- 기록완성도 = 활동완료율×0.5 + AI적합성×0.5
- 오늘 참여율 = 오늘 활동한 학생 수 ÷ 전체 학생 수
- "지원 필요" 판정 = total_score < 70

**프론트도 같이 손봐야 하는지**: **O** — `book-manage.html`/`individual-reading-manage.html` 둘 다 학생 이름·숫자까지 통째로 하드코딩된 mock 데이터라 실제 연동이 하나도 없음.

---

### 10. 일별 스냅샷 자동 계산 (자정 배치)
**뭐 하는 기능인지**: 매일 자정에 그날까지의 학생 성적을 계산해서 하루치 기록으로 저장하는 뒷단 작업(화면 없음)

**필요한 테이블**: 16. student_daily_metrics (원본 데이터는 9/10/4/8/15번 테이블에서 집계)

**필요한 API**: 없음 — 학생/교사가 직접 호출하는 API가 아니라 서버가 혼자 도는 배치

**계산 공식/규칙**:
- Spring `@Scheduled(cron="0 0 0 * * *")`로 전체 학생을 돌면서 9번의 공식을 "그날까지 누적 기준"으로 계산해 INSERT
- 이미 있는 값이면 덮어쓰기(UPSERT) — `UNIQUE(student_id, metric_type, metric_date)` 활용

**프론트도 같이 손봐야 하는지**: **X** — 서버 혼자 도는 배치라 프론트가 직접 호출할 일이 없음(이 데이터를 화면에 그래프로 그리는 부분은 9번 작업에 포함).

---

## 11. 그 외 놓친 부분

- **AI 피드백 결과 저장 연동**: `FeedbackAiController`(질문/요약 AI 판정)는 이미 완성되어 있지만, 판정 결과(`good`/`need`)가 `responses.passed`/`summaries.ai_passed`에 실제로 저장되도록 연동하는 작업이 빠져 있음 — 4번(책수다방)과 9번(대시보드 이해도/기록완성도) 양쪽에서 필요.
- **`reading_records` 최초 생성 시점**: 학생이 개별읽기에서 새 책을 등록할 때 `reading_records` 행이 실제로 만들어지는 흐름을 3번 작업에서 명확히 잡아야 함(지금은 아예 없음).
- **다권 완독 흐름**: 책 1권을 다 읽고 다음 책으로 넘어갈 때 새 `reading_records`가 자동으로 만들어지는지, 아니면 학생이 수동으로 "새 책 등록"을 눌러야 하는지 — 3번/5번에 걸친 문제라 별도 확인 필요(아래 섹션에도 추가).
- **로그아웃**: `POST /api/auth/logout`이 문서화는 되어 있으나, JWT는 보통 서버가 상태를 안 들고 있어서(stateless) 클라이언트가 토큰만 지우면 되는 경우가 많음 — 실제로 서버 쪽 작업이 필요한지부터 확인 필요.

---

## 확인/논의 필요 (다음에 따로 논의)

- 교사가 학생 계정 비밀번호를 초기화/재발급하는 기능이 필요한지
- 학생이 반을 옮기거나, 교사가 여러 학급을 동시에 운영할 때의 흐름
- JWT 토큰 만료 시간, 재발급 정책
- 표지 이미지/책수다방 첨부 이미지를 지금처럼 base64로 DB에 그대로 넣을지, 나중에 별도 파일 저장소로 옮길지
- "이번 차시 읽을 범위"가 학급 전체 공통 1개 값인 게 맞는지, 아니면 학생 개인별로 달라야 하는 경우가 있는지
- 다권 완독 시 `reading_records`를 새로 만드는 정확한 트리거 시점(예: "이전 책 완료 처리" 버튼이 따로 필요한지)
- 교사 학급/학생 등록(1번) 시 아이디 중복, 비밀번호 규칙 등 세부 검증 정책
