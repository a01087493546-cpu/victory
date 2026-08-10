/*
  파일명: auth.js
  역할: 문답책 로그인, 역할 선택, 로그인 확인 기능을 담당합니다.

  중요 규칙:
  1. 로그인 정보는 localStorage가 아니라 sessionStorage만 사용합니다.
  2. 학생/교사 역할을 구분해서 저장합니다.
  3. 학생은 첫 로그인일 때 스토리 화면으로 이동합니다.
  4. 스토리를 본 학생은 바로 학생 홈으로 이동합니다.
*/

/* 현재 선택된 역할입니다. 기본값은 학생입니다. */
let selectedRole = "student";

function getLoginApiBaseUrl() {
  if (window.location.hostname === "127.0.0.1") {
    return "http://127.0.0.1:8080";
  }

  return "http://localhost:8080";
}

async function readLoginErrorMessage(response) {
  const bodyText = await response.text().catch(function () {
    return "";
  });

  if (!bodyText) {
    return "아이디 또는 비밀번호가 올바르지 않습니다.";
  }

  try {
    const body = JSON.parse(bodyText);
    return body.message || "아이디 또는 비밀번호가 올바르지 않습니다.";
  } catch (error) {
    return "아이디 또는 비밀번호가 올바르지 않습니다.";
  }
}

/*
  함수명: checkLogin
  역할: 보호된 페이지에 로그인하지 않은 사용자가 들어오지 못하게 막습니다.
*/
function checkLogin() {
  const token = sessionStorage.getItem("token");

  /* 로그인 정보가 없으면 로그인 화면으로 돌려보냅니다. */
  if (!token) {
    alert("로그인이 필요합니다.");
    window.location.href = "../index.html";
  }
}

function isDemoAccount() {
  return sessionStorage.getItem("demoAccount") === "true";
}

/* 학생 인트로 두 단계의 다음 목적지를 한 곳에서 결정한다. */
function getStudentIntroDestination(status, studentBasePath) {
  const basePath = studentBasePath || "./student/";

  if (!status.storyIntroSeen) return basePath + "story-intro.html";
  if (!status.powerIntroSeen) return basePath + "ability-intro.html";
  return basePath + "individual-reading.html";
}

function getDemoIntroStatus() {
  return {
    storyIntroSeen:
      typeof loadDemoState === "function" && loadDemoState("storyIntroSeen", false) === true,
    powerIntroSeen:
      typeof loadDemoState === "function" && loadDemoState("powerIntroSeen", false) === true
  };
}

async function loadCurrentStudentIntroStatus() {
  if (isDemoAccount()) return getDemoIntroStatus();

  const token = sessionStorage.getItem("token");
  if (!token) return null;

  const response = await fetch(getLoginApiBaseUrl() + "/api/students/me/intro-status", {
    headers: { Authorization: "Bearer " + token }
  });

  if (!response.ok) throw new Error("인트로 완료 상태 조회 실패: " + response.status);

  const data = await response.json();
  return {
    storyIntroSeen: data.hasSeenStoryIntro === true,
    powerIntroSeen: data.hasSeenPowerIntro === true
  };
}

/* 완료한 인트로 URL로 직접 들어온 학생과 교사를 올바른 화면으로 돌려보낸다. */
async function guardStudentIntroDirectAccess() {
  const pageName = window.location.pathname.split("/").pop();
  if (pageName !== "story-intro.html" && pageName !== "ability-intro.html") return;

  const role = sessionStorage.getItem("role");
  if (role !== "student") {
    if (role === "teacher") window.location.replace("../teacher/home.html");
    return;
  }

  try {
    const status = await loadCurrentStudentIntroStatus();
    if (!status) return;

    const destination = getStudentIntroDestination(status, "./");
    if (destination !== "./" + pageName) window.location.replace(destination);
  } catch (error) {
    console.error("학생 인트로 진입 상태를 확인하지 못했습니다.", error);
  }
}

/*
  심사 안내는 HTML에서 hidden을 기본값으로 둔다. 화면별 배너 CSS의
  display:flex/inline-flex가 브라우저 기본 [hidden] 규칙을 덮더라도 일반
  계정에 잠깐 노출되지 않도록 공통 안전 규칙을 가장 먼저 설치한다.
*/
(function installDemoBannerVisibilityGuard() {
  if (document.getElementById("demoBannerVisibilityGuard")) return;
  const style = document.createElement("style");
  style.id = "demoBannerVisibilityGuard";
  style.textContent = [
    ".mq-demo-banner[hidden]",
    ".mq-demo-guide[hidden]",
    ".iar-demo-example-banner[hidden]",
    ".ir-demo-banner[hidden]",
    "[data-demo-banner][hidden]"
  ].join(",") + "{display:none!important;}";
  (document.head || document.documentElement).appendChild(style);
})();

/*
  함수명: showDemoBannerIfDemoAccount
  역할: 연습읽기 질문 작성 화면들이 공통으로 쓰는 "심사 체험 안내" 배지를
  켠다. 심사계정에서만 배지를 보여주고, 일반 계정에서는 hidden 상태를
  그대로 유지한다. 화면마다 같은 isDemoAccount() 분기를 반복해서 쓰지
  않도록 여기 한 곳에 모아 둔다.
*/
function showDemoBannerIfDemoAccount(elementId) {
  const banner = document.getElementById(elementId || "demoExperienceBanner");
  if (banner) {
    banner.hidden = !isDemoAccount();
  }
}

/*
  함수명: logoutAndGoLogin
  역할: 로그아웃 버튼 공통 처리입니다. sessionStorage에 저장된 로그인 정보를
  모두 지우고 로그인 화면으로 이동합니다. auth.js를 불러오는 화면이면
  어디서든 이 함수를 그대로 재사용하면 됩니다.
*/
function logoutAndGoLogin() {
  /*
    심사 체험 기록(mq_demo_ prefix, localStorage)은 로그아웃해도 지우지
    않는다 — 심사위원이 로그아웃 후 다시 로그인해도 같은 브라우저에서는
    작성했던 내용이 그대로 남아 있어야 한다. 심사 체험 기록을 지우고 싶으면
    화면에 있는 "심사 체험 기록 초기화" 버튼(clearAllDemoState 호출)을
    직접 눌러야 한다. 여기서는 로그인 세션 정보만 정리한다.
  */

  sessionStorage.removeItem("token");
  sessionStorage.removeItem("studentId");
  sessionStorage.removeItem("role");
  sessionStorage.removeItem("name");
  sessionStorage.removeItem("loginId");
  sessionStorage.removeItem("demoAccount");

  window.location.href = "../index.html";
}

/*
  HTML 화면이 모두 준비된 뒤 로그인 기능을 연결합니다.
*/
document.addEventListener("DOMContentLoaded", function () {
  /* 로그인 form을 가져옵니다. */
  const loginForm = document.getElementById("loginForm");

  /* 아이디/비밀번호 입력칸을 가져옵니다. */
  const loginIdInput = document.getElementById("loginId");
  const loginPasswordInput = document.getElementById("loginPassword");

  /* 학생/교사 역할 버튼을 가져옵니다. */
  const studentRoleButton = document.getElementById("studentRoleButton");
  const teacherRoleButton = document.getElementById("teacherRoleButton");

  document.querySelectorAll("[data-demo-role]").forEach(function (button) {
    button.addEventListener("click", function () {
      const role = button.dataset.demoRole;
      loginIdInput.value = role === "teacher" ? "tt11" : "ss01";
      loginPasswordInput.value = role === "teacher" ? "tt11" : "ss01";
      (role === "teacher" ? teacherRoleButton : studentRoleButton).click();
      loginIdInput.focus();
    });
  });

  document.querySelectorAll("[data-copy]").forEach(function (button) {
    button.addEventListener("click", async function () {
      const value = button.dataset.copy;
      const message = document.getElementById("demoCopyMessage");
      try {
        let copied = false;
        if (navigator.clipboard && window.isSecureContext) {
          try {
            await navigator.clipboard.writeText(value);
            copied = true;
          } catch (clipboardError) {
            copied = false;
          }
        }
        if (!copied) {
          const temporary = document.createElement("textarea");
          temporary.value = value;
          temporary.style.position = "fixed";
          temporary.style.opacity = "0";
          document.body.appendChild(temporary);
          temporary.focus();
          temporary.select();
          temporary.setSelectionRange(0, temporary.value.length);
          copied = document.execCommand("copy");
          temporary.remove();
        }
        if (!copied) throw new Error("copy unavailable");
        message.textContent = button.dataset.label + "가 복사되었습니다.";
      } catch (error) {
        message.textContent = "복사하지 못했습니다. 표시된 값을 직접 입력해 주세요.";
      }
      window.setTimeout(function () { message.textContent = ""; }, 1800);
    });
  });

  /* 학생 역할 버튼 클릭 처리입니다. */
  if (studentRoleButton) {
    studentRoleButton.addEventListener("click", function () {
      selectedRole = "student";

      /* 선택된 버튼 표시를 바꿉니다. */
      studentRoleButton.classList.add("active");
      teacherRoleButton.classList.remove("active");
    });
  }

  /* 교사 역할 버튼 클릭 처리입니다. */
  if (teacherRoleButton) {
    teacherRoleButton.addEventListener("click", function () {
      selectedRole = "teacher";

      /* 선택된 버튼 표시를 바꿉니다. */
      teacherRoleButton.classList.add("active");
      studentRoleButton.classList.remove("active");
    });
  }

  /* 로그인 form이 없는 페이지에서는 여기서 멈춥니다. */
  if (!loginForm) {
    return;
  }

  /*
    로그인 버튼을 눌렀을 때 실행됩니다.
    button type="submit"이기 때문에 form submit 이벤트로 처리합니다.
  */
  loginForm.addEventListener("submit", async function (event) {
  /* form 기본 새로고침 동작을 막습니다. */
  event.preventDefault();

  const loginId = loginIdInput.value.trim();
  const loginPassword = loginPasswordInput.value.trim();

  /* 아이디 입력 확인입니다. */
  if (loginId === "") {
    alert("아이디를 입력해주세요.");
    loginIdInput.focus();
    return;
  }

  /* 비밀번호 입력 확인입니다. */
  if (loginPassword === "") {
    alert("비밀번호를 입력해주세요.");
    loginPasswordInput.focus();
    return;
  }

  try {
    const loginUrl = getLoginApiBaseUrl() + "/api/auth/login";

    /* 백엔드 로그인 API를 호출합니다. */
    const response = await fetch(loginUrl, {
      method: "POST",
      headers: {
        "Content-Type": "application/json"
      },
      body: JSON.stringify({
        loginId: loginId,
        password: loginPassword
      })
    });

    if (!response.ok) {
      const message = await readLoginErrorMessage(response);
      console.error("로그인 요청 실패", {
        url: loginUrl,
        status: response.status,
        statusText: response.statusText,
        message: message
      });
      alert(message);
      return;
    }

    /* 백엔드에서 받은 로그인 결과입니다. */
    const data = await response.json();

    /* 로그인 화면에서 선택한 역할과 실제 계정 역할이 같은지 확인합니다. */
    if (data.role !== selectedRole) {
      alert(
        data.role === "student"
          ? "학생 계정입니다. 학생 로그인을 선택해주세요."
          : "교사 계정입니다. 교사 로그인을 선택해주세요."
      );
      return;
    }

    /* 실제 JWT 토큰과 숫자 사용자 ID를 저장합니다. */
    sessionStorage.setItem("token", data.token);
    sessionStorage.setItem("studentId", String(data.id));
    sessionStorage.setItem("role", data.role);
    sessionStorage.setItem("name", data.name);
    sessionStorage.setItem("loginId", data.loginId);
    sessionStorage.setItem("demoAccount", String(data.demoAccount === true));

    /* 학생 로그인 처리입니다. */
    if (data.role === "student") {
      /*
        시작 스토리/나의 힘 인트로는 "최초 로그인 때만" 순서대로 보여준다.
        일반 학생은 계정 기준(DB의 has_seen_story_intro/has_seen_power_intro,
        로그인 응답에 함께 내려온다)으로 판단하고, 심사계정(ss01)은 여러
        심사위원이 같은 계정을 같이 쓰므로 DB 값 대신 "이 브라우저"의
        localStorage(demo-storage.js)로 따로 판단한다.
      */
      const isDemo = data.demoAccount === true;
      const introStatus = isDemo
        ? getDemoIntroStatus()
        : {
            storyIntroSeen: data.hasSeenStoryIntro === true,
            powerIntroSeen: data.hasSeenPowerIntro === true
          };

      window.location.href = getStudentIntroDestination(introStatus, "./student/");
      return;
    }

    /* 교사 로그인 처리입니다. */
    if (data.role === "teacher") {
      window.location.href = "./teacher/home.html";
    }
  } catch (error) {
    console.error("로그인 요청 중 네트워크 오류", {
      url: getLoginApiBaseUrl() + "/api/auth/login",
      error: error
    });
    alert("서버에 연결할 수 없습니다. 잠시 후 다시 시도해 주세요.");
  }
});
});

document.addEventListener("DOMContentLoaded", guardStudentIntroDirectAccess);
/*
  사용 자료 출처 모달 기능입니다.
  로그인 화면의 '사용 자료 출처' 버튼을 누르면
  source-data.js에 저장된 출처 목록을 화면에 보여줍니다.
*/
document.addEventListener("DOMContentLoaded", function () {
  // 출처 버튼, 모달, 닫기 버튼, 출처 목록 영역을 가져옵니다.
  const openSourceButton = document.getElementById("openSourceButton");
  const closeSourceButton = document.getElementById("closeSourceButton");
  const sourceModal = document.getElementById("sourceModal");
  const sourceList = document.getElementById("sourceList");

  // 출처 버튼이 없는 화면에서는 아래 기능을 실행하지 않습니다.
  if (!openSourceButton || !sourceModal || !sourceList) {
    return;
  }

  // 사용 자료 출처 버튼을 눌렀을 때 모달을 엽니다.
  openSourceButton.addEventListener("click", function () {
    // hidden 클래스를 제거해서 모달을 화면에 보이게 합니다.
    sourceModal.classList.remove("hidden");

    // source-data.js의 sourceData 배열이 있으면 목록을 화면에 그립니다.
    renderSourceList();
  });

  // 닫기 버튼을 눌렀을 때 모달을 닫습니다.
  if (closeSourceButton) {
    closeSourceButton.addEventListener("click", function () {
      sourceModal.classList.add("hidden");
    });
  }

  // 모달 바깥 어두운 영역을 누르면 모달을 닫습니다.
  sourceModal.addEventListener("click", function (event) {
    if (event.target === sourceModal) {
      sourceModal.classList.add("hidden");
    }
  });

    /*
    함수명: renderSourceList
    역할: source-data.js에 있는 출처 정보를 화면에 표시합니다.
    source-data.js의 항목 이름이 조금 달라도 undefined가 뜨지 않게 처리합니다.
  */
  function renderSourceList() {
    const sourceList = document.getElementById("sourceList");

    if (!sourceList) {
      return;
    }

    sourceList.innerHTML = "";

    sourceData.forEach(function (item) {
      const sourceItem = document.createElement("article");
      sourceItem.className = "source-item";

      /*
        기존 출처 데이터와 새로 추가한 출처 데이터의 속성 이름이 다를 수 있으므로
        여러 이름을 함께 확인해서 안전하게 화면에 표시합니다.
      */
      const title = item.title || item.name || "제목 없음";
      const type = item.type || item.category || "분류 없음";
      const file = item.file || item.fileName || "파일명 없음";
      const description = item.description || item.license || "설명 없음";
      const source = item.source || "출처 없음";
      const location = item.location || item.usedIn || item.usage || "사용 위치 없음";
      const edit = item.edit || item.modified || "수정 여부 없음";
      const note = item.note || item.memo || item.remark || "비고 없음";

      sourceItem.innerHTML = `
        <strong>${title}</strong>
        <p><b>분류:</b> ${type}</p>
        <p><b>파일명:</b> ${file}</p>
        <p><b>설명:</b> ${description}</p>
        <p><b>출처:</b> ${source}</p>
        <p><b>사용 위치:</b> ${location}</p>
        <p><b>수정 여부:</b> ${edit}</p>
        <p><b>비고:</b> ${note}</p>
      `;

      sourceList.appendChild(sourceItem);
    });
  }
});
