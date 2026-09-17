/*
 * individual-min-input.js
 *
 * 개별읽기(읽기 전/중/후) 전용 "최소 작성 기준" 검증 + 안내 모달.
 *
 * 개별읽기 AI 피드백("확인받기")은 이제 학생이 선택적으로 쓰는 도움
 * 기능이라 AI가 fail(수정 필요)을 반환해도 다음 화면 이동을 막지 않는다.
 * 대신 "최소한 실제 기록이 있는가"만 이 파일의 함수로 판단한다 - 내용의
 * 좋고 나쁨은 절대 판단하지 않는다.
 *
 * 연습읽기(온책읽기)의 기존 AI 통과 필수 검증(frontend/js/pre-reading-validation.js)은
 * 이 파일과 완전히 별개이며 전혀 건드리지 않는다 - 이름과 동작을 의도적으로
 * 다르게 유지한다.
 */

/*
 * 실제로 의미를 담은 기록이 있는지만 판별한다:
 * - 완전히 비어 있거나 공백만 있으면 false
 * - 한글 완성 음절/영문/숫자가 하나도 없으면(자음·모음 낱자만, 물음표 등
 *   기호만, "ㄱㄱㄱ"/"?"/"..." 같은 입력) false
 * - 같은 글자(또는 2글자 패턴)만 3회 이상 반복해 전체를 채우면 false
 * - 그 외에는 true. 최소 길이 판정은 아래 validate 함수에서 입력 종류별로
 *   따로 수행한다.
 */
function individualHasMeaningfulRecord(text) {
  if (text == null) return false;

  var trimmed = String(text).trim();
  if (trimmed.length === 0) return false;

  var withoutSpaces = trimmed.replace(/\s+/g, "");
  if (withoutSpaces.length === 0) return false;

  var hasMeaningfulChar = /[가-힣a-zA-Z0-9]/.test(withoutSpaces);
  if (!hasMeaningfulChar) return false;

  if (/^(.)\1{2,}$/.test(withoutSpaces)) return false;
  if (/^(..)\1{2,}$/.test(withoutSpaces)) return false;

  return true;
}

/*
 * 질문/답 입력 화면에서 공통으로 사용하는 최소 작성 기준 검사입니다.
 * 비어 있는 입력과 의미 없는 반복·기호 입력을 구분해 같은 안내 문구를
 * 읽기 전/중/후에서 일관되게 사용할 수 있게 합니다.
 */
function validateIndividualMinimumInput(question, answer) {
  var questionText = question == null ? "" : String(question).trim();
  var answerText = answer == null ? "" : String(answer).trim();

  if (!questionText && !answerText) {
    return { valid: false, message: "질문과 답을 적은 뒤 다음으로 넘어갈 수 있어요.", field: "question" };
  }
  if (!questionText) {
    return { valid: false, message: "질문을 먼저 적어 주세요.", field: "question" };
  }
  if (!answerText) {
    return { valid: false, message: "내가 생각한 답을 먼저 적어 주세요.", field: "answer" };
  }
  if (!individualHasMeaningfulRecord(questionText) || !individualHasMeaningfulRecord(answerText)) {
    return { valid: false, message: "조금 더 알아볼 수 있게 적어 주세요.", field: !individualHasMeaningfulRecord(questionText) ? "question" : "answer" };
  }

  var compactQuestion = questionText.replace(/\s+/g, "");
  var compactAnswer = answerText.replace(/\s+/g, "");
  var questionWords = questionText.split(/\s+/).filter(Boolean).length;
  var answerWords = answerText.split(/\s+/).filter(Boolean).length;
  var questionMeaningfulChars = (questionText.match(/[가-힣a-zA-Z0-9]/g) || []).length;
  var clearShortQuestion = /[?？]$/.test(questionText)
    && questionMeaningfulChars >= 5;
  var questionLongEnough = compactQuestion.length >= 8
    || questionWords >= 4
    || clearShortQuestion;
  var answerLongEnough = compactAnswer.length >= 6 || answerWords >= 3;

  if (!questionLongEnough || !answerLongEnough) {
    var message = !questionLongEnough && !answerLongEnough
      ? "질문과 답을 조금 더 자세히 적어 주세요."
      : !questionLongEnough
        ? "질문을 조금 더 자세히 적어 주세요."
        : "답을 조금 더 자세히 적어 주세요.";
    return {
      valid: false,
      title: "조금 더 적어 주세요!",
      message: message,
      field: !questionLongEnough ? "question" : "answer"
    };
  }

  return { valid: true, message: "", field: "" };
}

function validateIndividualSummaryMinimumInput(summary) {
  var summaryText = summary == null ? "" : String(summary).trim();

  if (!summaryText) {
    return { valid: false, message: "간추리기를 작성한 뒤 다음으로 넘어갈 수 있어요." };
  }
  if (!individualHasMeaningfulRecord(summaryText)) {
    return { valid: false, message: "조금 더 알아볼 수 있게 적어 주세요." };
  }

  var compactSummary = summaryText.replace(/\s+/g, "");
  var summaryWords = summaryText.split(/\s+/).filter(Boolean).length;
  if (compactSummary.length < 20 && summaryWords < 5) {
    return {
      valid: false,
      title: "조금 더 적어 주세요!",
      message: "간추리기를 조금 더 자세히 적어 주세요."
    };
  }

  return { valid: true, message: "" };
}

(function () {
  var STYLE_ID = "individualMinInputModalStyle";
  var OVERLAY_ID = "individualMinInputOverlay";

  function ensureStyle() {
    if (document.getElementById(STYLE_ID)) return;
    var style = document.createElement("style");
    style.id = STYLE_ID;
    style.textContent =
      ".individual-min-input-overlay{position:fixed;inset:0;z-index:9999;display:flex;" +
      "align-items:center;justify-content:center;background:rgba(60,45,25,0.42);}" +
      ".individual-min-input-box{background:#fffaf1;border:2px solid #eacfa7;border-radius:20px;" +
      "max-width:620px;width:calc(100% - 40px);padding:28px 26px 22px;text-align:center;" +
      "box-shadow:0 12px 32px rgba(120,73,21,0.22);font-family:inherit;}" +
      ".individual-min-input-box img{width:64px;height:64px;object-fit:contain;margin-bottom:10px;}" +
      ".individual-min-input-title{font-size:20px;font-weight:800;color:#54361d;margin:0 0 10px;}" +
      ".individual-min-input-message{width:100%;font-size:16px;color:#7b5b34;line-height:1.5;" +
      "margin:0 0 20px;white-space:nowrap;word-break:keep-all;}" +
      ".individual-min-input-confirm{border:none;border-radius:14px;background:#f0a93e;color:#4a2f0f;" +
      "font-size:16px;font-weight:800;padding:12px 32px;cursor:pointer;}" +
      ".individual-min-input-confirm:hover{filter:brightness(1.05);}" +
      "@media(max-width:680px){.individual-min-input-box{width:calc(100% - 28px);padding-left:18px;padding-right:18px;}" +
      ".individual-min-input-message{white-space:normal;}}";
    document.head.appendChild(style);
  }

  window.showIndividualMinInputModal = function (message, focusTarget, title) {
    ensureStyle();

    var existing = document.getElementById(OVERLAY_ID);
    if (existing) existing.remove();

    var overlay = document.createElement("div");
    overlay.id = OVERLAY_ID;
    overlay.className = "individual-min-input-overlay";
    overlay.innerHTML =
      '<div class="individual-min-input-box">' +
      '<div class="individual-min-input-title">아직 적을 내용이 있어요!</div>' +
      '<p class="individual-min-input-message"></p>' +
      '<button type="button" class="individual-min-input-confirm">확인</button>' +
      "</div>";

    overlay.querySelector(".individual-min-input-message").textContent = message || "내용을 먼저 적어 주세요.";
    overlay.querySelector(".individual-min-input-title").textContent = title || "아직 적을 내용이 있어요!";

    function close() {
      overlay.remove();
      if (focusTarget && typeof focusTarget.focus === "function") {
        focusTarget.focus();
      }
    }

    overlay.querySelector(".individual-min-input-confirm").addEventListener("click", close);
    overlay.addEventListener("click", function (event) {
      if (event.target === overlay) close();
    });

    document.body.appendChild(overlay);
  };
})();
