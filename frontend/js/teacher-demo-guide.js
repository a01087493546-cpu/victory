/* 교사 심사계정의 관리 화면별 최초 1회 보조 안내. */
(function installTeacherDemoGuideStyle() {
  if (document.getElementById("teacherDemoGuideStyle")) return;

  const style = document.createElement("style");
  style.id = "teacherDemoGuideStyle";
  style.textContent = `
    .teacher-demo-guide {
      position: fixed;
      top: 108px;
      right: 32px;
      z-index: 2000;
      width: min(480px, calc(100vw - 40px));
      padding: 22px 54px 21px 24px;
      border: 2px solid rgba(178, 126, 50, 0.68);
      border-radius: 20px;
      background: linear-gradient(145deg, rgba(255, 252, 239, 0.98), rgba(244, 226, 190, 0.98));
      box-shadow: 0 16px 36px rgba(70, 39, 14, 0.24);
      color: #5d3a1d;
      font-family: "Pretendard", "Apple SD Gothic Neo", sans-serif;
      word-break: keep-all;
    }
    .teacher-demo-guide[hidden] { display: none !important; }
    .teacher-demo-guide__title {
      margin: 0 0 12px;
      color: #7a4a12;
      font-size: 19px;
      font-weight: 900;
      line-height: 1.3;
    }
    .teacher-demo-guide__text {
      margin: 0;
      font-size: 17px;
      font-weight: 650;
      line-height: 1.55;
    }
    .teacher-demo-guide__text + .teacher-demo-guide__text { margin-top: 8px; }
    .teacher-demo-guide__close {
      position: absolute;
      top: 12px;
      right: 13px;
      width: 34px;
      height: 34px;
      border: 1px solid rgba(126, 78, 28, 0.34);
      border-radius: 50%;
      background: rgba(255, 250, 232, 0.92);
      color: #744719;
      font: 900 20px/1 "Pretendard", sans-serif;
      cursor: pointer;
    }
    .teacher-demo-guide__close:hover { background: #f4dfb4; }
    @media (max-width: 760px) {
      .teacher-demo-guide { top: 78px; right: 20px; }
    }
  `;
  document.head.appendChild(style);
})();

function showTeacherDemoGuide(stateKey) {
  if (typeof isDemoAccount !== "function" || !isDemoAccount()) return;
  if (typeof loadDemoState !== "function" || typeof saveDemoState !== "function") return;
  if (loadDemoState(stateKey, false) === true) return;

  const guide = document.createElement("aside");
  guide.className = "teacher-demo-guide";
  guide.setAttribute("role", "dialog");
  guide.setAttribute("aria-label", "심사 체험 안내");
  guide.innerHTML = `
    <button class="teacher-demo-guide__close" type="button" aria-label="심사 체험 안내 닫기">×</button>
    <h2 class="teacher-demo-guide__title">🔍 심사 체험 안내</h2>
    <p class="teacher-demo-guide__text">심사계정에서는 학생들의 활동 모습을 확인할 수 있도록 학생 정보와 활동 예시 데이터를 미리 구성해 두었습니다.</p>
    <p class="teacher-demo-guide__text">실제 계정에서는 학생들의 실제 활동 기록을 바탕으로 학습 현황과 관리 정보가 자동으로 연계됩니다.</p>
  `;

  guide.querySelector(".teacher-demo-guide__close").addEventListener("click", function () {
    saveDemoState(stateKey, true);
    if (loadDemoState(stateKey, false) === true) guide.remove();
  });

  document.body.appendChild(guide);
}
