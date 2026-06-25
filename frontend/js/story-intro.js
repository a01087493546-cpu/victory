/*
  파일명: story-intro.js
  역할: 첫 로그인 스토리 인트로 화면의 넘기기 기능을 담당합니다.

  이 파일에서 하는 일:
  1. 스토리 6장을 배열로 관리합니다.
  2. 다음 버튼을 누르면 다음 이야기로 넘어갑니다.
  3. 이전 버튼을 누르면 이전 이야기로 돌아갑니다.
  4. 마지막 장에서 '모험 시작하기'를 누르면 학생 홈으로 이동합니다.
  5. 스토리를 다 본 기록을 studentId별로 sessionStorage에 저장합니다.
  6. 장면마다 배경 이미지와 루미 캐릭터 이미지를 바꿔 보여줍니다.
*/

// 로그인한 사용자만 스토리 인트로 화면에 접근할 수 있습니다.
// auth.js가 먼저 불러와졌을 때만 checkLogin을 실행합니다.
if (typeof checkLogin === "function") {
  checkLogin();
}

// 스토리 장면 목록입니다.
// 실제 assets/images/story 폴더에 있는 파일명과 정확히 맞춰야 이미지가 보입니다.
// 스토리 장면 목록입니다.
// story-intro.html 파일은 student 폴더 안에 있기 때문에,
// 이미지 경로는 ../assets/story/ 로 한 단계 올라가서 찾습니다.
// 현재 Live Server 주소 구조에 맞춰 이미지 기본 경로를 자동으로 정합니다.
// 주소에 /frontend/가 들어가 있으면 /frontend/assets 경로를 사용합니다.
// 주소에 /frontend/가 없으면 /assets 경로를 사용합니다.
// 현재 Live Server 주소 구조에 맞춰 이미지 기본 경로를 자동으로 정합니다.
// 실제 이미지 폴더가 frontend/assets/story/에 있으므로 images는 넣지 않습니다.
const storyImagePath = window.location.hostname === "hwani-dot.github.io"
  ? "/victory/frontend/assets/story/"
  : "../assets/story/";

// 스토리 장면 목록입니다.
// 이미지 파일명은 frontend/assets/images/story 폴더 안의 실제 파일명과 맞춰야 합니다.
const storySlides = [
  {
    speaker: "루미",
    text: "아무리 배워도 만족하지 못하는 용이 있었어.\n그 용은 세상의 지식을 모두 혼자 갖고 싶어 했지.",
    // 첫 번째 스토리 배경 이미지입니다.
// 실제 파일명이 bg-story-01로 시작하므로 01을 사용합니다.
    bg: storyImagePath + "bg-story-01-dragon-study.png",
    character: storyImagePath + "char-lumi-01-basic.png"
  },
  {
    speaker: "루미",
    text: "결국 용은 세계 지식창고를 습격했어.\n그리고 수많은 지식을 훔쳐 어디론가 숨겨 버렸지.",
    // 2장 배경 이미지입니다.
    bg: storyImagePath + "bg-story-02-dragon-raid.png",
    character: storyImagePath + "char-lumi-02-surprised.png"
  },
  {
    speaker: "루미",
    text: "다음 날, 사람들은 텅 빈 지식창고를 발견했어.\n왕국은 잃어버린 지식을 되찾을 방법을 찾아야 했어.",
    // 3장 배경 이미지입니다.
    bg: storyImagePath + "bg-story-03-empty-library.png",
    character: storyImagePath + "char-lumi-03-serious.png"
  },
  {
    speaker: "루미",
    text: "왕국은 국가의 보물인 문답책을 찾아갔어.\n문답책은 깊고 정확한 질문에만 답해주는 신비한 책이었지.",
    // 4장 배경 이미지입니다.
    bg: storyImagePath + "bg-story-04-mundapbook.png",
    character: storyImagePath + "char-lumi-04-mysterious.png"
  },
  {
    speaker: "루미",
    text: "문답책은 좋은 질문이 용을 약하게 만든다고 말했어.\n깊이 있는 질문을 받을수록 용은 힘을 잃게 되는 거야.",
    // 5장 배경 이미지입니다.
    bg: storyImagePath + "bg-story-05-question-power.png",
    character: storyImagePath + "char-lumi-05-confident.png"
  },
  {
    speaker: "루미",
    text: "그래서 용사들은 책을 읽고 질문을 만드는 훈련을 시작했어.\n이제 세상의 지식을 되찾는 모험이 시작되는 거야.",
    // 6장 배경 이미지입니다.
    bg: storyImagePath + "bg-story-06-training-start.png",
    character: storyImagePath + "char-lumi-06-smile.png"
  }
];

// 현재 보고 있는 스토리 번호입니다.
// 배열은 0번부터 시작하므로 첫 장면은 0입니다.
let currentSlideIndex = 0;

// HTML 요소를 담아둘 변수입니다.
// DOMContentLoaded 이후 실제 요소를 연결합니다.
let storyPage;
let storyCharacter;
let storyTitle;
let storyScene;
let storySpeaker;
let storyText;
let storyCount;
let storyDots;
let prevStoryButton;
let nextStoryButton;

/*
  페이지의 HTML이 모두 준비된 뒤에 실행됩니다.
  이 안에서 버튼과 화면 요소를 안전하게 연결합니다.
*/
document.addEventListener("DOMContentLoaded", function () {
  // HTML의 스토리 전체 화면 영역을 가져옵니다.
  // 이 영역에 장면별 배경 이미지를 적용합니다.
  storyPage = document.getElementById("storyPage");

  // HTML 요소들을 가져옵니다.
  storyCharacter = document.getElementById("storyCharacter");
  storyTitle = document.getElementById("storyTitle");
  storyScene = document.getElementById("storyScene");
  storySpeaker = document.getElementById("storySpeaker");
  storyText = document.getElementById("storyText");
  storyCount = document.getElementById("storyCount");
  storyDots = document.getElementById("storyDots");

  // 이전/다음 버튼을 가져옵니다.
  prevStoryButton = document.getElementById("prevStoryButton");
  nextStoryButton = document.getElementById("nextStoryButton");

  // 첫 번째 스토리를 화면에 보여줍니다.
  renderStorySlide();
});

/*
  함수명: renderStorySlide
  역할: 현재 번호에 맞는 스토리 내용을 화면에 보여줍니다.
*/
function renderStorySlide() {
  const slide = storySlides[currentSlideIndex];
    // 이미지 경로가 실제로 어떻게 잡히는지 확인하기 위해 콘솔에 출력합니다.
  console.log("현재 배경 이미지 경로:", slide.bg);
  console.log("현재 루미 이미지 경로:", slide.character);

  // 현재 장면에 맞는 배경 이미지를 적용합니다.
  // storySlides 배열에 저장된 bg 경로를 화면 배경으로 사용합니다.
  storyPage.style.backgroundImage = `url("${slide.bg}")`;

  // 루미 캐릭터 이미지를 보여줍니다.
  storyCharacter.innerHTML = `
    <img src="${slide.character}" alt="${slide.speaker}" class="story-character-img">
  `;

  // 제목과 장면 설명은 화면에 표시하지 않습니다.
  // 루미의 대사만 자연스럽게 보여주기 위해 비워둡니다.
  storyTitle.textContent = "";
  storyScene.textContent = "";

  // 화자와 본문을 현재 장면에 맞게 넣습니다.
  storySpeaker.textContent = slide.speaker;
  storyText.textContent = slide.text;

  // 현재 몇 번째 장면인지 표시합니다.
  storyCount.textContent = `${currentSlideIndex + 1} / ${storySlides.length}`;

  // 첫 번째 장면에서는 이전 버튼을 비활성화합니다.
  prevStoryButton.disabled = currentSlideIndex === 0;

  // 마지막 장면에서는 다음 버튼 문구를 바꿉니다.
  if (currentSlideIndex === storySlides.length - 1) {
    nextStoryButton.textContent = "모험 시작하기";
  } else {
    nextStoryButton.textContent = "다음";
  }

  // 진행 점을 다시 그립니다.
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
  함수명: goPrevStory
  역할: 이전 버튼을 눌렀을 때 이전 스토리로 이동합니다.
*/
function goPrevStory() {
  if (currentSlideIndex > 0) {
    currentSlideIndex = currentSlideIndex - 1;
    renderStorySlide();
  }
}

/*
  함수명: goNextStory
  역할: 다음 버튼을 눌렀을 때 다음 스토리로 이동합니다.
*/
function goNextStory() {
  const lastIndex = storySlides.length - 1;

  if (currentSlideIndex === lastIndex) {
    finishStoryIntro();
    return;
  }

  currentSlideIndex = currentSlideIndex + 1;
  renderStorySlide();
}

/*
  함수명: skipStory
  역할: 건너뛰기 버튼을 눌렀을 때 바로 스토리를 끝냅니다.
*/
function skipStory() {
  const confirmSkip = confirm("이야기를 건너뛰고 바로 모험을 시작할까요?");

  if (confirmSkip) {
    finishStoryIntro();
  }
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

  // 스토리 인트로가 끝나면 바로 학생 홈으로 가지 않고,
// 먼저 능력치 안내 화면으로 이동합니다.
window.location.href = "./ability-intro.html";
}