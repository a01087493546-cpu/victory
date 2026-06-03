var sourceData = [
  {
    type: "UI 디자인",
    fileName: "index.html, common.css",
    name: "첫 화면 로그인 UI",
    description: "문답책 첫 화면의 로그인 카드, 역할 선택 카드, 배경 레이아웃",
    source: "직접 제작",
    usedIn: "로그인 첫 화면",
    modified: "해당 없음",
    note: "개발: HTML/CSS/JavaScript로 직접 제작"
  },
  {
    type: "이미지",
    fileName: "hero-student.png",
    name: "학생 용사 캐릭터 이미지",
    description: "책과 검을 들고 모험을 시작하는 학생 용사 캐릭터",
    source: "OpenAI ChatGPT 이미지 생성",
    usedIn: "로그인 화면 왼쪽 모험 장면, 학생 화면 예정",
    modified: "파일명 변경, 배경 제거형 이미지로 사용, CSS로 크기 조정",
    note: "확보: ChatGPT 생성 이미지 / OpenAI 이용약관 증빙자료 확보"
  },
  {
    type: "이미지",
    fileName: "dragon-main.png",
    name: "메인 드래곤 캐릭터 이미지",
    description: "지식창고를 빼앗은 메인 드래곤 캐릭터",
    source: "OpenAI ChatGPT 이미지 생성",
    usedIn: "로그인 화면, 스토리 화면, 던전 화면 예정",
    modified: "파일명 변경, 배경 제거형 이미지로 사용, CSS로 크기 조정",
    note: "확보: ChatGPT 생성 이미지 / OpenAI 이용약관 증빙자료 확보"
  },
  {
    type: "이미지",
    fileName: "role-student.png",
    name: "학생 역할 선택 캐릭터 이미지",
    description: "학생 역할 선택 카드에 들어가는 학생 용사 이미지",
    source: "OpenAI ChatGPT 이미지 생성",
    usedIn: "로그인 화면 학생 역할 선택 카드",
    modified: "파일명 변경, 배경 제거형 이미지로 사용, 카드 크기에 맞게 CSS 조정",
    note: "확보: ChatGPT 생성 이미지 / OpenAI 이용약관 증빙자료 확보"
  },
  {
    type: "이미지",
    fileName: "role-teacher.png",
    name: "교사 역할 선택 캐릭터 이미지",
    description: "교사 역할 선택 카드에 들어가는 교사 캐릭터 이미지",
    source: "OpenAI ChatGPT 이미지 생성",
    usedIn: "로그인 화면 교사 역할 선택 카드",
    modified: "파일명 변경, 배경 제거형 이미지로 사용, 카드 크기에 맞게 CSS 조정",
    note: "확보: ChatGPT 생성 이미지 / OpenAI 이용약관 증빙자료 확보"
  },
  {
    type: "이미지",
    fileName: "logo-emblem.png",
    name: "문답책 로고 엠블럼 이미지",
    description: "책, 용, 질문표를 활용한 문답책 로고 엠블럼",
    source: "OpenAI ChatGPT 이미지 생성",
    usedIn: "로그인 화면 왼쪽 상단 로고 영역",
    modified: "파일명 변경, 배경 제거형 이미지로 사용, CSS로 크기 조정",
    note: "확보: ChatGPT 생성 이미지 / OpenAI 이용약관 증빙자료 확보"
  },
  {
    type: "폰트",
    fileName: "Google Fonts 웹폰트",
    name: "Noto Serif KR",
    description: "문답책 제목에 사용하는 한글 세리프 폰트",
    source: "Google Fonts / Noto Fonts",
    usedIn: "문답책 제목",
    modified: "수정 없음",
    note: "확보: Google Fonts FAQ / SIL Open Font License 1.1 증빙자료 확보"
  },
  {
    type: "폰트",
    fileName: "Google Fonts 웹폰트",
    name: "Noto Sans KR",
    description: "문제, 설명, 본문에 사용하는 기본 한글 폰트",
    source: "Google Fonts / Noto Fonts",
    usedIn: "문제, 설명, 본문 등 기본 텍스트",
    modified: "수정 없음",
    note: "확보: Google Fonts FAQ / SIL Open Font License 1.1 증빙자료 확보"
  },
  {
    type: "폰트",
    fileName: "pretendard-dynamic-subset.css",
    name: "Pretendard",
    description: "버튼, 카드 제목, 메뉴 등 UI 요소에 사용하는 폰트",
    source: "Pretendard 공식 GitHub / jsDelivr CDN",
    usedIn: "버튼, 카드 제목, 메뉴 등 UI 요소",
    modified: "수정 없음",
    note: "확보: Pretendard GitHub LICENSE / SIL Open Font License 1.1 증빙자료 확보"
  },
  {
    type: "이미지",
    fileName: "bg-story-01~06",
    name: "스토리 인트로 배경 이미지 6종",
    description: "용의 지식 욕심, 지식창고 습격, 텅 빈 도서관, 문답책, 질문의 힘, 훈련 시작 장면을 표현한 배경 이미지",
    source: "OpenAI ChatGPT 이미지 생성",
    usedIn: "스토리 인트로 화면",
    modified: "파일명 변경, 화면 비율에 맞게 CSS로 크기와 위치 조정",
    note: "확보: ChatGPT 생성 이미지 / OpenAI 이용약관 증빙자료 확보"
  },{
  title: "마법력 아이콘",
  category: "이미지",
  file: "ability-magic.png",
  description: "독서 활동에서 마법력 능력치를 나타내는 별빛 마법 아이콘 이미지",
  source: "OpenAI ChatGPT 이미지 생성",
  usage: "능력치 안내 화면",
  modified: "파일명 변경 및 화면 크기에 맞게 CSS로 조정",
  note: "ChatGPT 생성 이미지 / OpenAI 이용약관 증빙자료 확보"
},
{
  title: "체력 아이콘",
  category: "이미지",
  file: "ability-health.png",
  description: "독서 활동에서 체력 능력치를 나타내는 하트 아이콘 이미지",
  source: "OpenAI ChatGPT 이미지 생성",
  usage: "능력치 안내 화면",
  modified: "파일명 변경 및 화면 크기에 맞게 CSS로 조정",
  note: "ChatGPT 생성 이미지 / OpenAI 이용약관 증빙자료 확보"
},
{
  title: "지혜 아이콘",
  category: "이미지",
  file: "ability-wisdom.png",
  description: "독서 활동에서 지혜 능력치를 나타내는 책 아이콘 이미지",
  source: "OpenAI ChatGPT 이미지 생성",
  usage: "능력치 안내 화면",
  modified: "파일명 변경 및 화면 크기에 맞게 CSS로 조정",
  note: "ChatGPT 생성 이미지 / OpenAI 이용약관 증빙자료 확보"
},
{
  title: "용기 아이콘",
  category: "이미지",
  file: "ability-courage.png",
  description: "독서 활동에서 용기 능력치를 나타내는 방패 아이콘 이미지",
  source: "OpenAI ChatGPT 이미지 생성",
  usage: "능력치 안내 화면",
  modified: "파일명 변경 및 화면 크기에 맞게 CSS로 조정",
  note: "ChatGPT 생성 이미지 / OpenAI 이용약관 증빙자료 확보"

  },{
  title: "문답책 로고 이미지",
  category: "이미지",
  file: "logo-book.png",
  description: "문답책 상징으로 사용한 책 모양 로고 이미지",
  source: "OpenAI ChatGPT 이미지 생성",
  usage: "로그인 화면, 학생 홈 화면 상단 로고",
  modified: "파일명 변경 및 화면 크기에 맞게 CSS로 조정",
  note: "ChatGPT 생성 이미지 / OpenAI 이용약관 증빙자료 확보"
},
{
  title: "학생 홈 도서관 배경 이미지",
  category: "이미지",
  file: "home-library-bg.png",
  description: "책 속 모험 분위기를 나타내는 판타지 도서관 배경 이미지",
  source: "OpenAI ChatGPT 이미지 생성",
  usage: "학생 홈 화면 배경",
  modified: "홈 화면 분위기에 맞게 CSS로 어둡기와 배치 조정",
  note: "ChatGPT 생성 이미지 / OpenAI 이용약관 증빙자료 확보"
},
{
  title: "홈 화면 용사 루미 이미지",
  category: "이미지",
  file: "home-lumi.png",
  description: "학생을 독서 활동으로 안내하는 어린 용사 캐릭터 이미지",
  source: "OpenAI ChatGPT 이미지 생성",
  usage: "학생 홈 화면 안내 캐릭터",
  modified: "파일명 변경 및 홈 화면 배치에 맞게 크기 조정",
  note: "ChatGPT 생성 이미지 / OpenAI 이용약관 증빙자료 확보"
},
{
  title: "연습하기 카드 아이콘",
  category: "이미지",
  file: "home-practice-icon.png",
  description: "연습하기 활동을 나타내는 빛나는 책 아이콘 이미지",
  source: "OpenAI ChatGPT 이미지 생성",
  usage: "학생 홈 화면 연습하기 카드",
  modified: "카드 아이콘 영역에 맞게 CSS로 크기 조정",
  note: "ChatGPT 생성 이미지 / OpenAI 이용약관 증빙자료 확보"
},
{
  title: "개별읽기 카드 아이콘",
  category: "이미지",
  file: "home-individual-icon.png",
  description: "개별읽기 활동을 나타내는 책 더미와 별빛 아이콘 이미지",
  source: "OpenAI ChatGPT 이미지 생성",
  usage: "학생 홈 화면 개별읽기 카드",
  modified: "카드 아이콘 영역에 맞게 CSS로 크기 조정",
  note: "ChatGPT 생성 이미지 / OpenAI 이용약관 증빙자료 확보"
},  {
    title: "연습하기 - 책 고르기 단계 이미지",
    category: "이미지",
    fileName: "practice-book-select.png",
    description: "책 고르기 활동을 나타내는 빛나는 책 더미와 열린 책 이미지",
    source: "OpenAI ChatGPT 이미지 생성",
    usage: "학생 연습하기 화면 책 고르기 카드",
    modified: "파일명 변경 및 연습하기 카드 화면에 맞게 크기 조정",
    note: "ChatGPT 생성 이미지 / OpenAI 이용약관 증빙자료 확보"
  },
  {
    title: "연습하기 - 읽기 전 단계 이미지",
    category: "이미지",
    fileName: "practice-before-reading.png",
    description: "읽기 전 생각 열기 활동을 나타내는 책 위의 빛나는 전구 이미지",
    source: "OpenAI ChatGPT 이미지 생성",
    usage: "학생 연습하기 화면 읽기 전 카드",
    modified: "파일명 변경 및 연습하기 카드 화면에 맞게 크기 조정",
    note: "ChatGPT 생성 이미지 / OpenAI 이용약관 증빙자료 확보"
  },
  {
    title: "연습하기 - 읽기 중 단계 이미지",
    category: "이미지",
    fileName: "practice-during-reading.png",
    description: "읽기 중 질문하며 깊이 읽기 활동을 나타내는 열린 책, 돋보기, 물음표 이미지",
    source: "OpenAI ChatGPT 이미지 생성",
    usage: "학생 연습하기 화면 읽기 중 카드",
    modified: "파일명 변경 및 연습하기 카드 화면에 맞게 크기 조정",
    note: "ChatGPT 생성 이미지 / OpenAI 이용약관 증빙자료 확보"
  },
  {
    title: "연습하기 - 우리반 책읽기 단계 이미지",
    category: "이미지",
    fileName: "practice-class-reading.png",
    description: "우리반이 함께 책을 읽는 활동을 나타내는 학생들과 빛나는 책 이미지",
    source: "OpenAI ChatGPT 이미지 생성",
    usage: "학생 연습하기 화면 우리반 책읽기 카드",
    modified: "파일명 변경 및 연습하기 카드 화면에 맞게 크기 조정",
    note: "ChatGPT 생성 이미지 / OpenAI 이용약관 증빙자료 확보"
  },
  {
    title: "연습하기 - 읽기 후 단계 이미지",
    category: "이미지",
    fileName: "practice-after-reading.png",
    description: "읽기 후 내용을 간추리는 활동을 나타내는 책, 두루마리, 깃펜 이미지",
    source: "OpenAI ChatGPT 이미지 생성",
    usage: "학생 연습하기 화면 읽기 후 카드",
    modified: "파일명 변경 및 연습하기 카드 화면에 맞게 크기 조정",
    note: "ChatGPT 생성 이미지 / OpenAI 이용약관 증빙자료 확보"
  },  {
    title: "책 투표 안내 루미 캐릭터 이미지",
    category: "이미지",
    fileName: "book-vote-lumi.png",
    description: "책 투표 화면에서 학생에게 우리 반이 읽을 책 투표를 안내하는 루미 상반신 캐릭터 이미지",
    source: "OpenAI ChatGPT 이미지 생성",
    usage: "학생 책 투표 화면 루미 안내 영역",
    modified: "파일명 변경 및 책 투표 화면에 맞게 크기 조정",
    note: "ChatGPT 생성 이미지 / OpenAI 이용약관 증빙자료 확보"
  },
{
  title: "책 투표 대기 화면 신비로운 모래시계 이미지",
  category: "이미지",
  fileName: "mystic-hourglass.png",
  description: "책 투표 후 다른 학생들의 투표 결과를 기다리는 상태를 나타내는 신비로운 금빛 모래시계 이미지",
  source: "OpenAI ChatGPT 이미지 생성",
  usage: "학생 책 투표 화면 투표 대기 상태 영역",
  modified: "파일명 변경 및 책 투표 대기 화면에 맞게 크기 조정",
  note: "ChatGPT 생성 이미지 / OpenAI 이용약관 증빙자료 확보"
},  {
    title: "질문 꾸러미 인트로 도서관 배경 이미지",
    category: "이미지",
    fileName: "question-intro-library-bg.png",
    description: "질문하며 읽기의 중요성을 안내하는 인트로 화면에 사용한 신비로운 판타지 도서관 배경 이미지",
    source: "OpenAI ChatGPT 이미지 생성",
    usage: "학생 질문 꾸러미 화면 인트로 배경",
    modified: "파일명 변경 및 인트로 화면 비율에 맞게 CSS로 크기와 위치 조정",
    note: "ChatGPT 생성 이미지 / OpenAI 이용약관 증빙자료 확보"
  },
  {
    title: "질문 꾸러미 인트로 루미 장면 1 이미지",
    category: "이미지",
    fileName: "question-intro-lumi-scene1.png",
    description: "책을 읽으며 궁금함이 생기는 순간을 안내하는 루미 캐릭터 이미지",
    source: "OpenAI ChatGPT 이미지 생성",
    usage: "학생 질문 꾸러미 화면 인트로 1장면",
    modified: "파일명 변경, 배경 제거형 이미지로 사용, CSS 및 JavaScript로 화면 배치 조정",
    note: "ChatGPT 생성 이미지 / OpenAI 이용약관 증빙자료 확보"
  },
  {
    title: "질문 꾸러미 인트로 루미 장면 2 이미지",
    category: "이미지",
    fileName: "question-intro-lumi-scene2.png",
    description: "작은 궁금함이 글을 깊이 읽는 힘이 된다는 내용을 안내하는 루미 캐릭터 이미지",
    source: "OpenAI ChatGPT 이미지 생성",
    usage: "학생 질문 꾸러미 화면 인트로 2장면",
    modified: "파일명 변경, 배경 제거형 이미지로 사용, CSS 및 JavaScript로 화면 배치 조정",
    note: "ChatGPT 생성 이미지 / OpenAI 이용약관 증빙자료 확보"
  },
  {
    title: "질문 꾸러미 인트로 루미 장면 3 이미지",
    category: "이미지",
    fileName: "question-intro-lumi-scene3.png",
    description: "질문의 힘을 깨워 책을 깊이 읽어보자는 메시지를 전달하는 루미 캐릭터 이미지",
    source: "OpenAI ChatGPT 이미지 생성",
    usage: "학생 질문 꾸러미 화면 인트로 3장면",
    modified: "파일명 변경, 배경 제거형 이미지로 사용, CSS 및 JavaScript로 화면 배치 조정",
    note: "ChatGPT 생성 이미지 / OpenAI 이용약관 증빙자료 확보"
  },
  {
    title: "질문 꾸러미 1단계 루미 안내 이미지",
    category: "이미지",
    fileName: "question-bundle-lumi-step1.png",
    description: "이야기를 먼저 그냥 읽어보는 활동을 안내하는 루미 캐릭터 이미지",
    source: "OpenAI ChatGPT 이미지 생성",
    usage: "학생 질문 꾸러미 화면 1단계 그냥 읽어보기 안내 영역",
    modified: "파일명 변경 및 질문 꾸러미 카드 영역에 맞게 CSS로 크기 조정",
    note: "ChatGPT 생성 이미지 / OpenAI 이용약관 증빙자료 확보"
  },
  {
    title: "질문 꾸러미 2단계 루미 안내 이미지",
    category: "이미지",
    fileName: "question-bundle-lumi-step2.png",
    description: "질문과 함께 이야기를 다시 읽어보는 활동을 안내하는 루미 캐릭터 이미지",
    source: "OpenAI ChatGPT 이미지 생성",
    usage: "학생 질문 꾸러미 화면 2단계 질문과 함께 읽기 안내 영역",
    modified: "파일명 변경 및 질문 꾸러미 카드 영역에 맞게 CSS로 크기 조정",
    note: "ChatGPT 생성 이미지 / OpenAI 이용약관 증빙자료 확보"
  },
  {
    title: "질문 꾸러미 3단계 루미 안내 이미지",
    category: "이미지",
    fileName: "question-bundle-lumi-step3.png",
    description: "학생이 직접 궁금한 질문을 만들어보는 활동을 안내하는 루미 캐릭터 이미지",
    source: "OpenAI ChatGPT 이미지 생성",
    usage: "학생 질문 꾸러미 화면 3단계 내가 궁금한 질문 만들기 안내 영역",
    modified: "파일명 변경 및 질문 꾸러미 카드 영역에 맞게 CSS로 크기 조정",
    note: "ChatGPT 생성 이미지 / OpenAI 이용약관 증빙자료 확보"
  },
  {
    title: "질문 꾸러미 4단계 루미 안내 이미지",
    category: "이미지",
    fileName: "question-bundle-lumi-step4.png",
    description: "친구들의 생각을 살펴보고 내 생각을 나누는 활동을 안내하는 루미 캐릭터 이미지",
    source: "OpenAI ChatGPT 이미지 생성",
    usage: "학생 질문 꾸러미 화면 4단계 생각 나누기 안내 영역",
    modified: "파일명 변경 및 질문 꾸러미 카드 영역에 맞게 CSS로 크기 조정",
    note: "ChatGPT 생성 이미지 / OpenAI 이용약관 증빙자료 확보"
  },
  {
    title: "질문 꾸러미 5단계 루미 안내 이미지",
    category: "이미지",
    fileName: "question-bundle-lumi-step5.png",
    description: "질문하며 읽기의 중요성을 정리하는 활동을 안내하는 루미 캐릭터 이미지",
    source: "OpenAI ChatGPT 이미지 생성",
    usage: "학생 질문 꾸러미 화면 5단계 질문하며 읽기의 중요성 알기 안내 영역",
    modified: "파일명 변경 및 질문 꾸러미 카드 영역에 맞게 CSS로 크기 조정",
    note: "ChatGPT 생성 이미지 / OpenAI 이용약관 증빙자료 확보"
  },
];