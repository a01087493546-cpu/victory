/*
  파일명: story-intro.js
  역할: 첫 로그인 스토리 인트로 화면의 넘기기 기능을 담당합니다.

  이 파일에서 하는 일:
  1. 스토리 6장을 배열로 관리합니다.
  2. 다음 버튼을 누르면 다음 이야기로 넘어갑니다.
  3. 이전 버튼을 누르면 이전 이야기로 돌아갑니다.
  4. 마지막 장에서 '모험 시작하기'를 누르면 학생 홈으로 이동합니다.
  5. 스토리를 다 본 기록을 studentId별로 sessionStorage에 저장합니다.

  저작권/출처 원칙:
  - 현재는 외부 이미지 파일을 사용하지 않습니다.
  - 임시 이모지와 텍스트만 사용합니다.
  - 나중에 AI 생성 이미지를 넣을 때는 source-data.js에 출처를 기록합니다.
*/

const storySlides = [
  {
    character: "🐉📚",
    title: "욕심 많은 용",
    scene: "세상의 모든 지식을 탐내는 용이 있었어요.",
    speaker: "이야기꾼",
    text: "아무리 공부해도 만족하지 못한 용은 세상의 지식을 모두 혼자 차지하고 싶어 했어요."
  },
  {
    character: "🐉🏰",
    title: "지식창고 습격",
    scene: "용은 세계 지식창고를 몰래 찾아갔어요.",
    speaker: "이야기꾼",
    text: "어느 날 밤, 용은 세계의 지식이 모여 있는 지식창고를 습격하고 수많은 지식을 자기 보물창고에 숨겨 버렸어요."
  },
  {
    character: "🏰💨",
    title: "텅 빈 지식창고",
    scene: "다음 날, 사람들은 텅 빈 창고를 발견했어요.",
    speaker: "왕국 사람들",
    text: "지식창고가 텅 비자 왕국 사람들은 크게 놀랐어요. 모두가 지식을 되찾을 방법을 찾기 시작했어요."
  },
  {
    character: "📖✨",
    title: "문답책의 등장",
    scene: "왕국은 신비한 책, 문답책에게 도움을 청했어요.",
    speaker: "문답책",
    text: "나는 아무 질문에나 답하지 않아. 깊이 생각해서 만든 좋은 질문에만 길을 알려줄 수 있단다."
  },
  {
    character: "🧒⚔️",
    title: "질문의 힘",
    scene: "좋은 질문은 용을 약하게 만드는 힘이었어요.",
    speaker: "문답책",
    text: "용은 스스로 깊게 생각하는 힘이 약하단다. 정확하고 깊이 있는 질문을 받으면 점점 힘을 잃게 될 거야."
  },
  {
    character: "🧒📖🔥",
    title: "용사의 훈련 시작",
    scene: "이제 책을 읽고 질문을 만들 시간이에요.",
    speaker: "문답책",
    text: "용사들이여, 책을 읽고 질문하는 힘을 길러 지식창고를 되찾으러 떠나 볼까요?"
  }
];

// 현재 보고 있는 스토리 번호입니다. 0부터 시작합니다.
let currentSlideIndex = 0;

// HTML 요소들을 가져옵니다.
const storyCharacter = document.getElementById("storyCharacter");
const storyTitle = document.getElementById("storyTitle");
const storyScene = document.getElementById("storyScene");
const storySpeaker = document.getElementById("storySpeaker");
const storyText = document.getElementById("storyText");
const storyCount = document.getElementById("storyCount");
const storyDots = document.getElementById("storyDots");

const prevStoryButton = document.getElementById("prevStoryButton");
const nextStoryButton = document.getElementById("nextStoryButton");
const skipStoryButton = document.getElementById("skipStoryButton");

/*
  페이지가 열리자마자 첫 번째 스토리를 화면에 보여줍니다.
*/
renderStorySlide();

/*
  이전 버튼을 눌렀을 때 실행됩니다.
*/
prevStoryButton.addEventListener("click", function () {
  if (currentSlideIndex > 0) {
    currentSlideIndex = currentSlideIndex - 1;
    renderStorySlide();
  }
});

/*
  다음 버튼을 눌렀을 때 실행됩니다.
*/
nextStoryButton.addEventListener("click", function () {
  const lastIndex = storySlides.length - 1;

  if (currentSlideIndex === lastIndex) {
    finishStoryIntro();
    return;
  }

  currentSlideIndex = currentSlideIndex + 1;
  renderStorySlide();
});

/*
  건너뛰기 버튼을 눌렀을 때 실행됩니다.
*/
skipStoryButton.addEventListener("click", function () {
  const confirmSkip = confirm("이야기를 건너뛰고 바로 모험을 시작할까요?");

  if (confirmSkip) {
    finishStoryIntro();
  }
});

/*
  함수명: renderStorySlide
  역할: 현재 번호에 맞는 스토리 내용을 화면에 보여줍니다.
*/
function renderStorySlide() {
  const slide = storySlides[currentSlideIndex];

  storyCharacter.textContent = slide.character;
  storyTitle.textContent = slide.title;
  storyScene.textContent = slide.scene;
  storySpeaker.textContent = slide.speaker;
  storyText.textContent = slide.text;

  storyCount.textContent = `${currentSlideIndex + 1} / ${storySlides.length}`;

  prevStoryButton.disabled = currentSlideIndex === 0;

  if (currentSlideIndex === storySlides.length - 1) {
    nextStoryButton.textContent = "모험 시작하기";
  } else {
    nextStoryButton.textContent = "다음";
  }

  renderStoryDots();
}

/*
  함수명: renderStoryDots
  역할: 현재 스토리 진행 상태를 점으로 보여줍니다.
*/
function renderStoryDots() {
  let dotHTML = "";

  for (let i = 0; i < storySlides.length; i++) {
    if (i === currentSlideIndex) {
      dotHTML = dotHTML + `<span class="story-dot active"></span>`;
    } else {
      dotHTML = dotHTML + `<span class="story-dot"></span>`;
    }
  }

  storyDots.innerHTML = dotHTML;
}

/*
  함수명: finishStoryIntro
  역할: 스토리를 본 것으로 저장하고 학생 홈으로 이동합니다.
*/
function finishStoryIntro() {
  // studentId별로 스토리 인트로를 본 기록을 저장합니다.
  // 여러 학생이 같은 브라우저를 써도 각자 따로 관리됩니다.
  const studentId = sessionStorage.getItem("studentId") || "1";
  sessionStorage.setItem("hasSeenStoryIntro_" + studentId, "true");

  window.location.href = "./home.html";
}