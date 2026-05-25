/*
  파일명: auth.js
  역할: Victory 프로젝트의 로그인 기능과 출처 모달 기능을 담당합니다.

  이 파일에서 하는 일:
  1. 로그인 폼에서 아이디, 비밀번호, 역할을 가져옵니다.
  2. 백엔드 로그인 API와 연결할 준비를 합니다.
  3. 백엔드 연결 전에는 임시 로그인으로 화면 이동을 테스트합니다.
  4. 학생이 처음 로그인하면 story-intro.html로 이동하게 합니다.
  5. 사용 자료 출처 버튼을 누르면 출처 모달을 엽니다.
  6. source-data.js에 적어둔 출처 목록을 화면에 표시합니다.
*/

// 백엔드 서버 기본 주소입니다.
// 나중에 실제 Spring Boot 서버와 연결할 때 사용합니다.
const BASE_URL = "http://localhost:8080";

// 로그인 폼을 가져옵니다.
const loginForm = document.getElementById("loginForm");

// 출처 모달 열기 버튼을 가져옵니다.
const openSourceModalButton = document.getElementById("openSourceModal");

// 출처 모달 닫기 버튼을 가져옵니다.
const closeSourceModalButton = document.getElementById("closeSourceModal");

// 출처 모달 전체 영역을 가져옵니다.
const sourceModal = document.getElementById("sourceModal");

// 출처 목록이 들어갈 영역을 가져옵니다.
const sourceList = document.getElementById("sourceList");

/*
  로그인 폼이 존재하면 submit 이벤트를 연결합니다.
  index.html에서 로그인 버튼을 눌렀을 때 handleLogin 함수가 실행됩니다.
*/
if (loginForm) {
  loginForm.addEventListener("submit", handleLogin);
}

/*
  출처 모달 열기 버튼이 존재하면 클릭 이벤트를 연결합니다.
*/
if (openSourceModalButton) {
  openSourceModalButton.addEventListener("click", openSourceModal);
}

/*
  출처 모달 닫기 버튼이 존재하면 클릭 이벤트를 연결합니다.
*/
if (closeSourceModalButton) {
  closeSourceModalButton.addEventListener("click", closeSourceModal);
}

/*
  모달의 어두운 배경을 클릭하면 모달이 닫히게 합니다.
*/
if (sourceModal) {
  sourceModal.addEventListener("click", function (event) {
    if (event.target === sourceModal) {
      closeSourceModal();
    }
  });
}

/*
  함수명: handleLogin
  역할: 로그인 버튼을 눌렀을 때 실행되는 함수입니다.

  실제 백엔드 API:
  POST /api/auth/login

  요청 데이터:
  {
    username: "아이디",
    password: "비밀번호",
    role: "student 또는 teacher"
  }

  지금은 백엔드 연결 전이므로 tempLogin 함수를 사용해 화면 이동을 먼저 테스트합니다.
*/
async function handleLogin(event) {
  // form 태그의 기본 새로고침을 막습니다.
  event.preventDefault();

  // 아이디 입력값을 가져옵니다.
  const username = document.getElementById("username").value;

  // 비밀번호 입력값을 가져옵니다.
  const password = document.getElementById("password").value;

  // 선택된 역할 값을 가져옵니다.
  const role = document.querySelector("input[name='role']:checked").value;

  // 아이디가 비어 있으면 안내합니다.
  if (!username) {
    alert("아이디를 입력해주세요.");
    return;
  }

  // 비밀번호가 비어 있으면 안내합니다.
  if (!password) {
    alert("비밀번호를 입력해주세요.");
    return;
  }

  /*
    지금은 백엔드 로그인 API가 완전히 연결되기 전이므로
    임시 로그인으로 화면 이동을 먼저 테스트합니다.

    나중에 백엔드 연결할 때는 아래 두 줄을 주석 처리하고,
    그 아래 fetch 코드를 사용하면 됩니다.
  */
  tempLogin(username, role);
  return;

  /*
    ===== 백엔드 연결용 코드 =====
    백엔드 로그인 API가 완성되면 위의 tempLogin 부분을 막고 이 코드를 사용합니다.
  */

  try {
    /*
      백엔드 API 호출 부분입니다.
      호출 API: POST http://localhost:8080/api/auth/login
    */
    const response = await fetch(`${BASE_URL}/api/auth/login`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json"
      },
      body: JSON.stringify({
        username: username,
        password: password,
        role: role
      })
    });

    // 401 응답이면 로그인 실패로 처리합니다.
    if (response.status === 401) {
      alert("아이디 또는 비밀번호가 올바르지 않습니다.");
      return;
    }

    // 응답을 JSON으로 변환합니다.
    const result = await response.json();

    // 백엔드 공통 응답에서 success가 false인 경우입니다.
    if (result.success === false) {
      alert(result.message || "로그인에 실패했습니다.");
      return;
    }

    // 로그인 성공 후 토큰과 사용자 정보를 저장합니다.
    localStorage.setItem("token", result.data.token);
    localStorage.setItem("role", result.data.role);
    localStorage.setItem("name", result.data.name);
    localStorage.setItem("userId", result.data.id);

    // 역할에 따라 이동합니다.
    moveAfterLogin(result.data.role);

  } catch (error) {
    // 서버가 꺼져 있거나 주소가 틀렸을 때 실행됩니다.
    console.error("로그인 오류:", error);
    alert("서버와 연결할 수 없습니다. 백엔드 서버가 켜져 있는지 확인해주세요.");
  }
}

/*
  함수명: tempLogin
  역할: 백엔드 연결 전 화면 이동을 테스트하는 임시 로그인 함수입니다.

  설명:
  - 실제 서버 로그인 없이 localStorage에 임시 정보를 저장합니다.
  - 학생이면 처음 1회만 story-intro.html로 이동합니다.
  - 교사면 teacher/home.html로 이동합니다.
*/
function tempLogin(username, role) {
  // 임시 토큰을 저장합니다.
  localStorage.setItem("token", "temp-token");

  // 사용자 역할을 저장합니다.
  localStorage.setItem("role", role);

  // 사용자 이름을 임시로 저장합니다.
  localStorage.setItem("name", username);

  // 사용자 id를 임시로 저장합니다.
  localStorage.setItem("userId", "1");

  // 역할에 따라 이동합니다.
  moveAfterLogin(role);
}

/*
  함수명: moveAfterLogin
  역할: 로그인 후 역할에 따라 이동할 화면을 결정합니다.

  학생:
  - 처음 로그인한 경우 story-intro.html로 이동
  - 이미 스토리를 봤으면 student/home.html로 이동

  교사:
  - teacher/home.html로 이동
*/
function moveAfterLogin(role) {
  // 학생으로 로그인한 경우입니다.
  if (role === "student") {
    // 스토리 인트로를 봤는지 확인합니다.
    const hasSeenStoryIntro = localStorage.getItem("hasSeenStoryIntro");

    // 처음이면 스토리 화면으로 이동합니다.
    if (hasSeenStoryIntro !== "true") {
      window.location.href = "./student/story-intro.html";
      return;
    }

    // 이미 스토리를 봤으면 학생 홈으로 이동합니다.
    window.location.href = "./student/home.html";
    return;
  }

  // 교사로 로그인한 경우입니다.
  if (role === "teacher") {
    window.location.href = "./teacher/home.html";
    return;
  }

  // 역할 정보가 이상할 경우 안내합니다.
  alert("사용자 역할 정보를 확인할 수 없습니다.");
}

/*
  함수명: openSourceModal
  역할: 사용 자료 출처 모달을 엽니다.
*/
function openSourceModal() {
  // 출처 목록을 먼저 화면에 그립니다.
  renderSourceList();

  // hidden 클래스를 제거해서 모달을 보이게 합니다.
  sourceModal.classList.remove("hidden");
}

/*
  함수명: closeSourceModal
  역할: 사용 자료 출처 모달을 닫습니다.
*/
function closeSourceModal() {
  // hidden 클래스를 추가해서 모달을 숨깁니다.
  sourceModal.classList.add("hidden");
}

/*
  함수명: renderSourceList
  역할: source-data.js에 있는 sourceData 배열을 화면에 표시합니다.
*/
function renderSourceList() {
  // sourceList 영역이 없으면 함수를 종료합니다.
  if (!sourceList) {
    return;
  }

  // sourceData가 없거나 배열이 아니면 안내 문구를 보여줍니다.
  if (!Array.isArray(sourceData)) {
    sourceList.innerHTML = `
      <p class="empty-source">
        출처 데이터가 아직 연결되지 않았습니다.
      </p>
    `;
    return;
  }

  // sourceData 배열을 HTML 문자열로 변환합니다.
  const sourceHTML = sourceData.map(function (item) {
    return `
      <article class="source-item">
        <h3>${item.name}</h3>
        <p><strong>종류:</strong> ${item.type}</p>
        <p><strong>출처:</strong> ${item.source}</p>
        <p><strong>제작/생성 도구:</strong> ${item.tool}</p>
        <p><strong>라이선스/사용 조건:</strong> ${item.license}</p>
        <p><strong>사용 위치:</strong> ${item.usedIn}</p>
        <p><strong>수정 여부:</strong> ${item.modified}</p>
        <p><strong>비고:</strong> ${item.note}</p>
      </article>
    `;
  }).join("");

  // 완성된 HTML을 sourceList 영역에 넣습니다.
  sourceList.innerHTML = sourceHTML;
}

/*
  함수명: checkLogin
  역할: 로그인이 필요한 페이지에서 로그인 여부를 확인합니다.

  사용 예정 위치:
  - student/home.html
  - student/story-intro.html
  - teacher/home.html
*/
function checkLogin() {
  // 저장된 토큰을 가져옵니다.
  const token = localStorage.getItem("token");

  // 토큰이 없으면 로그인 화면으로 이동합니다.
  if (!token) {
    alert("로그인이 필요합니다.");
    window.location.href = "../index.html";
  }
}

/*
  함수명: logout
  역할: 로그아웃 처리 후 첫 화면으로 이동합니다.
*/
function logout() {
  // 저장된 로그인 정보를 삭제합니다.
  localStorage.removeItem("token");
  localStorage.removeItem("role");
  localStorage.removeItem("name");
  localStorage.removeItem("userId");

  // 첫 화면으로 이동합니다.
  window.location.href = "../index.html";
}/* 사용 자료 출처 모달 열기/닫기 안전 연결 코드 */
document.addEventListener("DOMContentLoaded", function () {
  var openSourceButton = document.getElementById("openSourceButton");
  var closeSourceButton = document.getElementById("closeSourceButton");
  var sourceModal = document.getElementById("sourceModal");
  var sourceList = document.getElementById("sourceList");

  if (!openSourceButton || !sourceModal || !sourceList) {
    return;
  }

  openSourceButton.addEventListener("click", function () {
    sourceList.innerHTML = "";

    if (!window.sourceData && typeof sourceData === "undefined") {
      sourceList.innerHTML = "<p class='empty-source'>출처 데이터를 찾을 수 없습니다.</p>";
    } else {
      var data = window.sourceData || sourceData;

      data.forEach(function (item) {
        var sourceItem = document.createElement("div");
        sourceItem.className = "source-item";

        sourceItem.innerHTML =
          "<strong>" + item.name + "</strong>" +
          "<p><b>분류:</b> " + item.type + "</p>" +
          "<p><b>출처:</b> " + item.source + "</p>" +
          "<p><b>도구:</b> " + item.tool + "</p>" +
          "<p><b>라이선스:</b> " + item.license + "</p>" +
          "<p><b>사용 위치:</b> " + item.usedIn + "</p>" +
          "<p><b>수정 여부:</b> " + item.modified + "</p>" +
          "<p><b>비고:</b> " + item.note + "</p>";

        sourceList.appendChild(sourceItem);
      });
    }

    sourceModal.classList.remove("hidden");
  });

  if (closeSourceButton) {
    closeSourceButton.addEventListener("click", function () {
      sourceModal.classList.add("hidden");
    });
  }

  sourceModal.addEventListener("click", function (event) {
    if (event.target === sourceModal) {
      sourceModal.classList.add("hidden");
    }
  });
});