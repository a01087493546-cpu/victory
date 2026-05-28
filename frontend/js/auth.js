/*
  파일명: auth.js
  역할: 문답책 프로젝트의 로그인 기능과 출처 모달 기능을 담당합니다.

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
const openSourceModalButton = document.getElementById("openSourceButton");

// 출처 모달 닫기 버튼을 가져옵니다.
const closeSourceModalButton = document.getElementById("closeSourceButton");

// 출처 모달 전체 영역을 가져옵니다.
const sourceModal = document.getElementById("sourceModal");

// 출처 목록이 들어갈 영역을 가져옵니다.
const sourceList = document.getElementById("sourceList");

/*
  로그인 폼이 존재하면 submit 이벤트를 연결합니다.
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
*/
async function handleLogin(event) {
  event.preventDefault();

  const username = document.getElementById("username").value;
  const password = document.getElementById("password").value;
  const role = document.querySelector("input[name='role']:checked").value;

  if (!username) {
    alert("아이디를 입력해주세요.");
    return;
  }

  if (!password) {
    alert("비밀번호를 입력해주세요.");
    return;
  }

  /*
    지금은 임시 로그인으로 화면 이동을 테스트합니다.
    나중에 백엔드 연결할 때는 아래 두 줄을 주석 처리하고,
    그 아래 fetch 코드를 사용하면 됩니다.
  */
  tempLogin(username, role);
  return;

  /*
    ===== 백엔드 연결용 코드 =====
    백엔드 로그인 API가 완성되면 위의 tempLogin 부분을 막고 이 코드를 사용합니다.
    나중에: POST /api/auth/login
  */
  try {
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

    if (response.status === 401) {
      alert("아이디 또는 비밀번호가 올바르지 않습니다.");
      return;
    }

    const result = await response.json();

    if (result.success === false) {
      alert(result.message || "로그인에 실패했습니다.");
      return;
    }

    // 로그인 성공 후 사용자 정보를 sessionStorage에 저장합니다.
    sessionStorage.setItem("token", result.data.token);
    sessionStorage.setItem("role", result.data.role);
    sessionStorage.setItem("name", result.data.name);
    sessionStorage.setItem("studentId", result.data.id);

    moveAfterLogin(result.data.role);

  } catch (error) {
    console.error("로그인 오류:", error);
    alert("서버와 연결할 수 없습니다. 백엔드 서버가 켜져 있는지 확인해주세요.");
  }
}

/*
  함수명: tempLogin
  역할: 백엔드 연결 전 화면 이동을 테스트하는 임시 로그인 함수입니다.
*/
function tempLogin(username, role) {
  // localStorage 대신 sessionStorage 사용
  sessionStorage.setItem("token", "temp-token");
  sessionStorage.setItem("role", role);
  sessionStorage.setItem("name", username);

  // userId 대신 studentId로 통일
  // 나중에 백엔드에서 실제 studentId 받아올 예정
  sessionStorage.setItem("studentId", "1");

  moveAfterLogin(role);
}

/*
  함수명: moveAfterLogin
  역할: 로그인 후 역할에 따라 이동할 화면을 결정합니다.
*/
function moveAfterLogin(role) {
  if (role === "student") {
    const studentId = sessionStorage.getItem("studentId") || "1";

    // studentId별로 스토리 인트로를 봤는지 확인합니다.
    // 학생마다 다르게 저장되므로 여러 학생이 같은 브라우저를 써도 구분됩니다.
    const hasSeenStoryIntro = sessionStorage.getItem("hasSeenStoryIntro_" + studentId);

    if (hasSeenStoryIntro !== "true") {
      window.location.href = "./student/story-intro.html";
      return;
    }

    window.location.href = "./student/home.html";
    return;
  }

  if (role === "teacher") {
    window.location.href = "./teacher/home.html";
    return;
  }

  alert("사용자 역할 정보를 확인할 수 없습니다.");
}

/*
  함수명: checkLogin
  역할: 로그인이 필요한 페이지에서 로그인 여부를 확인합니다.

  사용 방법:
  로그인이 필요한 모든 화면 JS 파일 상단에 checkLogin() 한 줄 추가하면 됩니다.
*/
function checkLogin() {
  const token = sessionStorage.getItem("token");

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
  sessionStorage.removeItem("token");
  sessionStorage.removeItem("role");
  sessionStorage.removeItem("name");
  sessionStorage.removeItem("studentId");

  // sessionStorage를 사용하므로 브라우저 세션이 유지되는 동안만 인트로 확인 기록이 유지됩니다.
  // 나중에 백엔드 연동 시 studentId별 스토리 확인 여부를 DB에서 관리할 수 있습니다.
  window.location.href = "../index.html";
}

/*
  함수명: openSourceModal
  역할: 사용 자료 출처 모달을 엽니다.
*/
function openSourceModal() {
  renderSourceList();
  sourceModal.classList.remove("hidden");
}

/*
  함수명: closeSourceModal
  역할: 사용 자료 출처 모달을 닫습니다.
*/
function closeSourceModal() {
  sourceModal.classList.add("hidden");
}

/*
  함수명: renderSourceList
  역할: source-data.js에 있는 sourceData 배열을 화면에 표시합니다.
*/
function renderSourceList() {
  if (!sourceList) return;

  if (!Array.isArray(sourceData)) {
    sourceList.innerHTML = `
      <p class="empty-source">
        출처 데이터가 아직 연결되지 않았습니다.
      </p>
    `;
    return;
  }

<<<<<<< HEAD
  sourceList.innerHTML = sourceData.map(function (item) {
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
=======
  sourceList.innerHTML = `
    <table class="source-table">
      <thead>
        <tr>
          <th>번호</th>
          <th>자료형태</th>
          <th>파일명</th>
          <th>자료 설명</th>
          <th>비고</th>
        </tr>
      </thead>
      <tbody>
        ${sourceData.map(function (item, index) {
          return `
            <tr>
              <td>${index + 1}</td>
              <td>${item.type || "-"}</td>
              <td>${item.fileName || item.name || "-"}</td>
              <td>${item.description || item.usedIn || "-"}</td>
              <td>${item.note || item.source || "-"}</td>
            </tr>
          `;
        }).join("")}
      </tbody>
    </table>
  `;
>>>>>>> origin/jiao
}