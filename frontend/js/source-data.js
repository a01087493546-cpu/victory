/*
  파일명: source-data.js
  역할: 문답책 프로젝트에 사용된 자료의 출처 정보를 관리하는 파일입니다.

  대회 규정 대응:
  - 폰트, 이미지, 아이콘, 효과음 등 사용 자료의 출처를 기록합니다.
  - 사용 자료 출처는 첫 화면의 i 버튼에서 확인할 수 있게 합니다.
  - 무료 사용 가능하고 출처/라이선스를 명확히 밝힐 수 있는 자료만 사용합니다.
*/

var sourceData = [
  {
    type: "UI 디자인",
    name: "첫 화면 로그인 UI",
    source: "직접 제작",
    tool: "HTML, CSS, JavaScript",
    license: "직접 제작 자료",
    usedIn: "index.html 첫 화면",
    modified: "해당 없음",
    note: "외부 이미지 파일 없이 CSS 그라데이션, 카드, 버튼 스타일로 직접 제작"
  },
  {
    type: "폰트",
    name: "Pretendard",
    source: "Pretendard 공식 GitHub",
    tool: "시스템/웹 폰트",
    license: "SIL Open Font License 1.1",
    usedIn: "전체 화면 기본 폰트 후보",
    modified: "수정 없음",
    note: "무료 오픈소스 한글 폰트. 실제 적용 방식은 추후 최종 출처 목록에 정리"
  },
  {
    type: "폰트",
    name: "Noto Sans KR",
    source: "Google Fonts / Noto Fonts",
    tool: "시스템/웹 폰트",
    license: "SIL Open Font License 1.1",
    usedIn: "전체 화면 대체 폰트 후보",
    modified: "수정 없음",
    note: "무료 오픈소스 한글 폰트. 실제 적용 방식은 추후 최종 출처 목록에 정리"
  },
  {
    type: "이미지",
    name: "학생 용사 캐릭터 이미지",
    source: "OpenAI ChatGPT 이미지 생성",
    tool: "ChatGPT 이미지 생성 기능",
    license: "생성형 AI 출력물 사용. 대회 제출 시 생성 도구와 사용 위치를 명시하여 사용",
    usedIn: "로그인 화면 왼쪽 모험 장면, 학생 홈 화면 예정",
    modified: "파일명 변경(hero-student.png), 배경 제거형 이미지로 사용, 화면 크기에 맞게 CSS로 크기 조정",
    note: "특정 게임·애니·기존 캐릭터를 참고하지 않고 문답책 프로젝트용 독자 학생 용사 캐릭터로 생성"
  },
  {
    type: "이미지",
    name: "메인 드래곤 캐릭터 이미지",
    source: "OpenAI ChatGPT 이미지 생성",
    tool: "ChatGPT 이미지 생성 기능",
    license: "생성형 AI 출력물 사용. 대회 제출 시 생성 도구와 사용 위치를 명시하여 사용",
    usedIn: "로그인 화면, 학생 홈 화면, 스토리 화면, 던전 화면 예정",
    modified: "파일명 변경(dragon-main.png), 배경 제거형 이미지로 사용, 화면 크기에 맞게 CSS로 크기 조정",
    note: "특정 게임·애니·기존 캐릭터를 참고하지 않고 문답책 프로젝트용 독자 드래곤 캐릭터로 생성"
  },
  {
    type: "이미지",
    name: "교사 역할 선택 캐릭터 이미지",
    source: "OpenAI ChatGPT 이미지 생성",
    tool: "ChatGPT 이미지 생성 기능",
    license: "생성형 AI 출력물 사용. 대회 제출 시 생성 도구와 사용 위치를 명시하여 사용",
    usedIn: "로그인 화면 교사 역할 선택 카드",
    modified: "파일명 변경(role-teacher.png), 배경 제거형 이미지로 사용, 역할 선택 카드 크기에 맞게 CSS로 크기 조정",
    note: "특정 게임·애니·기존 캐릭터를 참고하지 않고 문답책 프로젝트용 독자 교사 캐릭터로 생성"
  },
  {
    type: "이미지",
    name: "학생 역할 선택 캐릭터 이미지",
    source: "OpenAI ChatGPT 이미지 생성",
    tool: "ChatGPT 이미지 생성 기능",
    license: "생성형 AI 출력물 사용. 대회 제출 시 생성 도구와 사용 위치를 명시하여 사용",
    usedIn: "로그인 화면 학생 역할 선택 카드",
    modified: "파일명 변경(role-student.png), 배경 제거형 이미지로 사용, 역할 선택 카드 크기에 맞게 CSS로 크기 조정",
    note: "특정 게임·애니·기존 캐릭터를 참고하지 않고 문답책 프로젝트용 독자 학생 용사 캐릭터로 생성"
  },  {
    type: "이미지",
    name: "문답책 로고 엠블럼 이미지",
    source: "OpenAI ChatGPT 이미지 생성",
    tool: "ChatGPT 이미지 생성 기능",
    license: "생성형 AI 출력물 사용. 대회 제출 시 생성 도구와 사용 위치를 명시하여 사용",
    usedIn: "로그인 화면 왼쪽 상단 로고 영역",
    modified: "파일명 변경(logo-emblem.png), 글자 없는 엠블럼 형태로 생성, 배경 제거형 이미지로 사용, 화면 크기에 맞게 CSS로 크기 조정",
    note: "문답책 프로젝트용으로 생성한 독자 로고 엠블럼. 텍스트 로고는 HTML/CSS로 별도 표시하여 폰트 저작권 이슈를 줄임"
  }
];