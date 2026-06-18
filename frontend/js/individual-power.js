/*
  파일명: individual-power.js
  역할: 개별읽기 화면 어디서든 사용할 수 있는 '나의 힘' 팝업입니다.

  사용하는 화면 예시:
  - 개별읽기 메인
  - 책 추천
  - 오늘의 독서 모험
  - 읽기 전
  - 읽기 중
  - 읽기 후
  - 우리 반 추천 책장
  - 책 추천 작성
  - 나의 독서 보관함

  저장 방식:
  - 현재는 sessionStorage를 사용합니다.
  - 나중에 서버/DB가 붙으면 getIndividualPowerValue() 부분을 API 호출로 바꾸면 됩니다.
*/

/*
  현재 로그인한 학생 아이디를 가져옵니다.
  로그인 정보가 없을 때는 테스트용 student-demo를 사용합니다.
*/
function getIndividualPowerStudentId() {
  return sessionStorage.getItem("studentId") || "student-demo";
}

/*
  능력치 값을 가져옵니다.
  기본값은 연습읽기 완료 보상 기준인 8입니다.
*/
function getIndividualPowerValue(key) {
  const studentId = getIndividualPowerStudentId();
  return Number(sessionStorage.getItem("individualPower_" + key + "_" + studentId) || 8);
}

/*
  나의 힘 팝업 HTML을 화면에 한 번만 추가합니다.
*/
function ensureIndividualPowerModal() {
  if (document.getElementById("individualPowerModal")) {
    return;
  }

  const modalHtml = `
    <div id="individualPowerModal" class="individual-power-backdrop hidden">
      <div class="individual-power-modal">
        <button type="button" class="individual-power-close" onclick="closeIndividualPowerModal()">
          ×
        </button>

        <p class="individual-power-kicker">현재까지 모은 나의 힘</p>
        <h2 class="individual-power-title">
          책을 읽고 질문을 만들며<br>
          네 가지 힘이 자라고 있어요
        </h2>

        <div class="individual-power-grid">
          <div class="individual-power-card">
            <strong>
              마법력
              <em id="powerMagicText">8 / 100</em>
            </strong>
            <span>좋은 질문을 만들 힘이에요.</span>
            <div class="individual-power-bar">
              <div class="individual-power-fill" id="powerMagicFill"></div>
            </div>
          </div>

          <div class="individual-power-card">
            <strong>
              체력
              <em id="powerHealthText">8 / 100</em>
            </strong>
            <span>끝까지 읽고 버티는 힘이에요.</span>
            <div class="individual-power-bar">
              <div class="individual-power-fill" id="powerHealthFill"></div>
            </div>
          </div>

          <div class="individual-power-card">
            <strong>
              지혜
              <em id="powerWisdomText">8 / 100</em>
            </strong>
            <span>질문을 깊고 정확하게 다듬는 힘이에요.</span>
            <div class="individual-power-bar">
              <div class="individual-power-fill" id="powerWisdomFill"></div>
            </div>
          </div>

          <div class="individual-power-card">
            <strong>
              용기
              <em id="powerCourageText">8 / 100</em>
            </strong>
            <span>내 생각을 기록하고 나누는 힘이에요.</span>
            <div class="individual-power-bar">
              <div class="individual-power-fill" id="powerCourageFill"></div>
            </div>
          </div>
        </div>

        <button type="button" class="individual-power-confirm" onclick="closeIndividualPowerModal()">
          확인했어
        </button>
      </div>
    </div>
  `;

  document.body.insertAdjacentHTML("beforeend", modalHtml);
}

/*
  나의 힘 팝업을 엽니다.
*/
function openIndividualPowerModal() {
  ensureIndividualPowerModal();
  renderIndividualPowerModal();

  const modal = document.getElementById("individualPowerModal");
  if (modal) {
    modal.classList.remove("hidden");
  }
}

/*
  나의 힘 팝업을 닫습니다.
*/
function closeIndividualPowerModal() {
  const modal = document.getElementById("individualPowerModal");
  if (modal) {
    modal.classList.add("hidden");
  }
}

/*
  팝업 안의 능력치 숫자와 막대를 갱신합니다.
*/
function renderIndividualPowerModal() {
  const powers = {
    magic: getIndividualPowerValue("magic"),
    health: getIndividualPowerValue("health"),
    wisdom: getIndividualPowerValue("wisdom"),
    courage: getIndividualPowerValue("courage")
  };

  updateIndividualPowerItem("powerMagicText", "powerMagicFill", powers.magic);
  updateIndividualPowerItem("powerHealthText", "powerHealthFill", powers.health);
  updateIndividualPowerItem("powerWisdomText", "powerWisdomFill", powers.wisdom);
  updateIndividualPowerItem("powerCourageText", "powerCourageFill", powers.courage);
}

/*
  능력치 한 줄을 갱신합니다.
*/
function updateIndividualPowerItem(textId, fillId, value) {
  const safeValue = Math.max(0, Math.min(100, Number(value) || 0));

  const textElement = document.getElementById(textId);
  const fillElement = document.getElementById(fillId);

  if (textElement) {
    textElement.textContent = safeValue + " / 100";
  }

  if (fillElement) {
    fillElement.style.width = safeValue + "%";
  }
}