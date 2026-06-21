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

  // 연습읽기 최종 마무리 후 처음 받는 기본 보상값입니다.
  // ※ 임시 세팅값입니다. 나중에 게임 쪽과 맞춰 바뀔 수 있습니다.
  const BASE_START_POWER = 8;

  // 능력치 최대값입니다.
  const MAX_POWER = 100;

  // 능력치 이름 표시용
  const POWER_LABELS = {
    magic: "마법력",
    stamina: "체력",
    wisdom: "지혜",
    courage: "용기"
  };

    // 능력치 설명 표시용입니다.
  // ability-intro.html의 능력치 안내 문구와 의미가 맞도록 정리합니다.
  const POWER_DESCRIPTIONS = {
    magic: "좋은 질문을 만들고 떠올리는 힘이에요.",
    stamina: "책을 끝까지 읽고, 내용을 간추리며 쌓는 힘이에요.",
    wisdom: "질문을 더 깊고 정확하게 다듬는 힘이에요.",
    courage: "내 질문과 생각을 친구들과 나누며 쌓는 힘이에요."
  };

  // 보상 모달에 표시할 아이콘 경로입니다.
  // ※ 아래 경로는 네 프로젝트의 실제 아이콘 경로로 맞추면 됩니다.
  // ※ 아직 정확한 파일명이 다르면 그대로 두고, 나중에 아이콘 경로만 수정하면 됩니다.
  const POWER_ICON_MAP = {
    magic: "../assets/images/ability/magic-icon.png",
    stamina: "../assets/images/ability/stamina-icon.png",
    wisdom: "../assets/images/ability/wisdom-icon.png",
    courage: "../assets/images/ability/courage-icon.png"
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
        magic: BASE_START_POWER,
        stamina: BASE_START_POWER,
        wisdom: BASE_START_POWER,
        courage: BASE_START_POWER
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

    // 개별읽기 - AI/루미 피드백 통과
    individual_feedback_pass: {
      title: "질문 다듬기 보상",
      subtitle: "질문을 더 깊고 정확하게 다듬었어요.",
      conditionText: "루미 피드백을 확인하고 질문을 고쳐 통과했어요.",
      rewards: {
        wisdom: 1
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
        stamina: 5,
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

  function getPowerStorageKey() {
    return `individualPower_${getStudentId()}`;
  }

  function getRewardHistoryKey() {
    return `individualRewardHistory_${getStudentId()}`;
  }

  // ==============================
  // 4. 능력치 저장/불러오기
  // ==============================
  function getDefaultPowerState() {
    return {
      magic: 0,
      stamina: 0,
      wisdom: 0,
      courage: 0
    };
  }

  function clampPowerValue(value) {
    const number = Number(value) || 0;
    return Math.max(0, Math.min(MAX_POWER, number));
  }

  function getPowerState() {
    const saved = localStorage.getItem(getPowerStorageKey());

    if (!saved) {
      const defaultState = getDefaultPowerState();
      localStorage.setItem(getPowerStorageKey(), JSON.stringify(defaultState));
      return defaultState;
    }

    try {
      const parsed = JSON.parse(saved);
      return {
        magic: clampPowerValue(parsed.magic),
        stamina: clampPowerValue(parsed.stamina),
        wisdom: clampPowerValue(parsed.wisdom),
        courage: clampPowerValue(parsed.courage)
      };
    } catch (error) {
      const defaultState = getDefaultPowerState();
      localStorage.setItem(getPowerStorageKey(), JSON.stringify(defaultState));
      return defaultState;
    }
  }

  function savePowerState(powerState) {
    const normalized = {
      magic: clampPowerValue(powerState.magic),
      stamina: clampPowerValue(powerState.stamina),
      wisdom: clampPowerValue(powerState.wisdom),
      courage: clampPowerValue(powerState.courage)
    };

    localStorage.setItem(getPowerStorageKey(), JSON.stringify(normalized));
    window.dispatchEvent(
      new CustomEvent("individualPowerUpdated", { detail: normalized })
    );
    return normalized;
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

  // ==============================
  // 6. 능력치 증가 처리
  // ==============================
  function applyRewardValues(rewards) {
    const current = getPowerState();

    const next = {
      magic: clampPowerValue(current.magic + (rewards.magic || 0)),
      stamina: clampPowerValue(current.stamina + (rewards.stamina || 0)),
      wisdom: clampPowerValue(current.wisdom + (rewards.wisdom || 0)),
      courage: clampPowerValue(current.courage + (rewards.courage || 0))
    };

    savePowerState(next);
    return next;
  }

  // ==============================
  // 7. 보상 모달 HTML
  // ==============================
  function buildRewardItemHTML(type, amount) {
    const label = POWER_LABELS[type];
    const description = POWER_DESCRIPTIONS[type];
    const imagePath = POWER_ICON_MAP[type] || "";
    const fallback = POWER_ICON_FALLBACK[type] || "⭐";

    if (!amount || amount <= 0) {
      return "";
    }

    return `
      <div class="individual-reward-item">
        <div class="individual-reward-icon-wrap">
          ${
            imagePath
              ? `<img src="${imagePath}" alt="${label}" class="individual-reward-icon"
                     onerror="this.style.display='none'; this.nextElementSibling.style.display='flex';">`
              : ""
          }
          <div class="individual-reward-fallback" ${imagePath ? 'style="display:none;"' : ""}>${fallback}</div>
        </div>
        <div class="individual-reward-text">
          <div class="individual-reward-name">${label}</div>
          <div class="individual-reward-amount">+${amount}</div>
          <div class="individual-reward-desc">${description}</div>
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

        <div class="individual-reward-notice">
          ※ 현재 보상값은 임시 세팅값이며, 나중에 게임 능력치와 맞춰 조정될 수 있어요.
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

    // 능력치 반영
    applyRewardValues(preset.rewards);

    // 보상 이력 기록
    addRewardHistory(rewardKey);

    // 보상 모달 표시
    showRewardModal(preset);

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
    return `
      <div class="individual-power-row">
        <div class="individual-power-row-name">${POWER_LABELS[type]} ${value} / ${MAX_POWER}</div>
        <div class="individual-power-row-desc">${POWER_DESCRIPTIONS[type]}</div>
      </div>
    `;
  }

  function openIndividualPowerModal() {
    removePowerModal();

    const power = getPowerState();

    const overlay = document.createElement("div");
    overlay.id = "individualPowerModalOverlay";
    overlay.className = "individual-power-overlay";

    overlay.innerHTML = `
      <div class="individual-power-modal">
        <button type="button" class="individual-power-close" id="individualPowerCloseBtn">×</button>

        <div class="individual-power-top-text">현재까지 모은 나의 힘</div>
        <h2 class="individual-power-title">네 가지 힘이 얼마나 자랐는지 볼 수 있어요</h2>

        <div class="individual-power-list" id="individualPowerListArea">
          ${buildPowerRowHTML("magic", power.magic)}
          ${buildPowerRowHTML("stamina", power.stamina)}
          ${buildPowerRowHTML("wisdom", power.wisdom)}
          ${buildPowerRowHTML("courage", power.courage)}
        </div>

        <div class="individual-power-notice">
          ※ 현재 능력치와 보상값은 임시 세팅값이며, 추후 게임 능력치 기준에 맞춰 바뀔 수 있어요.
        </div>

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
  }

  function refreshPowerModalIfOpen() {
    const modal = document.getElementById("individualPowerModalOverlay");
    if (!modal) return;

    const listArea = document.getElementById("individualPowerListArea");
    if (!listArea) return;

    const power = getPowerState();
    listArea.innerHTML = `
      ${buildPowerRowHTML("magic", power.magic)}
      ${buildPowerRowHTML("stamina", power.stamina)}
      ${buildPowerRowHTML("wisdom", power.wisdom)}
      ${buildPowerRowHTML("courage", power.courage)}
    `;
  }

  // ==============================
  // 10. 외부에서 쓰도록 열어두기
  // ==============================
  window.openIndividualPowerModal = openIndividualPowerModal;
  window.givePowerRewardOnce = givePowerRewardOnce;
  window.getIndividualPowerState = getPowerState;
  window.REWARD_PRESETS = REWARD_PRESETS;

  // ==============================
  // 11. 페이지 로드 시 기본 저장값 보장
  // ==============================
  document.addEventListener("DOMContentLoaded", function () {
    getPowerState();
    getRewardHistory();
  });
})();