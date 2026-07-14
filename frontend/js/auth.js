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

/*
  함수명: logoutAndGoLogin
  역할: 로그아웃 버튼 공통 처리입니다. sessionStorage에 저장된 로그인 정보를
  모두 지우고 로그인 화면으로 이동합니다. auth.js를 불러오는 화면이면
  어디서든 이 함수를 그대로 재사용하면 됩니다.
*/
function logoutAndGoLogin() {
  sessionStorage.removeItem("token");
  sessionStorage.removeItem("studentId");
  sessionStorage.removeItem("role");
  sessionStorage.removeItem("name");
  sessionStorage.removeItem("loginId");

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
    /* 백엔드 로그인 API를 호출합니다. */
    const response = await fetch("http://localhost:8080/api/auth/login", {
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
      throw new Error("아이디 또는 비밀번호가 올바르지 않습니다.");
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

    /* 학생 로그인 처리입니다. */
    if (data.role === "student") {
      const studentId = String(data.id);
      const hasSeenStoryIntro =
        sessionStorage.getItem("hasSeenStoryIntro_" + studentId);

      /* 스토리를 본 적 있으면 학생 홈으로 이동합니다. */
      if (hasSeenStoryIntro === "true") {
        window.location.href = "./student/individual-reading.html";
      }

      /* 첫 로그인이라면 스토리 인트로 화면으로 이동합니다. */
      window.location.href = "./student/story-intro.html";
      return;
    }

    /* 교사 로그인 처리입니다. */
    if (data.role === "teacher") {
      window.location.href = "./teacher/home.html";
    }
  } catch (error) {
    console.error(error);
    alert(error.message || "로그인 중 오류가 발생했습니다.");
  }
});
});
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