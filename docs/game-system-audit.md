# 게임(던전) + "나의 힘" 능력치 시스템 — 순환 구조 감사

"읽기 활동 → 능력치 획득 → 게임(던전) 입장 → 전투 → 결과 반영 → 다시 읽기 활동" 순환이 지금 코드로 실제 성립하는지 조사하고, 성립하지 않는 지점을 구체적으로 지적한 뒤 백엔드/테이블 설계를 제안한 문서입니다. `docs/db-schema-final.md`/`docs/db-schema-audit.md`의 `dungeons`/`dungeon_records`/`student_stats` 설계와 겹치는 내용은 참조만 하고, 이번에 새로 발견한 사실 위주로 작성했습니다.

**조사 방법**: `game/`(저장소 최상위) 5개 JS 파일 전체(`game-api.js`, `dungeon-ui.js`, `game-state.js`, `battle-engine.js`, `player-stats.js`) + `index.html`을 전수 정독, `frontend/js/individual-power.js`(1198줄) 전체 정독, `frontend/` 전체에서 `givePowerRewardOnce` 호출 지점을 전수조사(리포지토리 전체 grep 기준 10개 호출, 5개 파일).

**코드 수정 없음** — 이 문서는 분석/제안만 담고 있습니다.

---

## 0. 결론 먼저

**순환은 지금 코드로 전혀 성립하지 않습니다.** 4단계 연결 고리 중 **첫 번째(읽기 활동 → 능력치 획득)만 실제로 동작**하고, 나머지 세 고리(능력치 → 게임 입장 조건, 입장 → 전투 스탯 반영, 전투 결과 → 능력치 반영)는 전부 하드코딩되었거나 미구현입니다. 특히 승리 화면의 소스코드 주석에 개발자 스스로 "현재는 UI 표시만 하고 실제 데이터는 증가하지 않음"이라고 명시해뒀을 정도로, 미완성이라는 사실 자체는 코드에도 이미 기록되어 있습니다.

```
[1] 읽기 활동 → 능력치 획득     ✅ 실제 동작 (개별읽기 8곳 + 연습읽기 다리 1곳)
[2] 능력치 → 게임 입장 조건 반영  ❌ 미구현 (입장 버튼 무조건 활성화, 조건 텍스트는 표시용)
[3] 게임 입장 → 전투 스탯 반영   ❌ 미구현 (전투 공식은 진짜지만 투입값이 학생별 하드코딩 고정값)
[4] 전투 결과 → 능력치 반영     ❌ 미구현 (승/패/시간초과 모두 스탯 변화 없음, 결과 저장도 없음)
```

---

## 1. 능력치가 쌓이는 조건 — 연습읽기/개별읽기 활동 전반

### REWARD_PRESETS 전체 인벤토리 (`frontend/js/individual-power.js:68-214`)

14개 프리셋이 정의되어 있는데, 실제로 호출되는 건 **8개뿐**입니다. 나머지 6개(`individual_question_created`, `individual_feedback_pass`, `individual_after_summary_pass`, `individual_question_shared`, `individual_summary_shared`, `individual_share_success`)는 정의만 되어 있고 프로젝트 전체 어디서도 호출되지 않는 **죽은/예약된 프리셋**입니다(각 키로 전체 grep했으나 호출 0건).

### 실제 호출되는 8개 프리셋 — 화면·액션 매핑 (근거: 리포지토리 전체 grep + 각 호출 지점 코드 확인)

| 프리셋 키 | 보상 | 호출 위치 | 트리거 액션 | 도메인 |
|---|---|---|---|---|
| `practice_all_complete` | 4개 능력치 전부 +8 | `after-read.html:5769` | 간추리기(요약) 저장·공유 완료 시 | **연습읽기(유일)** |
| `individual_before_complete` | 마법력+1, 지혜+1 | `individual-before-reading.html:2469` | 읽기 전 질문 4개 모두 통과 | 개별읽기 읽기전 |
| `individual_during_questions_complete` | 마법력+1, 지혜+1 | `individual-during-reading.html:8291` | 읽기중 질문 3개 이상 작성 | 개별읽기 읽기중 |
| `individual_book_chat_post` | 용기+1 | `individual-during-reading.html:9498` | 책수다방 글 작성 | 개별읽기 책수다방 |
| `individual_thought_comment` | 용기+1 | `individual-during-reading.html:11426`(실제 실행되는 건 이 지점뿐, 아래 §1-부수발견 참고) | 책수다방 A/B 선택+이유 작성 | 개별읽기 책수다방 |
| `individual_after_questions_pass` | 마법력+1, 지혜+1 | `individual-after-reading.html:4275` | 간추리기 질문 3개 이상 AI 피드백 통과 | 개별읽기 읽기후 |
| `individual_after_complete` | 체력+3, 마법력+1, 지혜+1 | `individual-after-reading.html:4479` | 책 1권 완독(간추리기까지) | 개별읽기 읽기후 |
| `individual_friend_book_recommend` | 용기+1 | `friend-book-write.html:1528` | 친구에게 책 추천 글 작성 | 개별읽기 친구추천 |

### 연습읽기 vs 개별읽기 — 명확히 구분되는 규칙 (새로 발견)

**연습읽기(온책읽기) 쪽에는 능력치를 지속적으로 쌓는 메커니즘이 전혀 없습니다.** `before-reading.html`, `during-reading-practice.html`, `class-reading.html`, `book-chat.html`, `during-read.html` — 연습읽기의 핵심 5개 화면 전부에서 `individual-power.js` `<script>` include도, `givePowerRewardOnce` 호출도 **0건**입니다.

연습읽기 트랙에서 유일하게 능력치 시스템과 맞닿는 지점은 `after-read.html`(연습읽기의 마지막 "읽기 후" 화면)의 `practice_all_complete` 보상 1건뿐이며, 이마저도 프리셋 자체의 소스 주석(69-71행)에 성격이 명시되어 있습니다:

> "연습읽기 최종 완료 보상입니다. 연습읽기에서는 능력치를 본격적으로 쌓기보다, 읽기 후 간추리기 공유까지 끝냈을 때 **개별읽기 시작 보상**으로 지급합니다."

즉 이 보상은 "능력치 성장"이 아니라 **"개별읽기 트랙 잠금 해제용 1회성 다리(bridge)"**로 설계된 것입니다. 실제로 `individual-reading.html`(990-1010행)이 이 보상 이력(`practice_all_complete_<studentId>`) 존재 여부만으로 "책 추천 받기/오늘의 독서 모험/던전 입장" 3개 버튼의 잠금을 해제합니다 — 능력치 수치나 게임 입장 조건과는 무관하게, 순수히 "연습읽기를 끝냈는가"라는 게이트로만 쓰입니다.

**결론**: §4/§7에서 연습읽기와 개별읽기를 구분해온 것과 동일하게, 능력치 획득도 **개별읽기 전용 메커니즘**이며 연습읽기는 그 트랙에 진입하기 위한 1회성 다리 역할만 합니다.

### 상한/하한 (0~100) 재검증

`clampPowerValue()`(243-246행)가 `Math.max(0, Math.min(100, number))`로 클램핑하며, **읽을 때·적용할 때·저장할 때 3중으로 재클램핑**되어 실제로 0~100을 벗어날 수 없습니다. 다만 클램핑이 "조용히" 이뤄져서, 이미 98인 능력치에 +8 보상을 줘도 2만 오르고 나머지 6은 그냥 사라지며 — 이 손실분을 로그로 남기거나 알려주는 코드는 없습니다(사소하지만 신규 발견).

### 중복 지급 방지 로직 — 견고성 재검증 (핵심 신규 발견)

`hasRewardHistory(rewardKey)`/`addRewardHistory(rewardKey)`가 중복 방지를 담당하는데, 이 함수들이 검사하는 **`rewardKey`는 프리셋 키(`presetKey`)와 다른, 호출부마다 자유롭게 조합하는 문자열**입니다. 실제 호출부를 전수조사한 결과 4가지 패턴으로 갈립니다:

| 패턴 | 프리셋 | rewardKey 예시 | 실질적 의미 |
|---|---|---|---|
| studentId만 덧붙임(중복이지만 무해) | `practice_all_complete`, `individual_before_complete`, `individual_during_questions_complete`, `individual_after_questions_pass` | `"individual_before_complete_" + studentId` | **학생당 평생 1회** — 단순 `UNIQUE(student_id, reward_type)`로 안전하게 이관 가능 |
| 완독 횟수 카운터 삽입 | `individual_after_complete` | `"individual_after_complete_" + nextCompletedBookCount` | **책 1권마다 1회** — 프리셋명만으로 유니크 제약을 걸면 2번째 책부터 보상이 막히는 회귀 발생 |
| 책 id 삽입 | `individual_friend_book_recommend` | `"friend_book_recommend_" + newBook.id` | **추천한 책마다 1회** — 위와 동일한 문제 |
| 게시글 id 삽입 | `individual_book_chat_post` | `"book_chat_post_" + post.id` | **글마다 1회** — 위와 동일한 문제 |
| **`Date.now()` 삽입 (버그)** | `individual_thought_comment` | `"book_chat_thought_" + Date.now()` | **중복 방지가 사실상 무력화됨** — 키가 항상 고유해서 `hasRewardHistory()`가 절대 true를 반환하지 않음 |

**`individual_thought_comment`는 이미 실제 버그 상태**입니다: 밀리초 타임스탬프를 키로 쓰기 때문에 중복 지급 방지 로직 자체가 있으나마나하고, 같은 액션을 여러 번 하면 매번 용기+1이 지급됩니다.

**부수 발견 — 죽은 코드 3중 중첩**: `individual_thought_comment` 호출부가 `individual-during-reading.html`에 3곳(9675행, 10974행, 11426행) 있는데, 전부 `window.saveBookChatThought = function() {...}` 형태로 같은 전역 이름에 재할당되고 있어 **마지막(11426행) 정의만 실제로 실행**되고 앞의 2개(9675행, 10974행)는 완전히 죽은 코드입니다. 이 파일 전반에 이미 확인된 "override layer" 패턴(뒤에 오는 코드가 앞 코드를 덮어씀)이 능력치 시스템 코드에도 그대로 나타난 사례입니다.

**`student_stat_reward_log`(기존 설계, `UNIQUE(student_id, reward_type)`) 이관 가능 여부**: **4개 프리셋만 안전하게 이관 가능**하고, 나머지 3개(`individual_after_complete`, `individual_friend_book_recommend`, `individual_book_chat_post`)는 프리셋명을 그대로 `reward_type`으로 쓰면 2번째 인스턴스부터 보상이 막히는 회귀가 생깁니다. `individual_thought_comment`는 애초에 클라이언트에서도 작동하는 중복방지가 없어 이관 시 새로 설계해야 합니다. → §5 신규 테이블 제안 참고.

---

## 2. 게임(던전) 입장 조건

**현재 상태: 미구현 — 조건 텍스트는 순수 표시용, 실제 체크 로직 자체가 없음**

- `game/index.html`의 `DUNGEON_INFO`(380-428행)에 "책 10/30/50권 이상", "나의 힘 평균 20/55/85 이상" 같은 조건 문구가 있고, `game/js/game-api.js`의 `DUNGEONS`(37-88행)에 `requiredBooks: 10/30/50`이 별도로 정의되어 있습니다(기존 문서가 지적한 "두 곳 중복 하드코딩"이 그대로 재확인됨).
- **결정적 코드**(`game/index.html:430-465`, `selectDungeon()`):
  ```js
  // 개발 중 테스트용: 중급/고급 던전도 임시로 입장 가능하게 합니다.
  // 실제 제출 전에는 locked 조건을 기준으로 버튼 비활성화 처리하면 됩니다.
  enterBtn.disabled = false;
  ```
  이 코드가 **모든 던전에 대해 무조건** 실행되어, 어떤 던전이든 항상 입장 버튼이 활성화됩니다. `locked`/`lockedMessage`/`requiredBooks` 필드는 정의만 되고 코드 어디에서도 읽히지 않습니다(전체 grep 재확인).
- "나의 힘 평균"의 데이터 소스도 확인했으나, `game/` 어디에도 `individualPower_v2_<studentId>`를 읽는 코드가 없습니다(전체 grep 결과 0건) — 즉 하드코딩 비교조차 아니고, **비교 자체가 아예 존재하지 않습니다.**

---

## 3. 게임 플레이 중 능력치 사용/소모

**현재 상태: 전투 공식 자체는 실재하지만, 투입되는 스탯 값이 학생 데이터와 완전히 단절됨**

전투 데미지 공식은 실제로 존재하고 작동합니다(`game/js/player-stats.js:9-44`):
```js
calcNormalDamage(magic)  = 10 + floor(magic * 0.8)
calcHeavyDamage(magic)   = 25 + floor(magic * 1.5)
calcCritChance(magic)    = min(5 + magic * 1.2, 60)
calcMaxHp(stamina)       = 100 + stamina * 5
calcDefenseRate(wisdom)  = min(80 + floor(wisdom * 0.3), 95)
```
`magic`(공격력/크리티컬), `stamina`(최대 HP), `wisdom`(방어율)은 실제 전투 계산에 쓰입니다. 다만 **`courage`는 어떤 실행되는 공식에도 쓰이지 않습니다** — `calcHeavyCooldown(courage)`가 정의는 되어 있지만 호출부가 없고, 강공격 쿨타임은 고정된 `SKILL_COOLDOWNS` 상수 테이블을 씁니다.

**진짜 문제는 이 공식에 들어가는 값 자체**입니다. `GameState.init()` → `GameAPI.getInitialPlayerState(studentId)`(`game-api.js:96-107`)가 매번 이렇게 반환합니다:
```js
function getInitialPlayerState(studentId) {
  return { magic: 10, stamina: 10, courage: 0, wisdom: 10, books: 0 };
}
```
`studentId` 매개변수를 받지만 함수 본문에서 전혀 쓰이지 않아서, **어떤 학생이든 항상 마법력10/체력10/지혜10/용기0으로 전투를 시작**합니다. 개별읽기에서 아무리 능력치를 쌓아도 게임에는 절대 반영되지 않습니다.

적의 공격력(`enemy.normalAtk`/`heavyAtk`)도 `DUNGEONS` 배열의 고정값이라 학생 데이터와 무관합니다.

---

## 4. 게임 승리/패배 시 능력치 변화 — 그리고 순환 구조

**현재 상태: 미구현 — 승리 화면 자체 소스 주석이 "실제 데이터는 증가하지 않음"이라고 명시**

`endBattle(result)`(`dungeon-ui.js:796-811`)는 victory/defeat/timeout 3분기로 갈리지만, 어느 분기도 `magic`/`stamina`/`wisdom`/`courage`를 변경하지 않습니다. 승리 시 결과 화면 코드(`showResult()`, 813-858행)를 그대로 인용합니다:

```js
if (isWin) {
  // TODO(백엔드 연동 시): 여기서 GameState.get().player.magic += rewardAmount
  // 형태로 실제 스탯을 증가시키고, GameState.toSaveData()를
  // PUT /api/students/{studentId}/game-state 로 전송해야 함.
  // 현재는 UI 표시만 하고 실제 데이터는 증가하지 않음.
  ...
  '<div class="reward-chip-result">마법력 +' + s.player.magic + '</div>' + ...
```
심지어 이 "보상" 표시조차 증가분(delta)이 아니라 **현재(고정된) 스탯 값을 그대로 다시 보여주는 것**이라, "마법력 +10"이라고 표시돼도 실제로는 10이 오른 게 아니라 원래 갖고 있던 고정값 10을 그대로 재출력하는 것입니다 — 사용자를 오도할 수 있는 표시입니다.

`dungeons.reward_stat_reset_value`(기존 문서가 이미 설계해 둔 컬럼)는 **`game/` 코드 어디에도 등장하지 않습니다.** 프론트에서 대응하는 건 `DUNGEON_INFO`의 `rewardNote` 텍스트(예: "초급 던전 클리어 후, 중급 던전 준비 능력치 평균 10으로 다시 시작합니다")뿐이며, 이는 `#detail-reward-note`에 그대로 꽂히는 **순수 표시 문자열**이지 실행되는 로직이 아닙니다.

**결과 저장(`dungeon_records.result`)도 완전히 없습니다.** `game-state.js`의 `toSaveData()`(105-118행, `PUT /api/students/{studentId}/game-state`용으로 준비된 함수)는 정의만 되어 있고 **어디서도 호출되지 않는 죽은 코드**입니다. `game/` 전체에서 스토리지 상호작용은 다음 단 한 줄뿐입니다:
```js
// game-state.js:9
const studentId = sessionStorage.getItem('studentId') || '1';
```
이마저 **읽기(read)**일 뿐 쓰기(write)가 아니며, 게임 화면을 새로고침하면 진행 중이던 전투도 방금 끝낸 결과도 전부 사라집니다(세션 스토리지조차 아닌 순수 메모리 변수 `_state`에만 존재).

### 순환 구조가 지금 가능한가 — 막히는 지점 총정리

**불가능합니다.** 4개 연결 고리를 하나씩 짚으면:

1. **읽기 활동 → 능력치 획득**: 동작함 (§1)
2. **능력치 → 게임 입장 조건**: 막힘 — 입장 버튼이 조건과 무관하게 항상 활성화됨 (§2)
3. **게임 입장 → 전투 스탯 반영**: 막힘 — 매번 고정값(마법력10/체력10/지혜10/용기0)으로 시작 (§3)
4. **전투 결과 → 능력치 반영 → 다시 읽기로 복귀**: 막힘 — 승패 무관하게 스탯도 결과도 전혀 저장되지 않음 (§4)

즉 학생이 아무리 열심히 읽기 활동을 해서 능력치를 쌓아도, 게임에는 그 노력이 전혀 반영되지 않고, 게임을 아무리 잘해도(또는 못해도) 다시 읽기 화면으로 돌아왔을 때 달라지는 게 하나도 없습니다. "한 번 게임하면 그걸로 끝나는 구조"에 더 가깝습니다 — 정확히는 "게임이 읽기 활동과 완전히 분리된 별도의 데모/프로토타입" 상태입니다.

---

## 5. 게임과 읽기 활동 진행 흐름의 연결점

**"나의 힘" 화면 → 게임 진입 경로**: `individual-reading.html`(972-978행)의 "던전 입장" 버튼이 `game/index.html`로 순수 `location.href` 이동합니다. 이 버튼은 "나의 힘" 모달(`openIndividualPowerModal()`) **안에 있는 게 아니라**, 개별읽기 홈 화면의 별도 버튼입니다 — 즉 "나의 힘" 모달과 "던전 입장" 버튼은 같은 화면에서 독립적으로 존재하며, 서로 직접 연결되어 있지 않습니다. 이 버튼의 잠금 해제 조건도 능력치 수치가 아니라 §1에서 확인한 `practice_all_complete` 보상 이력(연습읽기 완료 여부)입니다.

**게임 → 읽기 화면 복귀**: `game/index.html:16`의 "🏠 책읽기 홈" 링크가 `individual-reading.html`로 순수 `href` 이동합니다. 쿼리 파라미터나 상태 전달이 전혀 없는 단순 페이지 이동입니다.

**게임에서 돌아왔을 때 "나의 힘" 화면에 갱신된 능력치가 반영되는가**: 애초에 게임이 아무것도 바꾸지 않으므로(§4) 반영될 대상 자체가 없습니다. 설사 나중에 연동하더라도, 지금의 순수 `location.href`/`href` 방식으로는 상태 전달이 안 되므로 게임 쪽이 `individualPower_v2_<studentId>`(또는 향후 `GET/POST /api/students/{studentId}/stats`)를 직접 읽고 쓰도록 만들어야 합니다.

**게임 진행이 §4/§7 지표(참여도, 독서실천도 등)에 영향을 주는가**: `dungeon_records`조차 저장되지 않으므로 완전히 독립적입니다 — 애초에 연결점이 없어서 "영향을 준다/안 준다"를 논할 데이터 자체가 없습니다.

---

## 6. 불필요/중복 정리 제안

- **`game-api.js`의 `DUNGEONS` vs `index.html`의 `DUNGEON_INFO` 중복**: 기존 문서 지적이 그대로 유효함을 재확인. `dungeons` 테이블로 통합 시 반드시 하나로 합쳐야 함.
- **`dungeons.reward_stat_reset_value`**: `db-schema-final.md`의 설계 원칙("코드에 실제 근거가 없는 컬럼은 넣지 않음")에 비춰보면 이 컬럼도 재검토 대상입니다. 현재 코드엔 "클리어 후 능력치 평균을 특정 값으로 리셋한다"는 로직이 전혀 없고, `rewardNote`라는 순수 텍스트로만 그 의도가 표현되어 있습니다. 완전히 삭제하기보다는, §7에서 제안하는 대로 **실제로 구현할 계획이 있는 필드임을 명시**하고 프론트 연동 시 반드시 실행 로직을 붙이는 조건으로 유지할 것을 제안합니다(단순 삭제하면 "클리어 후 리셋" 게임 디자인 의도 자체가 문서에서 사라짐).
- **`student_stat_reward_log`의 `UNIQUE(student_id, reward_type)` 제약**: §1에서 확인했듯 8개 프리셋 중 3개(`individual_after_complete`, `individual_friend_book_recommend`, `individual_book_chat_post`)에는 이 제약이 그대로 맞지 않습니다. 아래 §7에서 컬럼 추가를 제안합니다(기존 `db-schema-final.md`/`db-schema-audit.md` 설계에 대한 수정 제안).

---

## 7. 신규 테이블/컬럼 제안

### `student_stat_reward_log` — 컬럼 추가 (기존 설계 수정)

| 컬럼명 | 타입 | 설명 | 비고 |
|---|---|---|---|
| instance_id | VARCHAR(100) | 반복 가능한 보상의 인스턴스 식별자(책 id, 게시글 id, 완독 순번 등). "학생당 평생 1회" 보상은 빈 문자열(`''`)로 통일 | **신규 컬럼**, MySQL에서 `NULL`은 UNIQUE 제약에서 여러 개가 허용되므로 반드시 `NOT NULL DEFAULT ''`로 설계 |
| — | — | `UNIQUE(student_id, reward_type)` → **`UNIQUE(student_id, reward_type, instance_id)`로 변경** | 제약 수정 |

이렇게 하면: `individual_before_complete`류 4개는 `instance_id=''`로 학생당 1회 그대로 보장되고, `individual_after_complete`(책 완독 순번)/`individual_friend_book_recommend`(책 id)/`individual_book_chat_post`(게시글 id)는 `instance_id`에 각각의 식별자를 넣어 "인스턴스마다 1회"가 정확히 보장됩니다.

`individual_thought_comment`는 프론트에서 `Date.now()` 대신 실제 댓글/생각 id를 `instance_id`로 넘기도록 **프론트 수정이 별도로 필요**합니다(이번 문서는 분석만이라 코드는 안 고쳤지만, 버그로 명확히 기록해둡니다).

### 게임 ↔ 능력치 연동 — 신규 테이블 불필요, 기존 테이블 재사용 + API 연동만 필요

새 테이블을 만들 필요는 없습니다. `student_stats`(이미 설계됨: magic/stamina/wisdom/courage)와 `dungeon_records`(이미 설계됨: student_id/dungeon_id/result/played_at)만으로 순환 구조를 지원할 수 있습니다. 필요한 건 **프론트-백엔드 연동 코드**이며, `docs/api-endpoints.md`가 이미 정확히 이 형태로 설계해 뒀음을 재확인했습니다:

- `GET /api/students/{studentId}/stats` — 게임 시작 시 `GameAPI.getInitialPlayerState()`가 하드코딩 대신 이 API를 호출하도록 교체
- `POST /api/students/{studentId}/dungeon-records` — 응답에 "승리 시에만 stats 갱신"이 이미 명시되어 있음(§I). `endBattle('victory')` 분기에서 이 API를 호출하도록 연동
- `GET /api/students/{studentId}/dungeon-records` — 던전 입장 조건("이전 던전 클리어했는지") 체크에 사용

**승리 시 스탯 반영 공식 제안** (`reward_stat_reset_value`의 실제 의미를 코드로 옮기면): `rewardNote` 문구("초급 던전 클리어 후, 중급 던전 준비 능력치 평균 10으로 다시 시작")를 그대로 해석하면, 이 게임은 "능력치를 계속 쌓기만 하는" 구조가 아니라 **"던전 클리어 후 평균 능력치를 `reward_stat_reset_value`로 리셋해서, 다음 던전을 위해 다시 읽기 활동으로 쌓아야 하는" 반복 유도형 구조**로 설계된 것으로 보입니다. 이 디자인 의도가 실제로 맞는지는 기획 확인이 필요하지만(§8 확인 필요 항목), 맞다면:
```
승리 시: 4개 능력치 각각 = MIN(현재값, dungeons.reward_stat_reset_value)
  (즉 상한을 낮춰서 "리셋"하되, 이미 그보다 낮으면 그대로 둠)
```
이 방식이어야 "게임 후 다시 읽기 활동으로 능력치를 쌓아야 다음 던전에 갈 수 있는" 반복 순환이 게임 디자인 의도와 일치하게 됩니다.

---

## 8. 확인 필요 항목

- §4/§7: "던전 클리어 후 능력치 평균을 리셋한다"는 `rewardNote` 문구의 게임 디자인 의도가 정확히 맞는지, 아니면 그냥 보상 문구를 잘못 쓴 것이고 실제로는 순수 증가형으로 가야 하는지 기획 확인 필요.
- §1: 죽은 프리셋 6개(`individual_question_created` 등)가 향후 구현 예정 기능인지, 완전히 폐기해도 되는 코드인지 확인 필요.
- §1: `individual_thought_comment`의 `Date.now()` 중복 방지 버그 — 코드 수정이 필요한 사안이므로 별도 작업으로 처리할지 확인 필요(이번 문서는 분석만 진행).
- §2: "책 N권 이상" 조건에서 "책"이 무엇을 세는 것인지(개별읽기 완독 권수인지, 등록한 책 권수인지) — `GameAPI.getInitialPlayerState()`의 `books: 0` 필드명으로 미루어 "등록/완독한 책 수"로 추정되나 실제 카운트 로직이 없어 확정 못함.

---

## 9. 학생이 실제로 겪는 전체 흐름 (현재 상태 그대로)

1. 학생이 연습읽기(온책읽기)를 시작해 읽기 전→읽기 중→읽기 후를 진행한다. 이 과정에서는 **능력치가 전혀 오르지 않는다** (§1).
2. `after-read.html`에서 간추리기를 저장하는 순간, 4개 능력치가 전부 +8 오르고(`practice_all_complete`), 이 보상 이력이 "개별읽기 잠금 해제 열쇠"로 저장된다.
3. 학생이 `individual-reading.html`(개별읽기 홈)으로 이동하면, 방금 생긴 보상 이력 덕분에 "책 추천 받기/오늘의 독서 모험/던전 입장" 버튼이 열린다.
4. 개별읽기 읽기 전·중·후, 책수다방, 친구추천 활동을 할 때마다 능력치가 조금씩 오른다(§1의 8개 프리셋). "나의 힘" 모달을 열면 이 값이 실시간으로 보인다.
5. 학생이 "던전 입장" 버튼을 눌러 `game/index.html`로 이동한다. **이 시점부터는 앞서 쌓은 능력치와 완전히 무관해진다.**
6. 던전 선택 화면에서 "책 30권 이상", "나의 힘 평균 55 이상" 같은 조건 문구가 보이지만, 실제로는 어떤 던전을 골라도 입장 버튼이 항상 눌린다.
7. 전투에 들어가면 학생은 항상 마법력10/체력10/지혜10/용기0으로 싸운다 — 지금까지 읽기 활동으로 쌓은 실제 능력치가 몇이든 상관없다.
8. 승리하든 패배하든 시간초과든, 결과 화면에 문구만 다르게 뜨고 **능력치도, 클리어 기록도 아무것도 저장되지 않는다.**
9. "🏠 책읽기 홈" 버튼으로 개별읽기 홈에 돌아오면, 게임을 하기 전과 완전히 동일한 상태다 — "나의 힘" 수치도, 던전 클리어 이력도, 아무 변화가 없다. 다시 읽기 활동을 해서 능력치를 쌓아도 되지만, 그것이 "다음 던전 도전"과 연결된다는 보장이나 표시가 전혀 없다.

---

## 10. 실제로 작동하게 만들기 위한 설계 (§1~§9 감사 결과에 대한 구현 설계)

§1~§9가 "무엇이 가짜인지"를 확인한 감사였다면, 이 섹션은 "어떻게 진짜로 만들지"에 대한 설계입니다. 5개 항목 모두 **결론부터 말하면, 새 테이블은 거의 필요 없고 기존 `db-schema-final.md`/`api-endpoints.md` 설계를 실제로 연동하는 작업**입니다 — 이 프로젝트에서 반복적으로 확인되는 패턴(설계 문서는 앞서 있고 프론트 연동만 빠짐)이 게임 시스템에도 그대로 나타납니다.

### 10-1. 능력치 획득 경로 — 개별읽기 누적분 + 연습읽기 보너스(+8) 합산

**설계 결론: 별도 합산 로직이 필요 없습니다.** §1에서 확인했듯 `practice_all_complete`(연습읽기 완료 보너스)와 개별읽기 8개 프리셋은 이미 **동일한 파이프라인**(`applyRewardValues()` → `clampPowerValue()` → `savePowerState()`)을 거쳐 **같은 4개 숫자**(`magic`/`stamina`/`wisdom`/`courage`)에 누적됩니다. 백엔드로 옮겨도 마찬가지로 둘 다 `student_stats`의 같은 행을 갱신하면 됩니다.

즉 "게임 참여 가능 능력치"는 **`student_stats.magic/stamina/wisdom/courage`를 그대로 읽으면 그 자체로 이미 개별읽기 누적분 + 연습읽기 보너스가 합쳐진 값**입니다. 이걸 "정식 인정"한다는 것은 실질적으로 **"게임 입장 조건 체크 시 능력치 출처를 따지지 않고 `student_stats`의 현재 값을 그대로 쓴다"**는 설계 결정을 명문화하는 것입니다.

| 항목 | 설계 |
|---|---|
| 신규 테이블 | 없음 — `student_stats` 그대로 재사용 |
| 신규 API | 없음 — `GET /api/students/{studentId}/stats`(기존 §H) 재사용 |
| 계산 로직 | `게임참여능력치평균 = ROUND((magic + stamina + wisdom + courage) / 4)` — 이 값을 §10-2의 입장 조건 비교에 사용 |

### 10-2. 게임 입장 조건 — 실제 체크되도록

**"책 N권" 정의 확정 제안** (§8에서 확인 필요로 남겼던 항목): `GameAPI.getInitialPlayerState()`의 `books` 필드 주석("등록한 책 수")과 실제 게임 디자인 의도(입장 조건이니 "진짜로 읽었는지"를 봐야 함) 사이에 간극이 있습니다. 단순 등록만으로 조건을 만족시킬 수 있으면 입장 조건으로서 의미가 없으므로, **완독 기준**을 제안합니다:
```sql
완독권수 = COUNT(reading_records WHERE student_id=? AND finished_at IS NOT NULL)
```
(`dashboard-metrics-audit.md` §3-B의 "월별 완독 기록" 집계와 동일한 기준 재사용 — 두 기능이 같은 정의를 공유하게 되어 일관성도 확보됩니다.)

**입장 가능 여부 판정 공식**:
```
입장가능 =
  완독권수 >= dungeons.required_books
  AND 게임참여능력치평균 >= dungeons.required_stat_avg
  AND (선행 던전이 없거나, dungeon_records에 선행 던전 result='victory' 기록이 있음)
```

세 번째 조건("선행 던전 클리어")은 지금 `DUNGEON_INFO`의 `locked`/`lockedMessage`(예: "초급 던전 클리어 후 입장 가능")가 표현하려던 것인데, 코드에는 이 선후 관계를 나타내는 컬럼이 없어 배열 순서로만 암묵적으로 존재합니다. **명시적 컬럼으로 뽑아내는 것을 제안합니다.**

**신규 컬럼: `dungeons.prerequisite_dungeon_id`**

| 컬럼명 | 타입 | 설명 | 비고 |
|---|---|---|---|
| prerequisite_dungeon_id | BIGINT | FK→dungeons.id, 이 던전에 입장하려면 먼저 클리어해야 하는 던전. 최초 던전(초급)은 NULL | **신규 컬럼**, NULL 허용 |

**신규 API: `GET /api/students/{studentId}/dungeons`**

기존 `GET /api/dungeons`(전체 던전 목록, 학생 무관)와 별개로, **학생별 입장 가능 여부까지 서버에서 계산해 내려주는 목록 API**를 추가할 것을 제안합니다. 클라이언트에서 조건 비교 로직을 중복 구현하면(그리고 개발 중 "테스트용" 우회처럼 다시 비활성화하기 쉬우면) §2에서 확인한 것과 같은 사고가 재발할 여지가 있어, **판정 자체를 서버 책임으로 옮기는 편이 안전**합니다.

```
GET /api/students/{studentId}/dungeons
응답: [
  {
    dungeonId, name, requiredBooks, requiredStatAvg,
    studentBookCount, studentStatAvg,
    eligible: boolean,
    reason: "requiredBooks" | "requiredStatAvg" | "prerequisite" | null
  }, ...
]
```

**프론트 교체 방향(설계만, 코드 수정은 안 함)**: `game/index.html`의 `selectDungeon()`에 있는 다음 코드—
```js
// 개발 중 테스트용: 중급/고급 던전도 임시로 입장 가능하게 합니다.
enterBtn.disabled = false;
```
—를 제거하고, 위 API 응답의 `eligible` 값으로 `enterBtn.disabled`를 설정하도록 교체해야 합니다. `req`(조건 표시 텍스트)도 하드코딩 문자열 대신 API가 내려주는 `requiredBooks`/`requiredStatAvg`와 `studentBookCount`/`studentStatAvg`를 조합해 "몇 권 더 읽어야 하는지"까지 보여줄 수 있습니다.

### 10-3. 전투에 실제 능력치 반영

`game/js/game-api.js`의 `getInitialPlayerState(studentId)`가 지금은 `studentId`를 무시하고 고정값을 반환합니다. 이 함수가 **기존에 이미 설계된 `GET /api/students/{studentId}/stats`(§H)를 호출**하도록 바뀌면 됩니다 — 신규 API 불필요.

```
GameAPI.getInitialPlayerState(studentId)
  → GET /api/students/{studentId}/stats
  → { magic, stamina, wisdom, courage }를 그대로 player 초기값에 대입
  → books(완독권수)는 10-2에서 쓰는 값을 그대로 재사용해 HUD에 표시(선택)
```

| 항목 | 설계 |
|---|---|
| 신규 테이블 | 없음 |
| 신규 API | 없음 — `GET /api/students/{studentId}/stats` 재사용 |
| 확인 필요 | `courage`가 현재 어떤 실행되는 전투 공식에도 안 쓰이는 문제(§3)는 스키마/API 이슈가 아니라 `game/js/player-stats.js`의 `calcHeavyCooldown(courage)`를 실제로 호출부에 연결하는 프론트 로직 문제 — 이번 설계 범위(테이블/API) 밖이라 별도 프론트 작업으로 남겨둠 |

### 10-4. 게임 결과 저장 및 반복 가능한 순환 구조

**`dungeon_records` 재확인 결과: 보정 없이 그대로 재사용 가능합니다.** 스키마에 학생-던전 조합을 유일하게 강제하는 제약(`UNIQUE` 등)이 없어서, 같은 학생이 같은 던전에 여러 번 도전한 기록이 여러 행으로 자연스럽게 쌓입니다. "1회성 이벤트"가 되는 원인은 테이블 설계가 아니라, §4에서 확인한 대로 **애초에 결과 저장 API 호출 자체가 없었기 때문**입니다.

**연동 설계**:
```
전투 종료(endBattle) 시:
  POST /api/students/{studentId}/dungeon-records
  body: { dungeonId, result: "victory" | "defeat" | "timeout" }
  응답: { recordId, updatedStats }  ← 이미 api-endpoints.md §I에 "승리 시에만 stats 갱신"으로 명시된 설계 그대로
```

서버 처리 순서(기존 §I 설계를 구체화):
1. `dungeon_records`에 결과 행 INSERT (승패 무관하게 항상 기록 — 재도전 이력을 위해 패배/시간초과도 남겨야 함)
2. `result='victory'`인 경우에만 §7에서 제안한 리셋 공식 적용:
   ```
   student_stats 각 능력치 = MIN(현재값, dungeons.reward_stat_reset_value)
   ```
3. 갱신된(또는 변경 없는) `student_stats` 값을 응답에 담아 프론트로 반환 → 프론트가 이 값으로 "나의 힘" 표시와 게임 내 HUD를 즉시 갱신

**반복 순환이 되는 구조**: 위 2번 공식이 핵심입니다. 승리하면 능력치가 `reward_stat_reset_value`(예: 초급 클리어 후 10)까지 낮아지므로, 학생은 자연히 다시 읽기 활동으로 돌아가 능력치를 채워야 다음 던전(중급, 조건 평균 55) 조건을 만족할 수 있습니다. **같은 던전 재도전**은 막을 이유가 없으므로(연습 삼아 다시 싸우고 싶을 수 있음) 허용하고, **다음 던전 해금**은 10-2에서 추가한 `prerequisite_dungeon_id` + `dungeon_records`에 해당 던전 `victory` 행 존재 여부로 판정합니다.

| 항목 | 설계 |
|---|---|
| 신규 테이블 | 없음 — `dungeon_records` 그대로 재사용 |
| 신규 API | 없음 — `POST/GET /api/students/{studentId}/dungeon-records`(기존 §I) 재사용 |
| 변경 필요 | 프론트가 `toSaveData()`/전투 종료 시점에 위 API를 실제로 호출하도록 연동(현재 미호출) |

### 10-5. 보상 시스템 버그 보정

§7에서 이미 제안한 `student_stat_reward_log.instance_id` 컬럼 + `UNIQUE(student_id, reward_type, instance_id)` 제약을 그대로 최종안으로 확정합니다. 이번엔 구체적인 **호출부별 `instance_id` 매핑표**로 정리합니다(실제 코드 수정 방향 제안, 이번 문서에서 코드는 안 고침):

| 프리셋 | instance_id 값 | 비고 |
|---|---|---|
| `practice_all_complete` | `''`(빈 문자열) | 학생당 평생 1회 |
| `individual_before_complete` | `''` | 학생당 평생 1회 |
| `individual_during_questions_complete` | `''` | 학생당 평생 1회 |
| `individual_after_questions_pass` | `''` | 학생당 평생 1회 |
| `individual_after_complete` | 완독한 책의 `reading_records.id` (또는 완독 순번) | 책마다 1회 |
| `individual_friend_book_recommend` | 추천한 책의 id(`book_recommendations.id` 또는 프론트 임시 id) | 추천 책마다 1회 |
| `individual_book_chat_post` | 작성한 게시글의 id(`responses.id` 또는 프론트 임시 id) | 게시글마다 1회 |
| `individual_thought_comment` | **작성한 댓글/생각 자체의 저장된 id** (현재처럼 매 호출 시 `Date.now()`를 새로 만드는 게 아니라, 그 댓글 객체가 생성될 때 이미 부여된 id를 재사용) | 댓글마다 1회 — 지금은 이 부분이 버그로 무력화되어 있음(§1) |

`individual_thought_comment` 수정 방향: 현재 `"book_chat_thought_" + Date.now()`처럼 **보상을 지급하는 그 순간** 타임스탬프를 새로 찍는 게 문제입니다. 올바른 방향은 댓글 저장 로직이 댓글 객체를 만들 때 이미 고유 id를 부여하고 있을 것이므로(있다면 그 id, 없다면 그 시점에 한 번만 생성해 댓글과 함께 저장), 보상 호출 시엔 **그 저장된 id를 재사용**해야 합니다. 또한 같은 파일에 같은 프리셋을 부르는 죽은 코드 2곳(9675행, 10974행)은 실제 실행되지 않으므로 이번 설계와 무관하지만, 향후 코드 정리 시 함께 제거 대상입니다.

`POST /api/students/{studentId}/stats/rewards`(기존 §H)의 요청 바디도 이 설계에 맞춰 `{rewardKey, presetKey?}` → **`{presetKey, instanceId}`**로 다듬는 것을 제안합니다(`rewardKey` 안에 studentId를 또 넣던 관행은 백엔드에서 어차피 `student_id` 컬럼으로 식별되므로 불필요해짐).

---

## 11. 신규/변경 테이블 · API 총정리

### 신규/변경 테이블

| 테이블 | 변경 | 컬럼 | 설명 |
|---|---|---|---|
| `student_stat_reward_log` | 컬럼 추가(§7 최종 확정) | `instance_id VARCHAR(100) NOT NULL DEFAULT ''` | 반복 가능한 보상의 인스턴스 식별자 |
| `student_stat_reward_log` | 제약 변경 | `UNIQUE(student_id, reward_type)` → `UNIQUE(student_id, reward_type, instance_id)` | §10-5 매핑표대로 인스턴스별 중복 방지 |
| `dungeons` | 컬럼 추가(신규) | `prerequisite_dungeon_id BIGINT NULL, FK→dungeons.id` | 선행 던전 클리어 조건을 배열 순서가 아닌 명시적 관계로 표현 |
| `dungeon_records` | 변경 없음 | — | 이미 다건 저장 가능한 구조, 재확인만 완료 |
| `student_stats` | 변경 없음 | — | 개별읽기·연습읽기 보상이 이미 같은 컬럼에 합산되는 구조 재확인 |

### API 재사용 vs 신규

| 기능 | API | 상태 |
|---|---|---|
| 게임 시작 시 능력치 조회 | `GET /api/students/{studentId}/stats` | **기존 재사용**(§H) — `getInitialPlayerState()`가 이걸 호출하도록 교체 |
| 보상 지급(중복 방지 포함) | `POST /api/students/{studentId}/stats/rewards` | **기존 재사용, 요청 바디만 보정**(§H) — `instanceId` 필드 추가 |
| 던전 목록 + 학생별 입장 가능 여부 | `GET /api/students/{studentId}/dungeons` | **신규** — 유일하게 새로 필요한 엔드포인트 |
| 던전 결과 저장 + 승리 시 스탯 반영 | `POST /api/students/{studentId}/dungeon-records` | **기존 재사용**(§I) — 이미 "승리 시에만 stats 갱신" 설계됨, 프론트 연동만 필요 |
| 클리어 이력 조회(선행조건/재도전 판단) | `GET /api/students/{studentId}/dungeon-records` | **기존 재사용**(§I) |

**결론: 신규 엔드포인트는 `GET /api/students/{studentId}/dungeons` 단 1개뿐**이고, 나머지는 이미 `docs/api-endpoints.md`에 설계돼 있던 것을 그대로 연동하면 됩니다. 이 프로젝트 전반에서 반복적으로 나타나는 패턴(설계는 있는데 프론트가 안 씀)이 게임 시스템에도 정확히 적용됩니다.

---

## 12. 학생이 게임에 반복 참여하는 전체 흐름 (설계 반영 후)

1. 학생이 연습읽기(온책읽기) 읽기 후를 완료 → `student_stats` 4개 능력치 +8, `student_stat_reward_log`에 `practice_all_complete`(instance_id='') 기록 → 개별읽기 트랙 잠금 해제.
2. 개별읽기 읽기 전·중·후, 책수다방, 친구추천 등을 하며 `student_stats`가 계속 누적(§1의 8개 프리셋, 각각 `instance_id`로 중복 방지).
3. 학생이 "던전 입장" 화면에 진입 → 프론트가 `GET /api/students/{studentId}/dungeons` 호출 → 서버가 완독권수·능력치평균·선행던전 클리어 여부를 비교해 `eligible` 계산 → 조건 미달 던전은 입장 버튼이 실제로 비활성화됨.
4. 조건을 만족한 던전에 입장 → `game/index.html`이 `GET /api/students/{studentId}/stats`로 그 학생의 **실제** 능력치를 받아와 전투 시작값으로 사용.
5. 전투 진행 — magic/wisdom/stamina가 실제 학생 데이터 기반으로 데미지/방어/HP에 반영됨.
6. 전투 종료 → `POST /api/students/{studentId}/dungeon-records`로 결과(victory/defeat/timeout) 저장.
   - 승리: 서버가 `student_stats`를 `MIN(현재값, reward_stat_reset_value)`로 낮추고, 갱신된 값을 응답으로 돌려줌 → "나의 힘" 화면과 게임 HUD가 즉시 낮아진 값을 반영.
   - 패배/시간초과: 기록만 남고 능력치 변화 없음 — 같은 던전 재도전 가능.
7. 학생이 "🏠 책읽기 홈"으로 복귀 → 낮아진(또는 그대로인) 능력치를 보고, 다시 개별읽기/연습읽기 활동으로 능력치를 채운다.
8. 능력치가 다음 던전의 `required_stat_avg`를 넘고 선행 던전 `victory` 기록이 쌓이면, 2~7단계가 반복되며 다음 던전으로 자연스럽게 이어진다.

이 흐름이 되면 "읽기 활동 → 능력치 획득 → 게임 입장 → 전투 → 결과 반영 → 다시 읽기 활동"이 실제로 끊기지 않는 반복 구조가 됩니다.

---

## 13. 능력치 기반 공격/방어 조건 및 소모 계산 설계

§3(전투에 실제 능력치 반영)·§10-3(전투 스탯 반영 설계)에 이어지는 내용입니다. §3에서는 "능력치가 데미지 공식에 쓰이는가"만 확인했는데, 이번엔 `frontend/student/ability-intro.html`(능력치 안내 화면)이 이미 학생에게 약속하고 있는 "능력치가 높을수록 쓸 수 있는 기술이 늘어난다"는 기획 의도를 실제 게임 코드와 대조해 계산 가능한 설계로 정리합니다.

### 13-1. 게임 코드에 이미 있는 기술 목록 — 그리고 안내 문구와의 불일치 (신규 발견)

`game/js/battle-engine.js`에 정의된 스킬은 총 **6개**입니다(전체 정독 확인, 41-125행):

| 스킬 키 | 이름 | 유형 | 계산에 쓰는 능력치 | 비고 |
|---|---|---|---|---|
| `skillIlgyeok` | 일격 | 기본 공격 | 마법력 (`calcNormalDamage`, 크리티컬 포함) | |
| `skillYeonsoek` | 연속베기 | 2타 공격 | 마법력 (`calcNormalDamage` 기반 1타×0.75 + 2타×0.85) | |
| `skillBangeo` | 방어 | 방어 | **지혜** (`calcDefenseRate`) | |
| `skillCheolbyeok` | 철벽 | 완전 방어(데미지 0) | **없음** — 매개변수 자체가 없어 무조건 100% 방어 | |
| `skillBulkkot` | 불꽃베기 | 강한 공격 | 마법력 (`calcHeavyDamage`) | |
| `skillHwayeom` | 화염폭발 | 초강력(필살기) | 마법력 (`calcHeavyDamage`×2) | 쿨타임 김(`SKILL_COOLDOWNS`, 고정값) |

**`ability-intro.html`(174-301행)의 안내 문구와 대조하면 불일치가 확인됩니다**:

| 능력치 | 안내 문구가 약속하는 효과 | 실제 코드가 쓰는 효과 | 일치 여부 |
|---|---|---|---|
| 마법력 | "기본 베기와 연속 공격이 더 힘있어져요" | 일격 + 연속베기 (✓ 일치) **그리고 불꽃베기 + 화염폭발도** (문구엔 없음) | 부분 일치 — 실제로는 4개 공격 스킬 전부가 마법력만 씀 |
| 체력 | "생명력이 늘어나서... 더 오래 버틸 수 있어요" | `calcMaxHp(stamina)` | 일치 |
| 지혜 | "강한 공격과 초강력 필살기를 더 잘 쓸 수 있어요" | 실제로는 **방어(`skillBangeo`)에만** 쓰임, 불꽃베기/화염폭발과는 무관 | **불일치** |
| 용기 | "방어와 철벽으로 용의 공격을 막을 수 있어요" | 실제로는 **어떤 스킬에도 안 쓰임** — 방어는 지혜가 담당, 철벽은 매개변수 자체가 없음 | **불일치** |

즉 코드상으로는 "마법력이 공격 4종을 전부 담당하고, 지혜는 방어 1종만 담당하며, 용기는 완전히 놀고 있는" 불균형한 상태인데, 안내 문구는 "마법력=기본 공격 2종 / 지혜=강공격 2종 / 용기=방어 2종 / 체력=HP"라는 훨씬 균형 잡힌 4분할 구조를 이미 약속하고 있습니다. §3에서 지적한 "용기가 어떤 공식에도 안 쓰인다"는 문제가, 사실은 안내 문구 쪽이 원래 의도한 정상 설계이고 게임 코드 쪽이 그 설계를 아직 못 따라간 상태라는 것이 이번에 명확해졌습니다.

**설계 제안**: 안내 문구(기획 의도)를 기준으로 스킬-능력치 매핑을 재정렬할 것을 제안합니다. 아래 §13-2부터는 이 재정렬된 매핑을 기준으로 설계합니다.

### 13-2. 능력치 구간별 사용 가능 기술표 (제안 — 안내 문구 의도에 맞춰 재정렬)

기존 `dungeons.required_stat_avg`(초급 20 / 중급 55 / 고급 85)와 자연스럽게 맞물리도록, 스킬 해금 구간도 같은 척도(0~100)로 설계했습니다.

| 능력치 | 스킬 | 해금 조건 | 비고 |
|---|---|---|---|
| 마법력 | 일격 | 항상 사용 가능 (0 이상) | 기본기 — 능력치가 0이어도 최소한의 공격 수단 보장 |
| 마법력 | 연속베기 | 마법력 30 이상 | |
| 지혜 | 방어 | 항상 사용 가능 (0 이상) | 기본 방어 — 최소한의 생존 수단 보장 |
| 지혜 | 불꽃베기 *(재정렬)* | 지혜 30 이상 | 기존 코드는 마법력 기준이었으나 §13-1 제안대로 지혜 기준으로 이관 |
| 용기 | 철벽 *(재정렬)* | 용기 50 이상 | 기존 코드는 무조건 사용 가능이었으나, 완전방어라는 강력함에 맞춰 해금 조건 신설 |
| 지혜 | 화염폭발 *(재정렬)* | 지혜 60 이상 | 필살기 — 가장 늦게 열림 |
| 체력 | (스킬 아님, HP 계산에 상시 반영) | 해당 없음 | `calcMaxHp(stamina)`는 해금 개념이 필요 없는 상시 적용 수치 |

`required_stat_avg`(20/55/85)와 대략 맞물리도록 30/50/60 구간을 잡아서, 중급 던전(55)에 입장할 즈음이면 연속베기·불꽃베기·철벽 정도는 열려 있고, 고급 던전(85)을 준비할 때쯤 화염폭발까지 열리는 자연스러운 성장 곡선을 의도했습니다. 정확한 구간 수치는 기획 확인 필요(§15).

### 13-3. 스킬 사용 시 능력치 소모 계산식 (제안)

| 스킬 | 소모 능력치 | 소모량 | 근거 |
|---|---|---|---|
| 일격 | 마법력 | 0 (소모 없음) | 기본기는 무제한 — 능력치가 바닥나도 전투 자체가 막히지 않도록 |
| 연속베기 | 마법력 | -2 / 1회 | 2타 공격이라 일격보다 소모 |
| 방어 | 지혜 | 0 (소모 없음) | 기본 방어도 무제한 — 최소 생존 수단 |
| 불꽃베기 | 지혜 | -3 / 1회 | |
| 철벽 | 용기 | -5 / 1회 | 완전 방어는 비쌈 |
| 화염폭발 | 지혜 | -8 / 1회 | 필살기, 이미 있는 긴 쿨타임(`SKILL_COOLDOWNS`)과 함께 이중으로 남용 억제 |

계산식 형태: `사용후능력치 = MAX(0, 사용전능력치 - 소모량)` — §1에서 이미 확인한 `clampPowerValue()`와 동일한 하한 클램프 방식을 그대로 재사용합니다.

### 13-4. 소모를 임시로 할지, 영구로 할지 — 비교 및 제안

**A안 — 임시 소모(전투 세션 메모리에서만 관리, 전투 종료 시 원상복구)**

- 장점: 학생이 전투 중 스킬을 몇 번 쓰든 읽기 활동으로 쌓은 실제 `student_stats`엔 영향이 없어 다음 전투는 항상 "읽기 활동 성과 그대로" 시작됨. API 호출이 스킬 사용마다 필요 없어 지연/서버 부하가 적음.
- 단점: "능력치를 아껴 써야 한다"는 자원관리 긴장감이 판마다 초기화되어 옅어질 수 있음.

**B안 — 영구 소모(`student_stats`에서 실시간 차감)**

- 장점: 전략성/긴장감이 높아짐.
- 단점: **"게임에서 스킬을 남발할수록 읽기 활동으로 쌓은 능력치가 깎여서, 오히려 다음 읽기 활동·다음 던전 도전에 불리해지는" 역설**이 생깁니다. "읽기를 열심히 한 보상으로 게임을 즐긴다"는 원래 동기부여 구조와 정면으로 충돌할 위험이 큽니다. 스킬 사용마다 서버 반영이 필요해 구현도 더 복잡합니다.

**제안: 하이브리드(A안 기반) — §7의 승리 시 리셋 공식과 자연스럽게 연결됩니다.**

- 전투 중에는 A안대로 **세션 로컬(프론트 메모리)에서만** 소모를 반영해 실시간 긴장감(자원 부족 시 스킬 잠김)을 줌.
- 전투 종료 시 `student_stats`에 실제로 반영되는 값은 소모량과 무관하게 **오직 §7의 승리 시 리셋 공식**(`MIN(현재값, reward_stat_reset_value)`)뿐입니다. 즉:
  - 승리: 전투 중 얼마나 소모했든 상관없이, 저장되는 값은 `reward_stat_reset_value` 기준 리셋값.
  - 패배/시간초과: 전투 중 소모는 전부 버려지고, `student_stats`는 전투 시작 전 값 그대로 유지 — 패배해도 읽기 활동 성과가 깎이지 않으므로 안심하고 재도전 가능.
- 이렇게 하면 "전투 중엔 자원을 아껴 써야 하는 긴장감"과 "읽기 활동으로 쌓은 성과는 게임 성과와 무관하게 보호된다"는 두 요구를 동시에 만족합니다.

### 13-5. 전투 중 실시간 능력치 상태 관리 설계

1. **전투 진입 시점**: `GameState.init()`이 `GET /api/students/{studentId}/stats`(§10-3에서 이미 설계)로 받아온 값을 `_state.player.magic/stamina/wisdom/courage`에 대입하는 동시에, **이 시점 값을 `_state.player.entryStats`로 별도 보관**할 것을 제안합니다.
2. **해금 여부 판정은 `entryStats`(전투 진입 시점 고정값) 기준**으로, **사용 가능 여부(자원이 남았는지) 판정은 `_state.player`(실시간으로 깎이는 값) 기준**으로 나눠서 체크해야 합니다. 이렇게 구분하지 않으면, 연속베기를 몇 번 써서 마법력이 30 밑으로 내려가는 순간 "해금 자체가 취소된 것처럼" 보이는 이상한 상태가 됩니다 — 한 번 해금된 스킬은 그 전투 내내 해금 상태를 유지하고, 다만 자원이 없으면 못 쓰는 것으로 설계해야 합니다.
   ```
   isUnlocked(skillKey)  = entryStats[해당 능력치] >= 해금조건(§13-2)
   canUseNow(skillKey)   = isUnlocked(skillKey) AND currentStats[해당 능력치] >= 소모량(§13-3)
   ```
3. **소진 시 처리**: `canUseNow()`가 false면 해당 스킬 버튼을 비활성화(현재 `game/index.html`의 다른 버튼들처럼 `disabled` 속성 + 흐림 처리)하고, 툴팁/텍스트로 "능력치가 부족해요"를 안내. 일격·방어는 소모량을 0으로 설계했으므로(§13-3) 아무리 자원이 바닥나도 최소한의 공격/방어 수단은 항상 남아있어 전투가 완전히 막히는 상황은 생기지 않습니다.
4. **UI 반영**: HUD의 능력치 표시(`dungeon-ui.js:408-411`, 기존 §5에서 확인)는 이미 `s.player.*`를 그대로 찍는 구조라, 소모가 반영된 실시간 값을 그대로 표시하면 됩니다 — 별도 로직 추가 없이 기존 렌더 코드 재사용 가능.

### 13-6. 신규 테이블이 필요한가

**결론: 지금 규모에서는 불필요합니다(옵션 A 권장).** 6개 스킬의 해금 구간·소모량은 자주 바뀌지 않는 게임 밸런스 상수이고, `dungeons`조차 아직 `game-api.js`/`index.html`에 하드코딩 배열로 관리되는 상태(§6에서 이미 지적)라 스킬만 먼저 테이블화하면 오히려 일관성이 떨어집니다.

| 옵션 | 방식 | 장단점 |
|---|---|---|
| **A (권장)** | 스킬 정의는 `game/js/battle-engine.js`에 그대로 두고, 해금 구간·소모량만 같은 파일 내 상수 객체(예: `SKILL_REQUIREMENTS`)로 정리 | 신규 테이블 불필요, 지금 코드 구조와 일관됨. 다만 밸런스 조정 시마다 배포 필요 |
| B (확장형, 필요 시) | 신규 테이블 `dungeon_skills`(skill_key, name, stat_type, unlock_threshold, cost_per_use, damage_type 등) | 배포 없이 밸런스 조정 가능, 서버가 소모량 검증까지 가능(치팅 방지). 다만 `dungeons`도 아직 테이블 이관 전이라 스킬만 먼저 테이블화하는 게 우선순위상 맞는지 재검토 필요 |

**신규 API도 필요 없습니다.** §13-4의 하이브리드 설계상 전투 중 소모는 순수 프론트 로컬 상태이고, 서버에 실제로 반영되는 건 §10-4에서 이미 설계한 `POST /api/students/{studentId}/dungeon-records`의 승리 시 리셋 공식뿐이라 별도 엔드포인트가 늘어나지 않습니다.

### 13-7. 이번 절에서 추가로 확인 필요한 항목

- §13-1의 "지혜=강공격, 용기=방어"로의 재정렬이 실제 기획 의도가 맞는지(안내 문구가 먼저 확정된 것이고 게임 코드가 나중에 따라가야 하는 게 맞는지, 아니면 반대로 안내 문구 쪽을 코드에 맞게 고쳐야 하는지) 확인 필요.
- §13-2의 해금 구간(30/50/60)과 §13-3의 소모량(-2/-3/-5/-8)은 전부 이번 설계에서 제안한 값이며, 실제 게임 밸런스(플레이 테스트) 확인 필요.
- §13-6에서 신규 테이블(옵션 B)로 갈지 상수 유지(옵션 A)로 갈지는 향후 밸런스 조정 빈도에 대한 기획 판단이 필요.
