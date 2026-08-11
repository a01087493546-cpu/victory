/*
  파일명: auth.js
  역할: 문답책 로그인, 역할 선택, 로그인 확인 기능을 담당합니다.

  중요 규칙:
  1. 로그인 정보는 localStorage가 아니라 sessionStorage만 사용합니다.
  2. 학생/교사 역할을 구분해서 저장합니다.
  3. 학생은 첫 로그인일 때 스토리 화면으로 이동합니다.
  4. 스토리를 본 학생은 바로 학생 홈으로 이동합니다.
*/

/*
  심사계정(mq_demo_*)은 localStorage가 source of truth인데, localStorage는
  브라우저 origin(프로토콜+호스트+포트) 단위로 완전히 분리된다.
  "http://localhost:PORT/..."와 "http://127.0.0.1:PORT/..."는 같은 컴퓨터의
  같은 서버를 가리켜도 서로 다른 origin이라 localStorage가 전혀 공유되지
  않는다 - story-intro.js/ability-intro.html에서 storyIntroSeen/
  powerIntroSeen을 아무리 정확히 저장해도, 다음에 "localhost"로 들어오면
  그 값이 전부 비어 있는 것처럼 보여 인트로가 처음부터 다시 뜬다(반대
  방향도 동일). 배포 도메인(hwani-dot.github.io 등)은 "localhost"라는
  문자열과 절대 일치하지 않으므로 이 리다이렉트는 로컬 개발 환경에만
  적용되고 실제 배포에는 영향이 없다. 로그인 폼이 뜨기도 전에, 가능한 한
  가장 이른 시점에 127.0.0.1로 통일해서 심사계정 상태가 항상 같은
  localStorage를 가리키게 만든다.
*/
(function normalizeLocalDevOrigin() {
  if (window.location.hostname !== "localhost") return;

  const canonical = new URL(window.location.href);
  canonical.hostname = "127.0.0.1";
  window.location.replace(canonical.toString());
})();

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

function normalizeIntroSeen(value) {
  return value === true || value === "true" || value === 1 || value === "1";
}

function readDemoIntroSeen(key) {
  if (typeof loadDemoState === "function") {
    return normalizeIntroSeen(loadDemoState(key, false));
  }

  /*
    로그인 화면의 스크립트가 비정상적인 캐시/로드 순서로 실행되더라도
    이미 저장된 브라우저별 인트로 완료값을 false로 오판하지 않는다.
    demo-storage.js와 같은 raw key 규칙을 사용하며 값을 변경하지는 않는다.
  */
  try {
    return normalizeIntroSeen(JSON.parse(localStorage.getItem("mq_demo_" + key) || "false"));
  } catch (error) {
    console.error("심사계정 인트로 완료 상태를 읽지 못했습니다.", error);
    return false;
  }
}

function getDemoIntroStatus() {
  return {
    storyIntroSeen: readDemoIntroSeen("storyIntroSeen"),
    powerIntroSeen: readDemoIntroSeen("powerIntroSeen")
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
    storyIntroSeen: normalizeIntroSeen(data.hasSeenStoryIntro),
    powerIntroSeen: normalizeIntroSeen(data.hasSeenPowerIntro)
  };
}

/*
  일반 학생 계정의 인트로 완료 상태를 DB에 저장한다. field는
  "story-intro-seen" 또는 "power-intro-seen"(컨트롤러 경로 그대로) - 두
  화면이 같은 모양의 PATCH를 각자 따로 구현하던 것을 여기 하나로 모았다.
  실패해도 화면 이동은 막지 않는다(다음 로그인 때 다시 보여주는 정도의
  가벼운 실패로 처리).
*/
async function markIntroSeenOnServer(field) {
  const token = sessionStorage.getItem("token");
  if (!token) return false;

  try {
    const response = await fetch(getLoginApiBaseUrl() + "/api/students/me/" + field, {
      method: "PATCH",
      headers: { Authorization: "Bearer " + token }
    });

    if (!response.ok) throw new Error(field + " 저장 실패: " + response.status);
    return true;
  } catch (error) {
    console.error("학생 인트로 완료 상태를 저장하지 못했습니다.", field, error);
    return false;
  }
}

/*
  "끝까지 완료했는가"가 아니라 "이 화면이 최초 자동 흐름으로 한 번
  보였는가"를 기준으로 seen을 저장한다. guardStudentIntroDirectAccess가
  "지금 이 페이지가 정확히 보여줘야 할 화면"이라고 확정한 직후에만
  호출하므로, 직접 재보기(?revisit=1)나 잘못된 화면 진입에서는 절대
  호출되지 않는다. 이미 true면 아무것도 하지 않는다(중복 저장 방지).
*/
async function markCurrentIntroPageSeen(pageName, status) {
  const isDemo = isDemoAccount();

  if (pageName === "story-intro.html" && !status.storyIntroSeen) {
    if (isDemo) {
      if (typeof saveDemoState === "function") saveDemoState("storyIntroSeen", true);
    } else {
      await markIntroSeenOnServer("story-intro-seen");
    }
  } else if (pageName === "ability-intro.html" && !status.powerIntroSeen) {
    if (isDemo) {
      if (typeof saveDemoState === "function") saveDemoState("powerIntroSeen", true);
    } else {
      await markIntroSeenOnServer("power-intro-seen");
    }
  }
}

/* 완료한 인트로 URL로 직접 들어온 학생과 교사를 올바른 화면으로 돌려보낸다. */
async function guardStudentIntroDirectAccess() {
  const pageName = window.location.pathname.split("/").pop();
  if (pageName !== "story-intro.html" && pageName !== "ability-intro.html") return;

  /*
    "나의 힘 쌓는 법" 버튼처럼, 이미 완료한 인트로를 학생이 스스로 다시
    보고 싶어서 들어온 경우(?revisit=1)에는 이 가드가 곧바로
    individual-reading.html로 되돌려 보내면 안 된다. 원래 이 가드는
    "완료한 인트로 URL을 직접 쳐서 들어온 경우"만 막으려던 것이라,
    다시보기 진입은 별도로 구분한다. revisit=1은 이미 seen=true인
    상태에서만 쓰는 진입이므로 markCurrentIntroPageSeen도 호출하지 않는다.
  */
  if (new URLSearchParams(window.location.search).get("revisit") === "1") return;

  const role = sessionStorage.getItem("role");
  if (role !== "student") {
    if (role === "teacher") window.location.replace("../teacher/home.html");
    return;
  }

  try {
    const status = await loadCurrentStudentIntroStatus();
    if (!status) return;

    const destination = getStudentIntroDestination(status, "./");
    if (destination !== "./" + pageName) {
      window.location.replace(destination);
      return;
    }

    /*
     * 여기 도달했다는 것은 "지금 이 화면이 최초 자동 흐름에서 정확히
     * 보여줘야 하는 화면"이라는 뜻이다 - 이 순간 seen=true로 저장해서,
     * 학생이 끝까지 보지 않고 중간에 돌아가기를 눌러도 다음 로그인부터
     * 다시 뜨지 않게 한다.
     */
    await markCurrentIntroPageSeen(pageName, status);
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
      let introStatus;

      if (isDemo) {
        introStatus = getDemoIntroStatus();
      } else {
        try {
          introStatus = await loadCurrentStudentIntroStatus();
        } catch (error) {
          console.error("로그인 후 인트로 완료 상태를 다시 확인하지 못했습니다.", error);
          introStatus = {
            storyIntroSeen: normalizeIntroSeen(data.hasSeenStoryIntro),
            powerIntroSeen: normalizeIntroSeen(data.hasSeenPowerIntro)
          };
        }
      }
      const introDestination = getStudentIntroDestination(introStatus, "./student/");
      /*
       * 인트로 리다이렉트가 다시 문제되면 이 한 줄이 "그 순간 실제로
       * 읽은 storySeen/powerSeen 값과 최종 목적지"를 곧바로 보여준다 -
       * origin 불일치(localhost/127.0.0.1) 같은 문제를 다시 추측하지
       * 않도록 남겨 둔다.
       */
      window.location.href = introDestination;
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
