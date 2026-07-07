# API 엔드포인트 목록 (초안)

> **문서 성격**: 지금까지 frontend/game 폴더 조사에서 발견한 "서버 연동 준비 코드"(TODO 주석, 함수 시그니처, localStorage/sessionStorage 사용 패턴)를 근거로 향후 만들어야 할 API를 정리한 문서입니다.
> **아직 DB 테이블이 최종 확정되지 않았으므로, 요청/응답 데이터의 필드명과 구조는 전부 대략적인 형태이며 "추후 테이블 확정 시 조정 필요"합니다.**
> 이 문서는 설계 참고용이며, 실제 Controller 코드는 아직 만들지 않았습니다.

## 근거 자료 요약

- `game/js/game-api.js` — 주석: "나중에 GET /api/dungeons로 교체 예정", "나중에: GET /api/students/{studentId}/game-state"
- `game/js/game-state.js` — `toSaveData()` 주석: "나중에: PUT /api/students/{studentId}/game-state"
- `game/js/dungeon-ui.js` — TODO 주석: "백엔드 연동 시... PUT /api/students/{studentId}/game-state로 전송해야 함"
- `frontend/js/individual-power.js` — 능력치 저장/보상이력(`givePowerRewardOnce`, `hasRewardHistory`) 로직이 향후 백엔드로 이관될 예정
- `frontend/teacher/teacher-register.html` — `finishRegister()`에서 teacher/classInfo/students를 한 번에 저장 → 교사+학급+학생 계정 일괄 등록 API 필요
- `frontend/js/auth.js` — 현재 mock 로그인(`"mock-token"`) → 실제 로그인/인증 API 필요 (조사 방법 2번 항목)
- 나머지 도메인(책 투표, 학급/개별 독서활동, 문답, 좋아요, 교사 대시보드 등)은 1차~3차 조사에서 확인된 localStorage/sessionStorage 키 구조를 근거로 함

---

## A. 인증 / 계정

| HTTP 메서드 | 엔드포인트 경로 | 설명 | 요청 데이터 | 응답 데이터 | 관련 프론트 파일 |
|---|---|---|---|---|---|
| POST | `/api/auth/login` | 학생/교사 공통 로그인 | `{id, password}` | `{token, role, name, studentId 또는 teacherId}` (추후 테이블 확정 시 조정 필요) | `frontend/js/auth.js` |
| POST | `/api/auth/logout` | 로그아웃 | 토큰(헤더) | 처리 결과 | `frontend/js/auth.js`, `frontend/student/home.html` |

## B. 교사 등록 / 학급 / 학생 관리

| HTTP 메서드 | 엔드포인트 경로 | 설명 | 요청 데이터 | 응답 데이터 | 관련 프론트 파일 |
|---|---|---|---|---|---|
| POST | `/api/teachers/register` | 교사 계정 + 학급 정보 + 학생 계정을 한 번에 등록 | `{teacher:{name,school,id,password}, classInfo:{grade,classNumber,className}, students:[{number,name,id,password}]}` (추후 테이블 확정 시 조정 필요, 특히 password는 해싱 처리 필요) | `{classId, teacherId, createdAt}` | `frontend/teacher/teacher-register.html` |
| GET | `/api/classes/{classId}/students` | 학급 소속 학생 명단 조회 | - | `[{studentId, name, number}]` | `frontend/teacher/book-select.html` (`loadStudents`) |

## C. 책(교재) / 투표

| HTTP 메서드 | 엔드포인트 경로 | 설명 | 요청 데이터 | 응답 데이터 | 관련 프론트 파일 |
|---|---|---|---|---|---|
| GET | `/api/classes/{classId}/book-candidates` | 책 투표 후보 목록 조회 | - | `[{bookId, title, summary, cover}]` | `frontend/teacher/book-select.html` |
| POST | `/api/classes/{classId}/book-candidates` | 책 투표 후보 등록/수정 (교사) | `[{title, summary, cover}]` | 처리 결과 | `frontend/teacher/book-select.html` |
| GET | `/api/classes/{classId}/book-vote/status` | 투표 진행 상태 조회(진행중/마감 등) | - | `{status}` | `frontend/teacher/book-select.html`, `frontend/student/book-vote.html` |
| PATCH | `/api/classes/{classId}/book-vote/status` | 투표 진행 상태 변경 (교사) | `{status}` | 처리 결과 | `frontend/teacher/book-select.html` |
| POST | `/api/students/{studentId}/book-vote` | 학생 투표 제출 | `{bookId}` | 처리 결과 | `frontend/student/book-vote.html` |
| GET | `/api/classes/{classId}/book-vote/records` | 학급 투표 결과 집계 조회 (교사) | - | `[{studentId, bookId}]` 또는 집계값 | `frontend/teacher/book-select.html` |
| GET | `/api/classes/{classId}/reading-range` | 학급 지정 읽기 범위 조회 | - | `{currentPage, totalPage, updatedAt}` (추후 조정 필요) | `frontend/teacher/book-manage.html` |
| POST | `/api/classes/{classId}/reading-range` | 학급 읽기 범위 설정 (교사) | `{currentPage, totalPage}` | 처리 결과 | `frontend/teacher/book-manage.html` |

## D. 독서 활동 — 학급형 (연습읽기 / 읽기 전·중·후)

| HTTP 메서드 | 엔드포인트 경로 | 설명 | 요청 데이터 | 응답 데이터 | 관련 프론트 파일 |
|---|---|---|---|---|---|
| GET | `/api/students/{studentId}/practice-progress` | 연습읽기 진행 단계 조회 | - | `{stage, ...}` (추후 조정 필요) | `before-reading-share.html`, `book-vote.html`, `class-reading.html`, `practice.html`, `question-bundle.html` |
| POST | `/api/students/{studentId}/practice-progress` | 연습읽기 진행 단계 저장 | `{stage, ...}` | 처리 결과 | 위와 동일 |
| POST | `/api/students/{studentId}/before-reading-answers` | 읽기 전 질문 답변 저장 | `[{questionId, answer}]` | 처리 결과 | `before-reading.html`, `before-reading-share.html` |
| GET | `/api/before-reading-answers/{questionId}` | 친구가 작성한 읽기 전 답변 조회(공유) | - | `{studentId, answer}` | `before-reading-share.html` |
| GET | `/api/classes/{classId}/reading-questions?date=` | 오늘 날짜 학급 읽기중 질문 목록 조회 | - | `[{questionId, studentId, question, answer}]` | `during-read.html`, `book-chat.html`, `friend-question.html` |
| POST | `/api/classes/{classId}/reading-questions` | 학급 읽기중 질문 등록 | `{question, answer}` | 처리 결과 | `during-read.html` |
| GET | `/api/students/{studentId}/reading-question-archive` | 학생별 질문 보관함 조회 | - | `[{date, questions}]` | `during-read.html` (archiveKey) |
| POST | `/api/students/{studentId}/class-reading/complete` | 학급 읽기 완료 처리 | - | 처리 결과 | `class-reading.html` |
| GET | `/api/students/{studentId}/during-reading-practice` | 읽는 중 연습 진행 조회 | - | `{progress, reviewDone, practiceDone}` | `during-reading-practice.html` |
| POST | `/api/students/{studentId}/during-reading-practice` | 읽는 중 연습 진행/완료 저장 | `{progress, reviewDone, practiceDone}` | 처리 결과 | `during-reading-practice.html` |
| GET | `/api/classes/{classId}/after-read-summaries` | 읽기 후 요약 목록 조회(학급 공유) | - | `[{summaryId, studentId, content}]` | `after-read.html` |
| POST | `/api/students/{studentId}/after-read-summaries` | 읽기 후 요약 작성 | `{content}` | `{summaryId}` | `after-read.html` |
| POST | `/api/summaries/{summaryId}/likes` | 요약 좋아요 등록/취소 (toggle) | - | `{liked: true/false}` | `after-read.html` |

## E. 독서 활동 — 개별읽기 (개인형)

| HTTP 메서드 | 엔드포인트 경로 | 설명 | 요청 데이터 | 응답 데이터 | 관련 프론트 파일 |
|---|---|---|---|---|---|
| GET | `/api/students/{studentId}/individual-reading/stage` | 오늘의 독서모험 전체 진행 단계 조회 (before/during/after 완료 여부) | - | `{stage, bookTitle, beforeDone, duringDone, afterDone}` | `today-reading-adventure.html` |
| GET | `/api/students/{studentId}/individual-reading/before` | 개별읽기 전 단계 데이터 조회 (책 정보, 질문) | - | `{bookTitle, bookAuthor, bookType, questions}` | `individual-before-reading.html` |
| POST | `/api/students/{studentId}/individual-reading/before` | 개별읽기 전 단계 저장 | `{bookTitle, bookAuthor, bookType, questions}` | 처리 결과 | `individual-before-reading.html` |
| GET | `/api/students/{studentId}/individual-reading/during` | 개별읽기 중 단계 데이터 조회 (페이지, 질문, 답글) | - | `{currentPage, totalPages, questions, replies}` | `individual-during-reading.html` |
| POST | `/api/students/{studentId}/individual-reading/during` | 개별읽기 중 단계 저장 | `{currentPage, totalPages, questions, replies}` | 처리 결과 | `individual-during-reading.html` |
| GET | `/api/students/{studentId}/individual-reading/after` | 개별읽기 후 단계 데이터 조회 (질문답변, 요약, 공유, 별점) | - | `{questionAnswers, summary, rating, sharedSummaries}` | `individual-after-reading.html` |
| POST | `/api/students/{studentId}/individual-reading/after` | 개별읽기 후 단계 저장 | `{questionAnswers, summary, rating}` | 처리 결과 | `individual-after-reading.html` |
| GET | `/api/students/{studentId}/individual-reading/archive` | 개별읽기 완료 기록(아카이브) 조회 | - | `[{bookTitle, bookAuthor, finishedAt, summary}]` | `individual-reading-archive.html` |

## F. 문답 / 책수다방 (학급형 · 개별형 공통)

| HTTP 메서드 | 엔드포인트 경로 | 설명 | 요청 데이터 | 응답 데이터 | 관련 프론트 파일 |
|---|---|---|---|---|---|
| GET | `/api/classes/{classId}/book-chat/posts?status=` | 책수다방 게시글 목록 조회 (대기/승인/반려 상태별) | - | `[{postId, studentId, content, status}]` | `book-chat-manage.html`, `book-chat.html` |
| POST | `/api/classes/{classId}/book-chat/posts` | 책수다방 게시글 작성 | `{content}` | `{postId, status: "pending"}` | `book-chat.html` |
| PATCH | `/api/book-chat/posts/{postId}/status` | 게시글 승인/반려 처리 (교사) | `{status}` | 처리 결과 | `book-chat-manage.html` |
| POST | `/api/book-chat/posts/{postId}/replies` | 게시글에 답글/댓글 작성 | `{content}` | `{replyId}` | `book-chat.html`, `friend-question.html` |
| GET | `/api/classes/{classId}/individual-book-chat/posts?status=` | 개별읽기 문답 게시글 목록 조회 (교사 관리용) | - | `[{postId, studentId, content, status}]` | `individual-book-chat-manage.html` |
| PATCH | `/api/individual-book-chat/posts/{postId}/status` | 개별읽기 문답 게시글 승인/반려 처리 | `{status}` | 처리 결과 | `individual-book-chat-manage.html` |

## G. 친구 책장 추천

| HTTP 메서드 | 엔드포인트 경로 | 설명 | 요청 데이터 | 응답 데이터 | 관련 프론트 파일 |
|---|---|---|---|---|---|
| GET | `/api/students/{studentId}/friend-book-recommendations` | 친구가 추천한 책 목록 조회 | - | `[{recommendationId, bookTitle, bookAuthor, content}]` | `friend-book-shelf.html` |
| POST | `/api/students/{studentId}/friend-book-recommendations` | 책 추천 글 작성 | `{bookTitle, bookAuthor, content}` | `{recommendationId}` | `friend-book-write.html` |
| POST | `/api/friend-book-recommendations/{recommendationId}/likes` | 추천 글 좋아요 등록/취소 (toggle) | - | `{liked: true/false}` | `friend-book-shelf.html` |

## H. 능력치 (student_stats) 및 보상 이력

| HTTP 메서드 | 엔드포인트 경로 | 설명 | 요청 데이터 | 응답 데이터 | 관련 프론트 파일 |
|---|---|---|---|---|---|
| GET | `/api/students/{studentId}/stats` | 능력치(마법력/체력/지혜/용기) 조회 — 게임 전투 시작 시 및 "나의 힘" 모달에서 사용 | - | `{magic, stamina, wisdom, courage}` (0~100, 추후 조정 필요) | `frontend/js/individual-power.js`, `game/js/game-api.js`(`getInitialPlayerState` 대체 대상) |
| POST | `/api/students/{studentId}/stats/rewards` | 독서 활동 완료 시 능력치 보상 지급 (rewardKey로 중복 지급 방지) | `{rewardKey, presetKey?}` | `{magic, stamina, wisdom, courage}` (갱신된 값) | `frontend/js/individual-power.js` (`givePowerRewardOnce`, `hasRewardHistory`) |

## I. 게임 — 던전

| HTTP 메서드 | 엔드포인트 경로 | 설명 | 요청 데이터 | 응답 데이터 | 관련 프론트 파일 |
|---|---|---|---|---|---|
| GET | `/api/dungeons` | 던전 목록 조회 (난이도, 필요 독서량, 적 스탯, 제한시간) | - | `[{dungeonId, name, difficulty, requiredBooks, enemy:{...}, timeLimit}]` | `game/js/game-api.js` (`getDungeons`, 주석: "나중에 GET /api/dungeons로 교체 예정") |
| GET | `/api/dungeons/{dungeonId}` | 던전 상세 조회 | - | `{dungeonId, name, enemy:{...}, timeLimit}` | `game/js/game-api.js` (`getDungeon`) |
| POST | `/api/students/{studentId}/dungeon-records` | 던전 전투 결과(승리/패배/시간초과) 기록 + 승리 시 능력치 반영 | `{dungeonId, result: "victory"/"defeat"/"timeout"}` | `{recordId, updatedStats}` (승리 시에만 stats 갱신) | `game/js/dungeon-ui.js` (TODO 주석), `game/js/game-state.js` (`toSaveData`) |
| GET | `/api/students/{studentId}/dungeon-records` | 학생별 던전 클리어 이력 조회 (재도전/중복보상 방지, 입장조건 판단용) | - | `[{dungeonId, result, clearedAt}]` | `game/js/dungeon-ui.js` (재도전 버튼 관련, 현재는 이력 체크 로직 없음) |

## J. 교사 대시보드

| HTTP 메서드 | 엔드포인트 경로 | 설명 | 요청 데이터 | 응답 데이터 | 관련 프론트 파일 |
|---|---|---|---|---|---|
| GET | `/api/classes/{classId}/students/achievement` | 학생별 집계 성취도 조회 (총점/연습/기록/문답/꾸준함 + 상태 라벨) | - | `[{studentId, name, total, practice, record, chat, consistency, state, note}]` (현재 하드코딩된 mock 구조 기반, 추후 조정 필요 — 실제 계산식/저장 방식은 다음 설계 단계에서 확정) | `frontend/teacher/individual-reading-manage.html` |

---

## 참고 — 이번 조사에서 화면은 있으나 코드가 비어있어 판단 보류한 기능

아래는 API 설계가 아직 불가능한 항목입니다. 화면 파일은 존재하지만 내용이 0줄이라 요구사항을 특정할 수 없어 이번 목록에서 제외했습니다.

- `frontend/teacher/achievement.html` (0줄)
- `frontend/teacher/feedback.html` (0줄)
- `frontend/student/challenge.html` (0줄)

향후 이 화면들이 구현되면 별도로 API를 추가 조사/정리해야 합니다.
