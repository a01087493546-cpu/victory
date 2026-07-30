/*
  파일명: frontend/js/individual-power.js
  역할:
  1. 개별읽기 능력치(마법력/체력/지혜/용기)를 저장합니다.
  2. 나의 힘 모달을 엽니다.
  3. 보상 조건을 달성했을 때 보상 모달을 띄우고 능력치를 올립니다.
  4. 보상값은 임시 세팅이며, 나중에 게임 담당과 맞춰 쉽게 수정할 수 있게 구성합니다.
*/

(function () {
  // ==============================
  // 1. 기본 설정
  // ==============================

  const PRACTICE_COMPLETE_REWARD = 8;

  // 능력치 최대값입니다.
  const MAX_POWER = 100;

  // 능력치 이름 표시용
  const POWER_LABELS = {
    magic: "마법력",
    stamina: "체력",
    wisdom: "지혜",
    courage: "용기"
  };

  // 보상 모달에서 능력치를 구분해서 보여줄 아이콘/색상 키입니다.
  const POWER_ICONS = {
    magic: "🔮",
    stamina: "💪",
    wisdom: "📖",
    courage: "🛡️"
  };

    // 능력치 설명 표시용입니다.
  // ability-intro.html의 능력치 안내 문구와 의미가 맞도록 정리합니다.
  const POWER_DESCRIPTIONS = {
    magic: "좋은 질문을 만들고 떠올리는 힘이에요.",
    stamina: "책을 끝까지 읽고, 내용을 간추리며 쌓는 힘이에요.",
    wisdom: "질문을 더 깊고 정확하게 다듬는 힘이에요.",
    courage: "내 질문과 생각을 친구들과 나누며 쌓는 힘이에요."
  };

  // 보상 팝업과 나의 힘 팝업에서 사용할 능력치 아이콘 경로입니다.
const POWER_ICON_MAP = {
  magic: "../assets/images/ability/ability-magic.png",
  stamina: "../assets/images/ability/ability-health.png",
  wisdom: "../assets/images/ability/ability-wisdom.png",
  courage: "../assets/images/ability/ability-courage.png"
};

  // 이미지가 없을 때 보여줄 대체 이모지
  const POWER_ICON_FALLBACK = {
    magic: "✨",
    stamina: "❤️",
    wisdom: "📘",
    courage: "🛡️"
  };

  // ==============================
  // 2. 임시 보상 규칙
  // ==============================
  // rewardId로 언제든 호출할 수 있게 미리 정리합니다.
  // ※ 전부 임시 세팅값입니다. 나중에 게임 능력치와 맞춰 변경할 수 있습니다.
  const REWARD_PRESETS = {
    // 연습읽기 최종 완료 보상입니다.
    // 연습읽기에서는 능력치를 본격적으로 쌓기보다,
    // 읽기 후 간추리기 공유까지 끝냈을 때 개별읽기 시작 보상으로 지급합니다.
    practice_all_complete: {
      title: "연습읽기 완료 보상",
      subtitle: "수고했어! 이제 개별읽기 모험을 시작할 준비가 되었어.",
      conditionText: "연습읽기에서 읽기 후 간추리기 공유까지 모두 마무리했어요.",
      rewards: {
        magic: PRACTICE_COMPLETE_REWARD,
        stamina: PRACTICE_COMPLETE_REWARD,
        wisdom: PRACTICE_COMPLETE_REWARD,
        courage: PRACTICE_COMPLETE_REWARD
      }
    },

    // 개별읽기 - 질문 1개 작성
    individual_question_created: {
      title: "질문 만들기 보상",
      subtitle: "책을 읽으며 스스로 질문을 떠올렸어요.",
      conditionText: "읽기 중 질문 만들기 활동에서 질문을 작성했어요.",
      rewards: {
        magic: 1
      }
    },
        // 개별읽기 - 읽기 중 질문 활동 완료
    individual_during_questions_complete: {
      title: "읽기 중 질문 활동 보상",
      subtitle: "책을 읽으며 질문을 만들고 생각을 정리했어요.",
      conditionText: "읽기 중 질문 활동을 완료했어요.",
      rewards: {
        magic: 1,
        wisdom: 1
      }
    },
    // 개별읽기 - 읽기 전 활동 완료
individual_before_complete: {
  title: "읽기 전 준비 보상",
  subtitle: "책을 읽기 전에 궁금한 점을 만들며 모험을 준비했어요.",
  conditionText: "읽기 전 질문 4개를 모두 만들었어요.",
  rewards: {
  magic: 1,
  wisdom: 1
}
},

    // 개별읽기 - AI/루미 피드백 통과
    individual_feedback_pass: {
      title: "질문 다듬기 보상",
      subtitle: "질문을 더 깊고 정확하게 다듬었어요.",
      conditionText: "루미 피드백을 확인하고 질문을 고쳐 통과했어요.",
      rewards: {
        wisdom: 1
      }
    },
// 개별읽기 - 읽기 후 질문 3개 피드백 모두 통과
individual_after_questions_pass: {
  title: "간추리기 질문 보상",
  subtitle: "질문과 답을 차근차근 다듬어 간추리기 준비를 마쳤어요.",
  conditionText: "읽기 후 질문 3개가 모두 루미 피드백을 통과했어요.",
  rewards: {
  magic: 1,
  wisdom: 1
}
},
// 개별읽기 - 읽기 후 간추리기 피드백 통과
individual_after_summary_pass: {
  title: "간추리기 완성 보상",
  subtitle: "질문과 답을 바탕으로 책의 중요한 내용을 잘 간추렸어요.",
  conditionText: "읽기 후 간추리기 루미 피드백을 통과했어요.",
  rewards: {
    wisdom: 2
  }
},
// 개별읽기 - 우리 반 추천 책장에 책 추천하기
individual_friend_book_recommend: {
  title: "책 추천 보상",
  subtitle: "내가 읽은 책의 좋은 점을 친구들에게 소개했어요.",
  conditionText: "우리 반 추천 책장에 책 추천글을 등록했어요.",
  rewards: {
    courage: 1
  }
},
    // 개별읽기 - 책수다방 글쓰기
    individual_book_chat_post: {
      title: "책수다방 보상",
      subtitle: "책을 읽고 떠오른 생각을 친구들과 나누었어요.",
      conditionText: "책수다방에 글을 썼어요.",
      rewards: {
        courage: 1
      }
    },

    // 개별읽기 - 질문 공유
    individual_question_shared: {
      title: "질문 나누기 보상",
      subtitle: "내가 만든 질문을 친구들과 나누었어요.",
      conditionText: "내 질문을 친구들에게 나누었어요.",
      rewards: {
        courage: 1
      }
    },

    // 개별읽기 - 간추리기 공유
    individual_summary_shared: {
      title: "간추리기 나누기 보상",
      subtitle: "내가 정리한 책 내용을 친구들과 나누었어요.",
      conditionText: "내 간추리기를 친구들에게 나누었어요.",
      rewards: {
        courage: 1
      }
    },

    // 개별읽기 - 친구 글에 생각 남기기
    individual_thought_comment: {
      title: "생각 나누기 보상",
      subtitle: "친구의 글을 읽고 내 생각을 남겼어요.",
      conditionText: "친구의 질문이나 글에 내 생각을 남겼어요.",
      rewards: {
        courage: 1
      }
    },

    // 기존 코드와 연결되어 있을 수 있어서 남겨두는 통합 용기 보상입니다.
    // 나중에 실제 기능별로 위의 보상 preset을 각각 연결하면 됩니다.
    individual_share_success: {
      title: "생각 나누기 보상",
      subtitle: "친구와 생각을 나누는 용기를 보여줬어요.",
      conditionText: "책수다방에 글을 쓰거나, 내 질문과 생각을 친구들에게 나누었어요.",
      rewards: {
        courage: 1
      }
    },

    // 개별읽기 - 읽기 후 간추리기 완료
    // 체력은 책을 끝까지 읽고 간추리기 활동까지 마쳤을 때 올라가도록 정리합니다.
    individual_after_complete: {
  title: "읽기 후 간추리기 보상",
  subtitle: "책을 끝까지 읽고 내용을 잘 간추렸어요.",
  conditionText: "책을 읽고 간추리기 활동을 마쳤어요.",
  rewards: {
  stamina: 3,
  magic: 1,
  wisdom: 1
}
}
  };

  // ==============================
  // 3. 학생별 저장 키
  // ==============================
  function getStudentId() {
    return sessionStorage.getItem("studentId") || "student-demo";
  }

function getRewardHistoryKey() {
  return `individualRewardHistory_v2_${getStudentId()}`;
}

  // ==============================
  // 4. 능력치 서버 조회 (student_stats DB가 유일한 원본)
  // ==============================
  /*
    나의 힘 모달/위젯을 쓰는 모든 화면은 이 함수 하나로만 능력치를 가져온다.
    학생 홈(individual-reading.html)이 쓰는 것과 완전히 같은 API
    (GET /api/students/{studentId}/stats, JWT 인증)를 그대로 재사용해서
    두 화면이 항상 같은 값을 보여주게 한다. localStorage/sessionStorage는
    더 이상 능력치 숫자의 원본으로 쓰지 않는다 - 조회 실패 시에도 예전
    로컬 숫자를 대신 보여주지 않고 오류로 처리한다.
  */
  const INDIVIDUAL_POWER_API_BASE_URL = "http://localhost:8080";

  function clampPowerValue(value) {
    const number = Number(value) || 0;
    return Math.max(0, Math.min(MAX_POWER, number));
  }

  async function fetchPowerStateFromServer() {
    const studentId = sessionStorage.getItem("studentId");
    const token = sessionStorage.getItem("token");

    if (!studentId || !token) {
      throw new Error("로그인 정보가 없습니다.");
    }

    const response = await fetch(
      INDIVIDUAL_POWER_API_BASE_URL + "/api/students/" + studentId + "/stats",
      {
        headers: {
          Authorization: "Bearer " + token
        }
      }
    );

    if (!response.ok) {
      // 화면에는 짧은 안내만 보여주고, 원인 파악에 필요한 상태 코드/응답
      // 본문은 개발자 콘솔에 그대로 남긴다.
      const body = await response.text().catch(function () { return ""; });
      console.error("나의 힘 능력치 조회 실패:", response.status, body);
      throw new Error("능력치 조회 실패: " + response.status);
    }

    const stats = await response.json();

    return {
      magic: clampPowerValue(stats.magic),
      stamina: clampPowerValue(stats.stamina),
      wisdom: clampPowerValue(stats.wisdom),
      courage: clampPowerValue(stats.courage)
    };
  }

  // ==============================
  // 5. 보상 이력 저장
  // ==============================
  // 같은 보상이 중복으로 계속 들어가지 않게 기록합니다.
function getRewardHistory() {
  const saved = localStorage.getItem(getRewardHistoryKey());

  if (!saved) {
    localStorage.setItem(getRewardHistoryKey(), JSON.stringify([]));
    return [];
  }

  try {
    const parsed = JSON.parse(saved);
    return Array.isArray(parsed) ? parsed : [];
  } catch (error) {
    localStorage.setItem(getRewardHistoryKey(), JSON.stringify([]));
    return [];
  }
}

  function saveRewardHistory(history) {
  localStorage.setItem(getRewardHistoryKey(), JSON.stringify(history));
}

  function hasRewardHistory(rewardKey) {
    return getRewardHistory().includes(rewardKey);
  }

  function addRewardHistory(rewardKey) {
    const history = getRewardHistory();

    if (!history.includes(rewardKey)) {
      history.push(rewardKey);
      saveRewardHistory(history);
    }
  }

  /*
    ==============================
    6. (제거됨) 능력치 증가 처리
    ==============================
    예전에는 여기서 localStorage 능력치에 보상값을 직접 더했다(client-side
    누적). student_stats DB만 유일한 원본으로 쓰기로 하면서 제거했다 -
    실제로 DB를 올리는 활동(연습읽기 완료 등)은 해당 백엔드 API가 이미
    student_stats를 갱신하고, 나의 힘 화면은 항상 서버를 다시 조회해서
    보여준다(fetchPowerStateFromServer). 아직 DB 보상이 구현되지 않은
    개별읽기 활동은 이 파일에서 새로 구현하지 않는다(범위 밖).
  */

  // ==============================
  // 7. 보상 모달 HTML
  // ==============================
  function buildRewardItemHTML(type, amount) {
  const label = POWER_LABELS[type];

  if (!amount || amount <= 0) {
    return "";
  }

  /*
    보상 모달에서는 보조 설명을 제거하고,
    이번에 오른 능력치를 가로 그래프로 보여줍니다.
    예: 마법력 +8
    4개 능력치를 한 줄에 나란히 보여주기 위해 아이콘 + 능력치별 강조색을 씁니다.
  */
  const percent = Math.round((amount / MAX_POWER) * 100);
  const icon = POWER_ICONS[type] || "✨";

  return `
    <div class="individual-reward-item type-${type}">
      <div class="individual-reward-icon">${icon}</div>

      <div class="individual-reward-row-head">
        <div class="individual-reward-name">${label}</div>
        <div class="individual-reward-amount">+${amount}</div>
      </div>

      <div class="individual-reward-bar">
        <div class="individual-reward-bar-fill" style="width: ${percent}%;"></div>
      </div>
    </div>
  `;
}

  function buildRewardListHTML(rewards) {
    return Object.keys(POWER_LABELS)
      .map((type) => buildRewardItemHTML(type, rewards[type] || 0))
      .join("");
  }

  function removeRewardModal() {
    const oldModal = document.getElementById("individualRewardOverlay");
    if (oldModal) {
      oldModal.remove();
    }
  }

  function showRewardModal(preset) {
  removeRewardModal();

  // 보상 모달에서도 가로 그래프 스타일을 사용할 수 있게 합니다.
  ensurePowerBarStyle();

  const overlay = document.createElement("div");
    overlay.id = "individualRewardOverlay";
    overlay.className = "individual-reward-overlay";

    overlay.innerHTML = `
      <div class="individual-reward-modal">
        <button type="button" class="individual-reward-close" id="individualRewardCloseBtn">×</button>

        <div class="individual-reward-top-text">보상을 획득했어요</div>
        <h2 class="individual-reward-title">${preset.title}</h2>
        <p class="individual-reward-subtitle">${preset.subtitle}</p>

        <div class="individual-reward-condition-box">
          <div class="individual-reward-condition-label">보상을 받은 이유</div>
          <div class="individual-reward-condition-text">${preset.conditionText}</div>
        </div>

        <div class="individual-reward-list">
          ${buildRewardListHTML(preset.rewards)}
        </div>

       

        <button type="button" class="individual-reward-confirm" id="individualRewardConfirmBtn">
          확인했어
        </button>
      </div>
    `;

    document.body.appendChild(overlay);

    const closeModal = () => {
      overlay.remove();
      refreshPowerModalIfOpen();
    };

    document.getElementById("individualRewardCloseBtn").addEventListener("click", closeModal);
    document.getElementById("individualRewardConfirmBtn").addEventListener("click", closeModal);

    overlay.addEventListener("click", function (event) {
      if (event.target === overlay) {
        closeModal();
      }
    });
  }

  // ==============================
  // 8. 공통 보상 지급 함수
  // ==============================
    /*
    사용 예시:
    givePowerRewardOnce("practice_all_complete");
    givePowerRewardOnce("individual_after_complete");

    질문을 만들 때처럼 날짜별/질문별로 다르게 줄 때:
    givePowerRewardOnce("individual_question_created_20260619_q1", {
      presetKey: "individual_question_created"
    });

    용기 보상을 기능별로 줄 때:
    givePowerRewardOnce("book_chat_post_20260619_1", {
      presetKey: "individual_book_chat_post"
    });

    givePowerRewardOnce("question_shared_20260619_q1", {
      presetKey: "individual_question_shared"
    });

    givePowerRewardOnce("summary_shared_20260619_1", {
      presetKey: "individual_summary_shared"
    });

    givePowerRewardOnce("thought_comment_20260619_1", {
      presetKey: "individual_thought_comment"
    });
  */
  function givePowerRewardOnce(rewardKey, options = {}) {
  // options.presetKey가 있으면 그 preset을 사용하고,
  // 없으면 rewardKey 자체를 preset key로 사용합니다.
  const presetKey = options.presetKey || rewardKey;
  const preset = REWARD_PRESETS[presetKey];

  if (!preset) {
    console.warn("등록되지 않은 보상 preset입니다:", presetKey);
    return false;
  }

  // 이미 받은 보상인지 확인
  if (hasRewardHistory(rewardKey)) {
    return false;
  }

  // 능력치는 여기서 직접 더하지 않는다 - student_stats DB를 실제로 올리는
  // 백엔드 보상 API가 있는 활동(연습읽기 완료 등)은 그 API가 이미 처리하고,
  // 프론트는 "이 보상을 이미 안내했는지"만 기록한다. 화면에 보이는 능력치
  // 총합은 언제나 fetchPowerStateFromServer()로 서버에서 새로 조회한다.

  // 보상 이력 기록
  addRewardHistory(rewardKey);

  // showModal이 false가 아닐 때만 공통 보상 모달을 띄웁니다.
  // 책수다방처럼 전용 보상 화면이 있는 경우에는 showModal: false로 호출합니다.
  if (options.showModal !== false) {
    showRewardModal(preset);
  }

  return true;
}

  // ==============================
  // 9. 나의 힘 모달
  // ==============================
  function removePowerModal() {
    const oldModal = document.getElementById("individualPowerModalOverlay");
    if (oldModal) {
      oldModal.remove();
    }
  }

  function buildPowerRowHTML(type, value) {
  // 능력치 값이 0~100 사이를 넘지 않도록 정리합니다.
  const safeValue = clampPowerValue(value);

  // 가로 그래프의 채워질 비율입니다.
  const percent = Math.round((safeValue / MAX_POWER) * 100);

  return `
    <div class="individual-power-row">
      <div class="individual-power-row-head">
        <div class="individual-power-row-name">${POWER_LABELS[type]}</div>
        <div class="individual-power-row-value">${safeValue} / ${MAX_POWER}</div>
      </div>

      <div class="individual-power-bar">
        <div class="individual-power-bar-fill" style="width: ${percent}%;"></div>
      </div>
    </div>
  `;
}
function ensurePowerBarStyle() {
  // 이미 스타일을 넣었다면 중복으로 넣지 않습니다.
  if (document.getElementById("individualPowerBarStyle")) {
    return;
  }

  const style = document.createElement("style");
  style.id = "individualPowerBarStyle";

  style.textContent = `
      .individual-power-overlay,
    .individual-reward-overlay {
      position: fixed !important;
      inset: 0 !important;
      z-index: 99999 !important;
      display: flex !important;
      align-items: center !important;
      justify-content: center !important;
      padding: 24px !important;
      background: rgba(54, 35, 15, 0.48) !important;
      backdrop-filter: blur(5px) !important;
    }

    .individual-power-modal,
    .individual-reward-modal {
      position: relative !important;
      width: min(520px, 100%) !important;
      max-height: calc(100vh - 48px) !important;
      overflow-y: auto !important;
      padding: 32px 32px 28px !important;
      border: 1px solid rgba(186, 120, 32, 0.34) !important;
      border-radius: 28px !important;
      background:
        radial-gradient(circle at 20% 0%, rgba(255, 225, 151, 0.42), transparent 34%),
        rgba(255, 250, 238, 0.98) !important;
      color: #573313 !important;
      box-shadow: 0 24px 60px rgba(79, 50, 18, 0.28) !important;
      text-align: center !important;
    }

    .individual-power-close,
    .individual-reward-close {
      position: absolute !important;
      top: 14px !important;
      right: 16px !important;
      width: 34px !important;
      height: 34px !important;
      border: none !important;
      border-radius: 50% !important;
      background: rgba(126, 77, 20, 0.1) !important;
      color: #6a3d15 !important;
      font-size: 24px !important;
      font-weight: 900 !important;
      cursor: pointer !important;
    }

    .individual-power-top-text,
    .individual-reward-top-text {
      margin: 0 0 10px !important;
      color: #b2731d !important;
      font-size: 15px !important;
      font-weight: 900 !important;
    }

    .individual-power-title,
    .individual-reward-title {
      margin: 0 0 22px !important;
      font-family: "Noto Serif KR", "Noto Sans KR", serif !important;
      color: #4f2f13 !important;
      font-size: 28px !important;
      font-weight: 900 !important;
      line-height: 1.35 !important;
      letter-spacing: -0.04em !important;
    }

    .individual-power-list {
      display: grid !important;
      gap: 12px !important;
      margin-bottom: 20px !important;
      text-align: left !important;
    }

    /* 보상 모달의 능력치 4개는 한 줄(가로)로 나란히 보여서
       스크롤 없이 한 화면에 다 들어오게 합니다. */
    .individual-reward-list {
      display: grid !important;
      grid-template-columns: repeat(4, minmax(0, 1fr)) !important;
      gap: 10px !important;
      margin-bottom: 18px !important;
      text-align: center !important;
    }

    .individual-power-notice {
      margin: 4px 0 18px !important;
      padding: 12px 14px !important;
      border-radius: 16px !important;
      background: rgba(255, 238, 190, 0.72) !important;
      color: #7a5123 !important;
      font-size: 13px !important;
      font-weight: 800 !important;
      line-height: 1.45 !important;
      word-break: keep-all !important;
    }

    .individual-reward-condition-box {
      margin: 0 0 16px !important;
      padding: 14px 16px !important;
      border-radius: 18px !important;
      background: rgba(255, 238, 190, 0.72) !important;
      text-align: left !important;
    }

    .individual-reward-condition-label {
      margin-bottom: 5px !important;
      color: #b2731d !important;
      font-size: 13px !important;
      font-weight: 900 !important;
    }

    .individual-reward-condition-text {
      color: #5d3615 !important;
      font-size: 15px !important;
      font-weight: 850 !important;
      line-height: 1.4 !important;
    }

    .individual-power-confirm,
    .individual-reward-confirm {
      width: 100% !important;
      height: 48px !important;
      border: none !important;
      border-radius: 999px !important;
      background: linear-gradient(180deg, #dca94f, #a8641e) !important;
      color: #fff9e8 !important;
      font-size: 16px !important;
      font-weight: 900 !important;
      cursor: pointer !important;
      box-shadow: 0 8px 16px rgba(119, 72, 19, 0.18) !important;
    }


    .individual-power-row {
      padding: 16px 18px !important;
      border-radius: 18px !important;
      border: 1px solid rgba(180, 116, 34, 0.18) !important;
      background: rgba(255, 246, 220, 0.82) !important;
    }

    .individual-power-row-head {
      display: flex !important;
      align-items: center !important;
      justify-content: space-between !important;
      gap: 12px !important;
      margin-bottom: 10px !important;
    }

    .individual-power-row-name {
      color: #4e2d10 !important;
      font-size: 18px !important;
      font-weight: 950 !important;
      line-height: 1.1 !important;
    }

    .individual-power-row-value {
      color: #6f431b !important;
      font-size: 17px !important;
      font-weight: 950 !important;
      white-space: nowrap !important;
    }

    .individual-power-bar {
      width: 100% !important;
      height: 14px !important;
      border-radius: 999px !important;
      background: rgba(94, 58, 18, 0.14) !important;
      overflow: hidden !important;
      box-shadow: inset 0 1px 2px rgba(74, 42, 12, 0.12) !important;
    }

    .individual-power-bar-fill {
      height: 100% !important;
      border-radius: 999px !important;
      background: linear-gradient(90deg, #e8bc55, #a8641e) !important;
      transition: width 0.25s ease !important;
    }

    .individual-power-status {
      padding: 24px 8px !important;
      color: #7a5123 !important;
      font-size: 15px !important;
      font-weight: 800 !important;
      text-align: center !important;
      line-height: 1.5 !important;
    }

    .individual-power-status-error {
      color: #b23b2b !important;
    }

    .individual-power-retry {
      width: 100% !important;
      height: 40px !important;
      margin-bottom: 12px !important;
      border: none !important;
      border-radius: 999px !important;
      background: rgba(126, 77, 20, 0.12) !important;
      color: #6a3d15 !important;
      font-size: 14px !important;
      font-weight: 900 !important;
      cursor: pointer !important;
    }
    /* 보상 카드 하나(능력치 1개)입니다. 4개가 한 줄에 들어가도록
       아이콘 위 / 이름·수치 아래 / 그래프 순서로 세로로 압축했습니다. */
    .individual-reward-item {
      min-width: 0 !important;
      padding: 12px 8px 10px !important;
      border-radius: 16px !important;
      border: 1px solid rgba(180, 116, 34, 0.18) !important;
      background: rgba(255, 246, 220, 0.82) !important;
      display: flex !important;
      flex-direction: column !important;
      align-items: center !important;
    }

    .individual-reward-icon {
      font-size: 22px !important;
      line-height: 1 !important;
      margin-bottom: 4px !important;
    }

    .individual-reward-row-head {
      display: flex !important;
      flex-direction: column !important;
      align-items: center !important;
      gap: 2px !important;
      width: 100% !important;
      margin-bottom: 8px !important;
    }

    .individual-reward-name {
      color: #4e2d10 !important;
      font-size: 13px !important;
      font-weight: 950 !important;
      line-height: 1.1 !important;
      white-space: nowrap !important;
    }

    .individual-reward-amount {
      color: #a8641e !important;
      font-size: 15px !important;
      font-weight: 950 !important;
      white-space: nowrap !important;
    }

    .individual-reward-bar {
      width: 100% !important;
      height: 8px !important;
      border-radius: 999px !important;
      background: rgba(94, 58, 18, 0.14) !important;
      overflow: hidden !important;
      box-shadow: inset 0 1px 2px rgba(74, 42, 12, 0.12) !important;
    }

    .individual-reward-bar-fill {
      height: 100% !important;
      border-radius: 999px !important;
      background: linear-gradient(90deg, #e8bc55, #a8641e) !important;
      transition: width 0.25s ease !important;
    }

    /* 능력치별 강조 색상: 한눈에 어떤 능력치인지 구분되도록 합니다. */
    .individual-reward-item.type-magic .individual-reward-amount {
      color: #7c4fd1 !important;
    }
    .individual-reward-item.type-magic .individual-reward-bar-fill {
      background: linear-gradient(90deg, #cbb6f5, #7c4fd1) !important;
    }

    .individual-reward-item.type-stamina .individual-reward-amount {
      color: #c0392b !important;
    }
    .individual-reward-item.type-stamina .individual-reward-bar-fill {
      background: linear-gradient(90deg, #f3a58a, #c0392b) !important;
    }

    .individual-reward-item.type-wisdom .individual-reward-amount {
      color: #2f7a6f !important;
    }
    .individual-reward-item.type-wisdom .individual-reward-bar-fill {
      background: linear-gradient(90deg, #9adbcf, #2f7a6f) !important;
    }

    .individual-reward-item.type-courage .individual-reward-amount {
      color: #4c8c3c !important;
    }
    .individual-reward-item.type-courage .individual-reward-bar-fill {
      background: linear-gradient(90deg, #b3e0a0, #4c8c3c) !important;
    }
  `;

  document.head.appendChild(style);
}
  // 모달의 능력치 영역(#individualPowerListArea)에 로딩/오류/재시도 상태를
  // 그리고, student_stats DB를 다시 조회해 최신 값으로 채웁니다. 실패하면
  // 과거 값(로컬 캐시)으로 대체하지 않고 오류 + 재시도 버튼을 보여줍니다.
  async function loadPowerModalStats() {
    const listArea = document.getElementById("individualPowerListArea");
    if (!listArea) return;

    listArea.innerHTML = `
      <div class="individual-power-status">불러오는 중...</div>
    `;

    try {
      const power = await fetchPowerStateFromServer();
      listArea.innerHTML = `
        ${buildPowerRowHTML("magic", power.magic)}
        ${buildPowerRowHTML("stamina", power.stamina)}
        ${buildPowerRowHTML("wisdom", power.wisdom)}
        ${buildPowerRowHTML("courage", power.courage)}
      `;
    } catch (error) {
      // 네트워크 자체가 끊긴 경우(예: "Load failed")는 상태 코드가 없으므로
      // 에러 객체 자체를 콘솔에 남긴다 - fetchPowerStateFromServer 안에서
      // 이미 상태 코드/응답 본문을 남긴 경우(4xx/5xx)와 합쳐서 두 경우 모두
      // 개발자 콘솔에서 원인을 확인할 수 있게 한다.
      console.error("나의 힘 능력치를 불러오지 못했습니다.", error);

      listArea.innerHTML = `
        <div class="individual-power-status individual-power-status-error">
          능력치를 불러오지 못했습니다.<br>잠시 후 다시 시도해주세요.
        </div>
        <button type="button" class="individual-power-retry" id="individualPowerRetryBtn">다시 시도</button>
      `;

      const retryBtn = document.getElementById("individualPowerRetryBtn");
      if (retryBtn) {
        retryBtn.addEventListener("click", loadPowerModalStats);
      }
    }
  }

  function openIndividualPowerModal() {
  removePowerModal();

  // 나의 힘 팝업에서 가로 그래프 스타일을 사용할 수 있게 합니다.
  ensurePowerBarStyle();

  const overlay = document.createElement("div");
  overlay.id = "individualPowerModalOverlay";
  overlay.className = "individual-power-overlay";

  overlay.innerHTML = `
    <div class="individual-power-modal">
      <button type="button" class="individual-power-close" id="individualPowerCloseBtn">×</button>

      <div class="individual-power-top-text">현재까지 모은 나의 힘</div>
      <h2 class="individual-power-title">책을 읽고 질문하며<br>네 가지 힘을 길러요</h2>

      <div class="individual-power-list" id="individualPowerListArea"></div>

      <button type="button" class="individual-power-confirm" id="individualPowerConfirmBtn">
        확인했어
      </button>
    </div>
  `;

  document.body.appendChild(overlay);

  const closeModal = () => overlay.remove();

  document.getElementById("individualPowerCloseBtn").addEventListener("click", closeModal);
  document.getElementById("individualPowerConfirmBtn").addEventListener("click", closeModal);

  overlay.addEventListener("click", function (event) {
    if (event.target === overlay) {
      closeModal();
    }
  });

  // 모달을 열 때마다 이전에 남아있던 값을 재사용하지 않고 항상 서버에서
  // 새로 조회합니다.
  loadPowerModalStats();
}

  function refreshPowerModalIfOpen() {
    const modal = document.getElementById("individualPowerModalOverlay");
    if (!modal) return;

    loadPowerModalStats();
  }

  // ==============================
  // 9-1. 활동 완료 보상 미니 모달 (공통 컴포넌트)
  // ==============================
  // 개별읽기 읽기 후 - 간추리기 완료 화면에서 쓰던 "간추리기 질문을 모두
  // 다듬었어요!" 스타일의 보상 모달을 모든 화면(읽기 전/읽기 중/읽기 후/
  // 책수다방)에서 공통으로 쓸 수 있게 여기로 옮겨 왔습니다.
  // 화면마다 badge/title/message/rewards만 다르게 넘기면 됩니다.
  const MINI_REWARD_ICON_MAP = {
    magic: "../assets/images/individual-reading/reward/reward-magic-wand.png",
    stamina: "../assets/images/individual-reading/reward/reward-stamina-heart.png",
    wisdom: "../assets/images/individual-reading/reward/reward-wisdom-book.png",
    courage: "../assets/images/individual-reading/reward/reward-courage-shield.png"
  };

  let miniRewardNextAction = null;

  function escapeMiniRewardText(text) {
    return String(text || "")
      .replaceAll("&", "&amp;")
      .replaceAll("<", "&lt;")
      .replaceAll(">", "&gt;")
      .replaceAll('"', "&quot;")
      .replaceAll("'", "&#039;");
  }

  function ensureMiniRewardModalStyle() {
    if (document.getElementById("miniRewardModalStyle")) {
      return;
    }

    const style = document.createElement("style");
    style.id = "miniRewardModalStyle";

    style.textContent = `
      .mini-reward-overlay {
        position: fixed !important;
        inset: 0 !important;
        z-index: 100000 !important;
        display: none !important;
        align-items: center !important;
        justify-content: center !important;
        padding: 24px !important;
        background: rgba(42, 25, 10, 0.5) !important;
        backdrop-filter: blur(5px) !important;
      }

      .mini-reward-overlay.is-open {
        display: flex !important;
      }

      .mini-reward-modal {
        position: relative !important;
        width: min(640px, calc(100vw - 48px)) !important;
        max-height: calc(100vh - 48px) !important;
        overflow-y: auto !important;
        padding: 46px 42px 34px !important;
        border-radius: 32px !important;
        background:
          radial-gradient(circle at 50% 0%, rgba(255, 231, 153, 0.75), transparent 34%),
          radial-gradient(circle at 18% 22%, rgba(255, 244, 204, 0.72), transparent 28%),
          radial-gradient(circle at 86% 80%, rgba(224, 158, 47, 0.22), transparent 34%),
          linear-gradient(180deg, #fff9e9 0%, #f8ecd2 58%, #f0d8a9 100%) !important;
        box-shadow: 0 24px 60px rgba(79, 50, 18, 0.30) !important;
        text-align: center !important;
      }

      .mini-reward-close {
        position: absolute !important;
        top: 16px !important;
        right: 18px !important;
        width: 34px !important;
        height: 34px !important;
        border: none !important;
        border-radius: 50% !important;
        background: rgba(126, 77, 20, 0.10) !important;
        color: #6a3d15 !important;
        font-size: 22px !important;
        font-weight: 900 !important;
        cursor: pointer !important;
      }

      .mini-reward-badge {
        display: inline-block !important;
        margin: 0 0 14px !important;
        padding: 8px 18px !important;
        border-radius: 999px !important;
        background: rgba(255, 231, 153, 0.6) !important;
        color: #a86618 !important;
        font-size: 16px !important;
        font-weight: 900 !important;
      }

      .mini-reward-title {
        margin: 0 0 24px !important;
        font-family: "Noto Serif KR", "Noto Sans KR", serif !important;
        color: #4f2f17 !important;
        font-size: 30px !important;
        font-weight: 900 !important;
        line-height: 1.3 !important;
        letter-spacing: -0.02em !important;
      }

      .mini-reward-list {
        display: grid !important;
        grid-template-columns: minmax(0, 1fr) !important;
        gap: 14px !important;
        margin-bottom: 20px !important;
        /* 루미 대사 카드(.mini-reward-message)와 좌우 폭을 맞추려고 여기서는
           오른쪽 여백을 두지 않는다 - 루미 캐릭터를 피하는 여백은 카드
           자체(.mini-reward-item)의 padding-right로 옮겨서, 카드 배경/테두리는
           대사 카드와 똑같이 전체 폭을 쓰고 그 안의 +1 숫자만 안쪽으로 들어온다. */
        width: 100% !important;
        box-sizing: border-box !important;
        text-align: left !important;
      }

      .mini-reward-item {
        display: flex !important;
        align-items: center !important;
        justify-content: space-between !important;
        width: 100% !important;
        box-sizing: border-box !important;
        /* 오른쪽만 148px로 넓혀 루미 캐릭터와 겹치지 않게 한다(왼쪽/상하는 기존과 동일). */
        padding: 14px 148px 14px 18px !important;
        border-radius: 20px !important;
        background: linear-gradient(90deg, rgba(255, 255, 247, 0.92), rgba(255, 246, 222, 0.78)) !important;
        border: 1px solid rgba(180, 116, 34, 0.18) !important;
      }

      .mini-reward-left {
        display: flex !important;
        align-items: center !important;
        gap: 12px !important;
      }

      .mini-reward-icon {
        width: 48px !important;
        height: 48px !important;
        object-fit: contain !important;
      }

      .mini-reward-name {
        color: #4e2d10 !important;
        font-size: 19px !important;
        font-weight: 900 !important;
      }

      .mini-reward-point {
        color: #b86e17 !important;
        font-size: 24px !important;
        font-weight: 950 !important;
      }

      .mini-reward-message {
        margin-bottom: 22px !important;
        padding: 13px 16px !important;
        /* 루미 캐릭터 자리만큼 오른쪽 여백을 확보해서 말풍선 글자와 겹치지 않게 합니다. */
        padding-right: 148px !important;
        border-radius: 18px !important;
        background: rgba(255, 249, 232, 0.82) !important;
        color: #5d3615 !important;
        font-size: 16px !important;
        font-weight: 850 !important;
        line-height: 1.5 !important;
        word-break: keep-all !important;
      }

      .mini-reward-confirm {
        width: 100% !important;
        height: 58px !important;
        border: none !important;
        border-radius: 999px !important;
        background: linear-gradient(180deg, #e8b64f 0%, #c9821d 52%, #a95f15 100%) !important;
        color: #fff9e8 !important;
        font-size: 19px !important;
        font-weight: 900 !important;
        cursor: pointer !important;
        box-shadow: 0 8px 16px rgba(119, 72, 19, 0.22) !important;
      }

      .mini-reward-lumi {
        position: absolute !important;
        z-index: 2 !important;
        right: 18px !important;
        /* 하단 확인 버튼(34px 모달 패딩 + 58px 버튼 높이 = 92px) 위쪽에서
           끝나도록 여유를 두어, 버튼과도 절대 겹치지 않게 고정합니다. */
        bottom: 100px !important;
        width: 108px !important;
        height: auto !important;
        max-height: 168px !important;
        pointer-events: none !important;
        filter: drop-shadow(0 10px 14px rgba(70, 38, 8, 0.24)) !important;
      }

      @media (max-width: 560px) {
        .mini-reward-lumi {
          display: none !important;
        }
      }
    `;

    document.head.appendChild(style);
  }

  function ensureMiniRewardModal() {
    let overlay = document.getElementById("miniRewardOverlay");

    if (overlay) {
      return overlay;
    }

    ensureMiniRewardModalStyle();

    overlay = document.createElement("div");
    overlay.id = "miniRewardOverlay";
    overlay.className = "mini-reward-overlay";
    overlay.setAttribute("aria-hidden", "true");

    overlay.innerHTML = `
      <div class="mini-reward-modal">
        <button type="button" class="mini-reward-close" id="miniRewardCloseBtn" aria-label="닫기">×</button>

        <img
          class="mini-reward-lumi"
          src="../assets/images/individual-reading/reward/reward-lumi-cheer.png"
          alt="응원하는 루미"
        >

        <div class="mini-reward-badge" id="miniRewardBadge">✨ 나의 힘 성장!</div>
        <h2 class="mini-reward-title" id="miniRewardTitle">보상을 받았어요!</h2>

        <div class="mini-reward-list" id="miniRewardList"></div>

        <div class="mini-reward-message" id="miniRewardMessage"></div>

        <button type="button" class="mini-reward-confirm" id="miniRewardConfirmBtn">확인했어</button>
      </div>
    `;

    document.body.appendChild(overlay);

    document.getElementById("miniRewardCloseBtn").addEventListener("click", closeMiniRewardModal);
    document.getElementById("miniRewardConfirmBtn").addEventListener("click", confirmMiniRewardModal);

    overlay.addEventListener("click", function (event) {
      if (event.target === overlay) {
        closeMiniRewardModal();
      }
    });

    return overlay;
  }

  /*
    사용 예시:
    openMiniRewardModal({
      badge: "✨ 지혜 성장!",
      title: "간추리기 질문을 모두 다듬었어요!",
      message: "루미: 질문을 잘 다듬었어! 이제 간추리기도 잘할 수 있겠다!",
      rewards: [
        { type: "magic", name: "마법력", point: "+1" },
        { type: "wisdom", name: "지혜", point: "+1" }
      ]
    });
  */
  function openMiniRewardModal(rewardData) {
    const overlay = ensureMiniRewardModal();
    const data = rewardData || {};

    const rewards = data.rewards || [];

    miniRewardNextAction = typeof data.nextAction === "function" ? data.nextAction : null;

    document.getElementById("miniRewardBadge").textContent = data.badge || "✨ 나의 힘 성장!";
    document.getElementById("miniRewardTitle").textContent = data.title || "보상을 받았어요!";
    document.getElementById("miniRewardMessage").textContent = data.message || "";

    document.getElementById("miniRewardList").innerHTML = rewards.map(function (reward) {
      const iconSrc = MINI_REWARD_ICON_MAP[reward.type] || MINI_REWARD_ICON_MAP.magic;
      const rewardName = escapeMiniRewardText(reward.name || POWER_LABELS[reward.type] || "능력치");
      const rewardPoint = escapeMiniRewardText(reward.point || "+1");

      return `
        <div class="mini-reward-item">
          <div class="mini-reward-left">
            <img class="mini-reward-icon" src="${iconSrc}" alt="${rewardName}">
            <span class="mini-reward-name">${rewardName}</span>
          </div>
          <strong class="mini-reward-point">${rewardPoint}</strong>
        </div>
      `;
    }).join("");

    overlay.classList.add("is-open");
    overlay.setAttribute("aria-hidden", "false");
  }

  function closeMiniRewardModal() {
    const overlay = document.getElementById("miniRewardOverlay");

    if (!overlay) return;

    overlay.classList.remove("is-open");
    overlay.setAttribute("aria-hidden", "true");
  }

  function confirmMiniRewardModal() {
    closeMiniRewardModal();

    if (typeof miniRewardNextAction === "function") {
      const nextAction = miniRewardNextAction;
      miniRewardNextAction = null;
      nextAction();
    }
  }

  // ==============================
  // 10. 외부에서 쓰도록 열어두기
  // ==============================
  window.openIndividualPowerModal = openIndividualPowerModal;
  window.givePowerRewardOnce = givePowerRewardOnce;
  window.fetchIndividualPowerState = fetchPowerStateFromServer;
  window.REWARD_PRESETS = REWARD_PRESETS;
  window.openMiniRewardModal = openMiniRewardModal;
  window.closeMiniRewardModal = closeMiniRewardModal;
  window.confirmMiniRewardModal = confirmMiniRewardModal;

  // ==============================
  // 11. 페이지 로드 시 초기화
  // ==============================
  document.addEventListener("DOMContentLoaded", function () {
    getRewardHistory();
  });
})();
