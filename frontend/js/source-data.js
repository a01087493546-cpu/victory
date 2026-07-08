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
    title: "연습하기 - 읽기 후 단계 이미지",
    category: "이미지",
    fileName: "practice-after-reading.png",
    description: "읽기 후 내용을 간추리는 활동을 나타내는 책, 두루마리, 깃펜 이미지",
    source: "OpenAI ChatGPT 이미지 생성",
    usage: "학생 연습하기 화면 읽기 후 카드",
    modified: "파일명 변경 및 연습하기 카드 화면에 맞게 크기 조정",
    note: "ChatGPT 생성 이미지 / OpenAI 이용약관 증빙자료 확보"
  },    
  {
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
    {
    title: "읽기 전 인트로 배경 이미지",
    category: "이미지",
    fileName: "pre-reading-bg-magic-greenhouse.png",
    description: "읽기 전 활동을 시작하기 전에 보여주는 판타지 도서관 배경 이미지",
    source: "OpenAI ChatGPT 이미지 생성",
    usage: "학생 읽기 전 인트로 화면 배경",
    modified: "파일명 변경 및 인트로 화면 분위기에 맞게 CSS로 밝기와 배치 조정",
    note: "ChatGPT 생성 이미지 / OpenAI 이용약관 증빙자료 확보"
  },
  {
    title: "읽기 전 인트로 1단계 루미 안내 이미지",
    category: "이미지",
    fileName: "pre-reading-lumi-clue-book.png",
    description: "책을 펼치기 전 표지와 제목에 숨은 단서를 살펴보도록 안내하는 루미 캐릭터 이미지",
    source: "OpenAI ChatGPT 이미지 생성",
    usage: "학생 읽기 전 인트로 화면 1단계 루미 안내 영역",
    modified: "파일명 변경, 투명 배경 PNG로 정리, 화면 배치에 맞게 CSS로 크기 조정",
    note: "ChatGPT 생성 이미지 / OpenAI 이용약관 증빙자료 확보"
  },
  {
    title: "읽기 전 인트로 2단계 루미 안내 이미지",
    category: "이미지",
    fileName: "pre-reading-lumi-thinking.png",
    description: "책을 읽기 전에 궁금한 점을 떠올리도록 안내하는 루미 캐릭터 이미지",
    source: "OpenAI ChatGPT 이미지 생성",
    usage: "학생 읽기 전 인트로 화면 2단계 루미 안내 영역",
    modified: "파일명 변경, 투명 배경 PNG로 정리, 다른 루미 이미지와 크기가 맞도록 CSS로 크기 조정",
    note: "ChatGPT 생성 이미지 / OpenAI 이용약관 증빙자료 확보"
  },
  {
    title: "읽기 전 인트로 3단계 루미 안내 이미지",
    category: "이미지",
    fileName: "pre-reading-lumi-question-note.png",
    description: "읽기 전 질문을 만들어 보도록 안내하는 루미 캐릭터 이미지",
    source: "OpenAI ChatGPT 이미지 생성",
    usage: "학생 읽기 전 인트로 화면 3단계 루미 안내 영역",
    modified: "파일명 변경, 투명 배경 PNG로 정리, 화면 배치에 맞게 CSS로 크기 조정",
    note: "ChatGPT 생성 이미지 / OpenAI 이용약관 증빙자료 확보"
  },
  ,
{
  title: "읽기 중 인트로 배경 이미지",
  category: "이미지",
  fileName: "during-reading-intro-bg.png",
  description: "읽기 중 활동 인트로 화면에 사용되는 신비로운 도서관 배경 이미지",
  source: "OpenAI ChatGPT 이미지 생성",
  usage: "학생 읽기 중 활동 인트로 1~3장면 공통 배경",
  modified: "파일명 변경 및 인트로 화면 비율에 맞게 CSS로 크기와 위치 조정 예정",
  note: "ChatGPT 생성 이미지 / OpenAI 이용약관 증빙자료 확보"
},
{
  title: "읽기 중 인트로 루미 장면 1 이미지",
  category: "이미지",
  fileName: "during-reading-lumi-01-goodjob.png",
  description: "읽기 전 활동을 끝낸 학생을 칭찬하는 루미 캐릭터 이미지",
  source: "OpenAI ChatGPT 이미지 생성",
  usage: "학생 읽기 중 활동 인트로 1장면",
  modified: "파일명 변경, 배경 제거형 이미지로 사용, CSS로 캐릭터 크기와 위치 통일 예정",
  note: "ChatGPT 생성 이미지 / OpenAI 이용약관 증빙자료 확보"
},
{
  title: "읽기 중 인트로 루미 장면 2 이미지",
  category: "이미지",
  fileName: "during-reading-lumi-02-question-note.png",
  description: "책을 읽으며 생긴 궁금함에 답해 보는 활동을 안내하는 루미 캐릭터 이미지",
  source: "OpenAI ChatGPT 이미지 생성",
  usage: "학생 읽기 중 활동 인트로 2장면",
  modified: "파일명 변경, 배경 제거형 이미지로 사용, CSS로 캐릭터 크기와 위치 통일 예정",
  note: "ChatGPT 생성 이미지 / OpenAI 이용약관 증빙자료 확보"
},
{
  title: "읽기 중 인트로 루미 장면 3 이미지",
  category: "이미지",
  fileName: "during-reading-lumi-03-practice.png",
  description: "읽기 중 질문의 종류를 하나씩 연습하고 책읽기에 적용해 보자고 안내하는 루미 캐릭터 이미지",
  source: "OpenAI ChatGPT 이미지 생성",
  usage: "학생 읽기 중 활동 인트로 3장면",
  modified: "파일명 변경, 배경 제거형 이미지로 사용, CSS로 캐릭터 크기와 위치 통일 예정",
  note: "ChatGPT 생성 이미지 / OpenAI 이용약관 증빙자료 확보"
},
{
  category: "이미지",
  name: "읽기 중 질문 연습 배경 이미지",
  file: "frontend/assets/images/during-reading/intro/during-reading-practice-bg.png",
  description: "읽기 중 질문 연습 화면의 마법 도서관 배경 이미지",
  source: "ChatGPT 생성 이미지",
  license: "OpenAI 이용약관에 따름",
  usage: "읽기 중 질문 연습 화면 배경",
  createdDate: "2026-06-04",
  modified: "크기 및 파일명 수정",
  note: "ChatGPT 생성 이미지 / OpenAI 이용약관 증빙자료 확보"
},
{
  category: "이미지",
  name: "책 내용에서 바로 답을 찾는 질문 아이콘",
  file: "frontend/assets/images/during-reading/intro/icon-direct-answer.png",
  description: "책 속 문장에서 바로 답을 찾는 질문 유형을 나타내는 아이콘",
  source: "ChatGPT 생성 이미지",
  license: "OpenAI 이용약관에 따름",
  usage: "읽기 중 질문 연습 - 질문 종류 선택 화면",
  createdDate: "2026-06-04",
  modified: "크기 및 파일명 수정",
  note: "ChatGPT 생성 이미지 / OpenAI 이용약관 증빙자료 확보"
},
{
  category: "이미지",
  name: "책 내용에서 답을 짐작하는 질문 아이콘",
  file: "frontend/assets/images/during-reading/intro/icon-infer-answer.png",
  description: "글 속 단서를 바탕으로 답을 짐작하는 질문 유형을 나타내는 아이콘",
  source: "ChatGPT 생성 이미지",
  license: "OpenAI 이용약관에 따름",
  usage: "읽기 중 질문 연습 - 질문 종류 선택 화면",
  createdDate: "2026-06-04",
  modified: "크기 및 파일명 수정",
  note: "ChatGPT 생성 이미지 / OpenAI 이용약관 증빙자료 확보"
},
{
  category: "이미지",
  name: "생각이나 느낌 질문 아이콘",
  file: "frontend/assets/images/during-reading/intro/icon-thought-feeling.png",
  description: "책을 읽고 떠오른 생각이나 느낌을 묻는 질문 유형을 나타내는 아이콘",
  source: "ChatGPT 생성 이미지",
  license: "OpenAI 이용약관에 따름",
  usage: "읽기 중 질문 연습 - 질문 종류 선택 화면",
  createdDate: "2026-06-04",
  modified: "크기 및 파일명 수정",
  note: "ChatGPT 생성 이미지 / OpenAI 이용약관 증빙자료 확보"
},
{
  category: "이미지",
  name: "삶과 관련짓는 질문 아이콘",
  file: "frontend/assets/images/during-reading/intro/icon-life-connection.png",
  description: "책 속 내용과 자신의 경험을 연결하는 질문 유형을 나타내는 아이콘",
  source: "ChatGPT 생성 이미지",
  license: "OpenAI 이용약관에 따름",
  usage: "읽기 중 질문 연습 - 질문 종류 선택 화면",
  createdDate: "2026-06-04",
  modified: "크기 및 파일명 수정",
  note: "ChatGPT 생성 이미지 / OpenAI 이용약관 증빙자료 확보"
},
{
  category: "이미지",
  name: "읽기 중 질문 연습 루미 이미지",
  file: "frontend/assets/images/during-reading/intro/during-reading-lumi-03-practice.png",
  description: "읽기 중 질문 연습에서 학생에게 안내와 피드백을 제공하는 루미 캐릭터 이미지",
  source: "ChatGPT 생성 이미지",
  license: "OpenAI 이용약관에 따름",
  usage: "읽기 중 질문 연습 - 방법 알기 화면, 기본 연습 피드백 박스",
  createdDate: "2026-06-04",
  modified: "투명 배경 처리 및 파일명 수정",
  note: "ChatGPT 생성 이미지 / OpenAI 이용약관 증빙자료 확보"
},{
title: "우리반 책읽기 배경 이미지",
category: "이미지",
fileName: "class-reading-bg.png",
description: "우리반 책읽기 메인 화면의 어두운 판타지 도서관 분위기 배경 이미지",
source: "ChatGPT 생성 이미지",
usage: "우리반 책읽기 메인 화면 배경",
modified: "크기 및 파일명 수정",
note: "ChatGPT 생성 이미지 / OpenAI 이용약관 증빙자료 확보"
},
{
title: "우리반 책읽기 루미 이미지",
category: "이미지",
fileName: "class-reading-lumi.png",
description: "우리반 책읽기 메인 화면에서 안내 역할을 하는 루미 캐릭터 이미지",
source: "ChatGPT 생성 이미지",
usage: "우리반 책읽기 메인 화면 안내 영역",
modified: "크기 및 파일명 수정",
note: "ChatGPT 생성 이미지 / OpenAI 이용약관 증빙자료 확보"
},
{
title: "질문 만들기 아이콘",
category: "이미지",
fileName: "class-reading-icon-create.png",
description: "우리반 책읽기 메인 화면의 질문 만들기 활동을 나타내는 아이콘",
source: "ChatGPT 생성 이미지",
usage: "우리반 책읽기 메인 화면 - 질문 만들기 카드",
modified: "크기 및 파일명 수정",
note: "ChatGPT 생성 이미지 / OpenAI 이용약관 증빙자료 확보"
},
{
title: "질문 나누기 아이콘",
category: "이미지",
fileName: "class-reading-icon-share.png",
description: "우리반 책읽기 메인 화면의 질문 나누기 활동을 나타내는 아이콘",
source: "ChatGPT 생성 이미지",
usage: "우리반 책읽기 메인 화면 - 질문 나누기 카드",
modified: "크기 및 파일명 수정",
note: "ChatGPT 생성 이미지 / OpenAI 이용약관 증빙자료 확보"
},
{
title: "질문 모음 보기 아이콘",
category: "이미지",
fileName: "class-reading-icon-collection.png",
description: "우리반 책읽기 메인 화면의 질문 모음 보기 활동을 나타내는 아이콘",
source: "ChatGPT 생성 이미지",
usage: "우리반 책읽기 메인 화면 - 질문 모음 보기 카드",
modified: "크기 및 파일명 수정",
note: "ChatGPT 생성 이미지 / OpenAI 이용약관 증빙자료 확보"
},
{
title: "질문 만들기 밝은 도서관 배경 이미지",
category: "이미지",
fileName: "class-reading-question-bg.png",
description: "질문 만들기, 질문 나누기, 나의 질문 모음 화면에 사용되는 밝은 판타지 도서관 배경 이미지",
source: "ChatGPT 생성 이미지",
usage: "우리반 책읽기 - 질문 만들기 / 질문 나누기 / 나의 질문 모음 화면 배경",
modified: "크기 및 파일명 수정",
note: "ChatGPT 생성 이미지 / OpenAI 이용약관 증빙자료 확보"
},
{
title: "질문 만들기 피드백 루미 이미지",
category: "이미지",
fileName: "class-reading-lumi-feedback.png",
description: "질문 만들기 화면에서 학생 질문에 대한 피드백을 안내하는 루미 캐릭터 이미지",
source: "ChatGPT 생성 이미지",
usage: "우리반 책읽기 - 질문 만들기 피드백 영역 / 질문 나누기 안내 영역",
modified: "배경 투명 처리 및 크기 수정",
note: "ChatGPT 생성 이미지 / OpenAI 이용약관 증빙자료 확보"
},  {
    category: "이미지",
    title: "개별읽기 메인 배경 이미지",
    fileName: "individual-reading.png",
    description: "개별읽기 메인 화면의 밝은 판타지 도서관 분위기를 나타내는 배경 이미지",
    source: "ChatGPT 생성 이미지",
    usage: "개별읽기 메인 화면 배경",
    modified: "크기 및 화면 비율에 맞게 배치 조정",
    note: "ChatGPT 생성 이미지 / OpenAI 이용약관 증빙자료 확보"
  },
  {
    category: "이미지",
    title: "개별읽기 메인 루미 이미지",
    fileName: "home-lumi.png",
    description: "개별읽기 메인 화면에서 학생을 안내하는 용사 루미 캐릭터 이미지",
    source: "ChatGPT 생성 이미지",
    usage: "개별읽기 메인 화면 안내 캐릭터",
    modified: "투명 배경 처리 및 화면 배치 조정",
    note: "ChatGPT 생성 이미지 / OpenAI 이용약관 증빙자료 확보"
  },
  {
    category: "이미지",
    title: "책 추천 받기 배경 이미지",
    fileName: "book-recommend-bg.png",
    description: "책 추천 받기 화면의 따뜻하고 신비로운 도서관 분위기를 나타내는 배경 이미지",
    source: "ChatGPT 생성 이미지",
    usage: "책 추천 받기 화면 배경",
    modified: "크기 및 화면 비율에 맞게 배치 조정",
    note: "ChatGPT 생성 이미지 / OpenAI 이용약관 증빙자료 확보"
  },
  {
    category: "이미지",
    title: "책 추천 받기 루미 이미지",
    fileName: "book-recommend-lumi.png",
    description: "책 추천 받기 화면에서 학생에게 질문을 던지고 책 추천을 안내하는 용사 루미 캐릭터 이미지",
    source: "ChatGPT 생성 이미지",
    usage: "책 추천 받기 화면 안내 캐릭터",
    modified: "투명 배경 처리 및 화면 배치 조정",
    note: "ChatGPT 생성 이미지 / OpenAI 이용약관 증빙자료 확보"
  },
  {
    category: "이미지",
    title: "오늘의 독서 모험 배경 이미지",
    fileName: "today-reading-adventure-bg.png",
    description: "오늘의 독서 모험 화면의 밝고 신비로운 분위기를 나타내는 배경 이미지",
    source: "ChatGPT 생성 이미지",
    usage: "오늘의 독서 모험 화면 배경",
    modified: "크기 및 화면 비율에 맞게 배치 조정",
    note: "ChatGPT 생성 이미지 / OpenAI 이용약관 증빙자료 확보"
  },
  {
    category: "이미지",
    title: "읽기 전 활동 아이콘 이미지",
    fileName: "today-reading-before-icon.png",
    description: "읽기 전 활동을 나타내는 책과 질문 아이콘 이미지",
    source: "ChatGPT 생성 이미지",
    usage: "오늘의 독서 모험 읽기 전 카드 아이콘",
    modified: "크기 및 카드 배치에 맞게 조정",
    note: "ChatGPT 생성 이미지 / OpenAI 이용약관 증빙자료 확보"
  },
  {
    category: "이미지",
    title: "읽기 중 활동 아이콘 이미지",
    fileName: "today-reading-during-icon.png",
    description: "읽기 중 활동을 나타내는 책과 기록 아이콘 이미지",
    source: "ChatGPT 생성 이미지",
    usage: "오늘의 독서 모험 읽기 중 카드 아이콘",
    modified: "크기 및 카드 배치에 맞게 조정",
    note: "ChatGPT 생성 이미지 / OpenAI 이용약관 증빙자료 확보"
  },
  {
    category: "이미지",
    title: "읽기 후 활동 아이콘 이미지",
    fileName: "today-reading-after-icon.png",
    description: "읽기 후 활동을 나타내는 기록지와 정리 활동 아이콘 이미지",
    source: "ChatGPT 생성 이미지",
    usage: "오늘의 독서 모험 읽기 후 카드 아이콘",
    modified: "크기 및 카드 배치에 맞게 조정",
    note: "ChatGPT 생성 이미지 / OpenAI 이용약관 증빙자료 확보"
  },
  {
    category: "이미지",
    title: "오늘의 독서 모험 루미 안내 이미지",
    fileName: "today-reading-lumi-guide.png",
    description: "오늘의 독서 모험 화면에서 활동 순서를 안내하는 용사 루미 캐릭터 이미지",
    source: "ChatGPT 생성 이미지",
    usage: "오늘의 독서 모험 안내 캐릭터",
    modified: "투명 배경 처리 및 화면 배치 조정",
    note: "ChatGPT 생성 이미지 / OpenAI 이용약관 증빙자료 확보"
  },
  {
    category: "이미지",
    title: "오늘의 독서 모험 루미 강조 이미지",
    fileName: "today-reading-lumi-point.png",
    description: "오늘의 독서 모험 화면에서 특정 활동을 강조하며 안내하는 용사 루미 캐릭터 이미지",
    source: "ChatGPT 생성 이미지",
    usage: "오늘의 독서 모험 활동 안내 캐릭터",
    modified: "투명 배경 처리 및 화면 배치 조정",
    note: "ChatGPT 생성 이미지 / OpenAI 이용약관 증빙자료 확보"
  },
  {
    category: "이미지",
    title: "독서 안내 루미 이미지",
    fileName: "today-reading-lumi-reading.png",
    description: "책을 들고 독서 활동을 안내하는 용사 루미 캐릭터 이미지",
    source: "ChatGPT 생성 이미지",
    usage: "읽기 전·읽기 중 활동 안내 및 피드백 캐릭터",
    modified: "투명 배경 처리 및 화면 배치 조정",
    note: "ChatGPT 생성 이미지 / OpenAI 이용약관 증빙자료 확보"
  },
  {
    category: "이미지",
    title: "개별읽기 읽기 중 배경 이미지",
    fileName: "individual-during-bg.png",
    description: "개별읽기 읽기 중 활동 화면의 따뜻한 독서 공간 분위기를 나타내는 배경 이미지",
    source: "ChatGPT 생성 이미지",
    usage: "개별읽기 읽기 중 활동 화면 배경",
    modified: "크기 및 화면 비율에 맞게 배치 조정",
    note: "ChatGPT 생성 이미지 / OpenAI 이용약관 증빙자료 확보"
  },
  {
    category: "이미지",
    title: "개별읽기 읽기 중 루미 안내 이미지",
    fileName: "individual-during-lumi-guide.png",
    description: "개별읽기 읽기 중 화면에서 질문 만들기와 독서 진행을 안내하는 용사 루미 캐릭터 이미지",
    source: "ChatGPT 생성 이미지",
    usage: "개별읽기 읽기 중 메인 및 피드백 안내 캐릭터",
    modified: "투명 배경 처리 및 화면 배치 조정",
    note: "ChatGPT 생성 이미지 / OpenAI 이용약관 증빙자료 확보"
  },
  {
    category: "이미지",
    title: "읽기 중 질문 만들기 아이콘 이미지",
    fileName: "individual-during-question-icon.png",
    description: "읽기 중 질문 만들기 활동을 나타내는 책과 질문 아이콘 이미지",
    source: "ChatGPT 생성 이미지",
    usage: "개별읽기 읽기 중 질문 만들기 활동 아이콘",
    modified: "크기 및 카드 배치에 맞게 조정",
    note: "ChatGPT 생성 이미지 / OpenAI 이용약관 증빙자료 확보"
  },
  {
    category: "이미지",
    title: "읽기 중 질문 만들기 메뉴 아이콘 이미지",
    fileName: "individual-during-menu-question.png",
    description: "읽기 중 메인 화면의 질문 만들기 메뉴를 나타내는 책과 질문 아이콘 이미지",
    source: "ChatGPT 생성 이미지",
    usage: "개별읽기 읽기 중 메인 질문 만들기 메뉴 아이콘",
    modified: "크기 및 메뉴 카드 배치에 맞게 조정",
    note: "ChatGPT 생성 이미지 / OpenAI 이용약관 증빙자료 확보"
  },
  {
    category: "이미지",
    title: "읽기 중 질문 나누기 메뉴 아이콘 이미지",
    fileName: "individual-during-menu-share.png",
    description: "읽기 중 메인 화면의 질문 나누기 메뉴를 나타내는 소통 아이콘 이미지",
    source: "ChatGPT 생성 이미지",
    usage: "개별읽기 읽기 중 메인 질문 나누기 메뉴 아이콘",
    modified: "크기 및 메뉴 카드 배치에 맞게 조정",
    note: "ChatGPT 생성 이미지 / OpenAI 이용약관 증빙자료 확보"
  },
  {
    category: "이미지",
    title: "읽기 중 나의 질문 모음 메뉴 아이콘 이미지",
    fileName: "individual-during-menu-collection.png",
    description: "읽기 중 메인 화면의 나의 질문 모음 메뉴를 나타내는 책장과 기록 아이콘 이미지",
    source: "ChatGPT 생성 이미지",
    usage: "개별읽기 읽기 중 메인 나의 질문 모음 메뉴 아이콘",
    modified: "크기 및 메뉴 카드 배치에 맞게 조정",
    note: "ChatGPT 생성 이미지 / OpenAI 이용약관 증빙자료 확보"
  },
  {
  title: "게임 던전 지도 배경 이미지",
  category: "이미지",
  fileName: "bg_map.png",
  description: "던전 선택 화면에서 모험의 시작 분위기를 보여주는 판타지 지도 배경 이미지",
  source: "Microsoft Copilot 이미지 생성",
  usage: "게임 던전 선택 화면 배경",
  modified: "파일명 변경 및 게임 화면 비율에 맞게 CSS로 크기와 위치 조정",
  note: "Copilot 생성 이미지 / Microsoft 서비스 약관 증빙자료 확보"
},
{
  title: "게임 초급 던전 배경 이미지",
  category: "이미지",
  fileName: "bg_hatchling.png",
  description: "초급 던전 전투 화면에 사용되는 해츨링 던전 배경 이미지",
  source: "Microsoft Copilot 이미지 생성",
  usage: "게임 초급 던전 전투 화면 배경",
  modified: "파일명 변경 및 전투 화면 비율에 맞게 CSS로 크기와 위치 조정",
  note: "Copilot 생성 이미지 / Microsoft 서비스 약관 증빙자료 확보"
},
{
  title: "게임 중급 던전 배경 이미지",
  category: "이미지",
  fileName: "bg_dragon.png",
  description: "중급 던전 전투 화면에 사용되는 드래곤 던전 배경 이미지",
  source: "Microsoft Copilot 이미지 생성",
  usage: "게임 중급 던전 전투 화면 배경",
  modified: "파일명 변경 및 전투 화면 비율에 맞게 CSS로 크기와 위치 조정",
  note: "Copilot 생성 이미지 / Microsoft 서비스 약관 증빙자료 확보"
},
{
  title: "게임 고급 던전 배경 이미지",
  category: "이미지",
  fileName: "bg_elder.png",
  description: "고급 던전 전투 화면에 사용되는 엘더 드래곤 던전 배경 이미지",
  source: "Microsoft Copilot 이미지 생성",
  usage: "게임 고급 던전 전투 화면 배경",
  modified: "파일명 변경 및 전투 화면 비율에 맞게 CSS로 크기와 위치 조정",
  note: "Copilot 생성 이미지 / Microsoft 서비스 약관 증빙자료 확보"
},
{
  title: "게임 용사 기본 자세 이미지",
  category: "이미지",
  fileName: "hero_idle.png",
  description: "전투 화면에서 용사가 기본 자세로 서 있는 상태를 표현한 캐릭터 이미지",
  source: "Microsoft Copilot 이미지 생성",
  usage: "게임 전투 화면 용사 기본 상태",
  modified: "파일명 변경, 배경 제거형 이미지로 사용, CSS 및 JavaScript로 화면 배치 조정",
  note: "Copilot 생성 이미지 / Microsoft 서비스 약관 증빙자료 확보"
},
{
  title: "게임 용사 공격 자세 이미지",
  category: "이미지",
  fileName: "hero_attack.png",
  description: "전투 화면에서 용사가 공격하는 동작을 표현한 캐릭터 이미지",
  source: "Microsoft Copilot 이미지 생성",
  usage: "게임 전투 화면 용사 공격 모션",
  modified: "파일명 변경, 배경 제거형 이미지로 사용, CSS 및 JavaScript로 공격 연출에 맞게 위치 조정",
  note: "Copilot 생성 이미지 / Microsoft 서비스 약관 증빙자료 확보"
},
{
  title: "게임 용사 피격 이미지",
  category: "이미지",
  fileName: "hero_hit.png",
  description: "전투 화면에서 용사가 공격을 받아 피격된 상태를 표현한 캐릭터 이미지",
  source: "Microsoft Copilot 이미지 생성",
  usage: "게임 전투 화면 용사 피격 모션",
  modified: "파일명 변경, 배경 제거형 이미지로 사용, CSS 및 JavaScript로 피격 연출에 맞게 위치 조정",
  note: "Copilot 생성 이미지 / Microsoft 서비스 약관 증빙자료 확보"
},
{
  title: "게임 용사 쓰러짐 이미지",
  category: "이미지",
  fileName: "hero_dead.png",
  description: "전투 실패 시 용사가 쓰러진 상태를 표현한 캐릭터 이미지",
  source: "Microsoft Copilot 이미지 생성",
  usage: "게임 전투 실패 화면 및 용사 쓰러짐 연출",
  modified: "파일명 변경, 배경 제거형 이미지로 사용, CSS 및 JavaScript로 결과 화면 연출에 맞게 위치 조정",
  note: "Copilot 생성 이미지 / Microsoft 서비스 약관 증빙자료 확보"
},
{
  title: "게임 해츨링 기본 자세 이미지",
  category: "이미지",
  fileName: "hatchling_idle.png",
  description: "초급 던전 몬스터인 해츨링의 기본 자세를 표현한 이미지",
  source: "Microsoft Copilot 이미지 생성",
  usage: "게임 초급 던전 전투 화면 몬스터 기본 상태",
  modified: "파일명 변경, 배경 제거형 이미지로 사용, CSS 및 JavaScript로 화면 배치 조정",
  note: "Copilot 생성 이미지 / Microsoft 서비스 약관 증빙자료 확보"
},
{
  title: "게임 해츨링 공격 이미지",
  category: "이미지",
  fileName: "hatchling_attack.png",
  description: "초급 던전 몬스터인 해츨링이 공격하는 동작을 표현한 이미지",
  source: "Microsoft Copilot 이미지 생성",
  usage: "게임 초급 던전 전투 화면 몬스터 공격 모션",
  modified: "파일명 변경, 배경 제거형 이미지로 사용, CSS 및 JavaScript로 공격 연출에 맞게 위치 조정",
  note: "Copilot 생성 이미지 / Microsoft 서비스 약관 증빙자료 확보"
},
{
  title: "게임 해츨링 피격 이미지",
  category: "이미지",
  fileName: "hatchling_hit.png",
  description: "초급 던전 몬스터인 해츨링이 공격을 받아 피격된 상태를 표현한 이미지",
  source: "Microsoft Copilot 이미지 생성",
  usage: "게임 초급 던전 전투 화면 몬스터 피격 모션",
  modified: "파일명 변경, 배경 제거형 이미지로 사용, CSS 및 JavaScript로 피격 연출에 맞게 위치 조정",
  note: "Copilot 생성 이미지 / Microsoft 서비스 약관 증빙자료 확보"
},
{
  title: "게임 해츨링 쓰러짐 이미지",
  category: "이미지",
  fileName: "hatchling_dead.png",
  description: "초급 던전 몬스터인 해츨링이 쓰러진 상태를 표현한 이미지",
  source: "Microsoft Copilot 이미지 생성",
  usage: "게임 초급 던전 전투 승리 화면 및 몬스터 쓰러짐 연출",
  modified: "파일명 변경, 배경 제거형 이미지로 사용, CSS 및 JavaScript로 결과 화면 연출에 맞게 위치 조정",
  note: "Copilot 생성 이미지 / Microsoft 서비스 약관 증빙자료 확보"
},
{
  title: "게임 드래곤 기본 자세 이미지",
  category: "이미지",
  fileName: "dragon_idle.png",
  description: "중급 던전 몬스터인 드래곤의 기본 자세를 표현한 이미지",
  source: "Microsoft Copilot 이미지 생성",
  usage: "게임 중급 던전 전투 화면 몬스터 기본 상태",
  modified: "파일명 변경, 배경 제거형 이미지로 사용, CSS 및 JavaScript로 화면 배치 조정",
  note: "Copilot 생성 이미지 / Microsoft 서비스 약관 증빙자료 확보"
},
{
  title: "게임 드래곤 공격 이미지",
  category: "이미지",
  fileName: "dragon_attack.png",
  description: "중급 던전 몬스터인 드래곤이 공격하는 동작을 표현한 이미지",
  source: "Microsoft Copilot 이미지 생성",
  usage: "게임 중급 던전 전투 화면 몬스터 공격 모션",
  modified: "파일명 변경, 배경 제거형 이미지로 사용, CSS 및 JavaScript로 공격 연출에 맞게 위치 조정",
  note: "Copilot 생성 이미지 / Microsoft 서비스 약관 증빙자료 확보"
},
{
  title: "게임 드래곤 피격 이미지",
  category: "이미지",
  fileName: "dragon_hit.png",
  description: "중급 던전 몬스터인 드래곤이 공격을 받아 피격된 상태를 표현한 이미지",
  source: "Microsoft Copilot 이미지 생성",
  usage: "게임 중급 던전 전투 화면 몬스터 피격 모션",
  modified: "파일명 변경, 배경 제거형 이미지로 사용, CSS 및 JavaScript로 피격 연출에 맞게 위치 조정",
  note: "Copilot 생성 이미지 / Microsoft 서비스 약관 증빙자료 확보"
},
{
  title: "게임 드래곤 쓰러짐 이미지",
  category: "이미지",
  fileName: "dragon_dead.png",
  description: "중급 던전 몬스터인 드래곤이 쓰러진 상태를 표현한 이미지",
  source: "Microsoft Copilot 이미지 생성",
  usage: "게임 중급 던전 전투 승리 화면 및 몬스터 쓰러짐 연출",
  modified: "파일명 변경, 배경 제거형 이미지로 사용, CSS 및 JavaScript로 결과 화면 연출에 맞게 위치 조정",
  note: "Copilot 생성 이미지 / Microsoft 서비스 약관 증빙자료 확보"
},
{
  title: "게임 엘더 드래곤 기본 자세 이미지",
  category: "이미지",
  fileName: "elder_idle.png",
  description: "고급 던전 몬스터인 엘더 드래곤의 기본 자세를 표현한 이미지",
  source: "Microsoft Copilot 이미지 생성",
  usage: "게임 고급 던전 전투 화면 몬스터 기본 상태",
  modified: "파일명 변경, 배경 제거형 이미지로 사용, CSS 및 JavaScript로 화면 배치 조정",
  note: "Copilot 생성 이미지 / Microsoft 서비스 약관 증빙자료 확보"
},
{
  title: "게임 엘더 드래곤 공격 이미지",
  category: "이미지",
  fileName: "elder_attack.png",
  description: "고급 던전 몬스터인 엘더 드래곤이 공격하는 동작을 표현한 이미지",
  source: "Microsoft Copilot 이미지 생성",
  usage: "게임 고급 던전 전투 화면 몬스터 공격 모션",
  modified: "파일명 변경, 배경 제거형 이미지로 사용, CSS 및 JavaScript로 공격 연출에 맞게 위치 조정",
  note: "Copilot 생성 이미지 / Microsoft 서비스 약관 증빙자료 확보"
},
{
  title: "게임 엘더 드래곤 피격 이미지",
  category: "이미지",
  fileName: "elder_hit.png",
  description: "고급 던전 몬스터인 엘더 드래곤이 공격을 받아 피격된 상태를 표현한 이미지",
  source: "Microsoft Copilot 이미지 생성",
  usage: "게임 고급 던전 전투 화면 몬스터 피격 모션",
  modified: "파일명 변경, 배경 제거형 이미지로 사용, CSS 및 JavaScript로 피격 연출에 맞게 위치 조정",
  note: "Copilot 생성 이미지 / Microsoft 서비스 약관 증빙자료 확보"
},
{
  title: "게임 엘더 드래곤 쓰러짐 이미지",
  category: "이미지",
  fileName: "elder_dead.png",
  description: "고급 던전 몬스터인 엘더 드래곤이 쓰러진 상태를 표현한 이미지",
  source: "Microsoft Copilot 이미지 생성",
  usage: "게임 고급 던전 전투 승리 화면 및 몬스터 쓰러짐 연출",
  modified: "파일명 변경, 배경 제거형 이미지로 사용, CSS 및 JavaScript로 결과 화면 연출에 맞게 위치 조정",
  note: "Copilot 생성 이미지 / Microsoft 서비스 약관 증빙자료 확보"
},
{
  title: "게임 방어 아이콘 이미지",
  category: "이미지",
  fileName: "icon_bangeo.png",
  description: "게임 전투에서 일반 방어 스킬을 나타내는 아이콘 이미지",
  source: "Microsoft Copilot 이미지 생성",
  usage: "게임 전투 화면 방어 스킬 버튼 아이콘",
  modified: "파일명 변경 및 스킬 버튼 크기에 맞게 CSS로 크기 조정",
  note: "Copilot 생성 이미지 / Microsoft 서비스 약관 증빙자료 확보"
},
{
  title: "게임 튜토리얼 아이콘 이미지",
  category: "이미지",
  fileName: "icon_tutorial.png",
  description: "게임 화면에서 튜토리얼 또는 도움말 기능을 나타내는 아이콘 이미지",
  source: "OpenAI ChatGPT 이미지 생성",
  usage: "게임 전투 화면 튜토리얼 바 아이콘",
  modified: "파일명 변경 및 버튼 크기에 맞게 CSS로 크기 조정",
  note: "ChatGPT 생성 이미지 / OpenAI 이용약관 증빙자료 확보"
},
{
  title: "게임 일격 아이콘 이미지",
  category: "이미지",
  fileName: "icon_ilgyeok.png",
  description: "게임 전투에서 일격 스킬을 나타내는 아이콘 이미지",
  source: "OpenAI ChatGPT 이미지 생성",
  usage: "게임 전투 화면 일격 스킬 버튼 아이콘",
  modified: "파일명 변경 및 스킬 버튼 크기에 맞게 CSS로 크기 조정",
  note: "ChatGPT 생성 이미지 / OpenAI 이용약관 증빙자료 확보"
},
{
  title: "게임 연속 베기 아이콘 이미지",
  category: "이미지",
  fileName: "icon_yeonsoek.png",
  description: "게임 전투에서 연속 베기 스킬을 나타내는 아이콘 이미지",
  source: "OpenAI ChatGPT 이미지 생성",
  usage: "게임 전투 화면 연속 베기 스킬 버튼 아이콘",
  modified: "파일명 변경 및 스킬 버튼 크기에 맞게 CSS로 크기 조정",
  note: "ChatGPT 생성 이미지 / OpenAI 이용약관 증빙자료 확보"
},
{
  title: "게임 철벽 아이콘 이미지",
  category: "이미지",
  fileName: "icon_cheolbyeok.png",
  description: "게임 전투에서 철벽 스킬을 나타내는 아이콘 이미지",
  source: "OpenAI ChatGPT 이미지 생성",
  usage: "게임 전투 화면 철벽 스킬 버튼 아이콘",
  modified: "파일명 변경 및 스킬 버튼 크기에 맞게 CSS로 크기 조정",
  note: "ChatGPT 생성 이미지 / OpenAI 이용약관 증빙자료 확보"
},
{
  title: "게임 불꽃 베기 아이콘 이미지",
  category: "이미지",
  fileName: "icon_bulkkot.png",
  description: "게임 전투에서 불꽃 베기 스킬을 나타내는 아이콘 이미지",
  source: "OpenAI ChatGPT 이미지 생성",
  usage: "게임 전투 화면 불꽃 베기 스킬 버튼 아이콘",
  modified: "파일명 변경 및 스킬 버튼 크기에 맞게 CSS로 크기 조정",
  note: "ChatGPT 생성 이미지 / OpenAI 이용약관 증빙자료 확보"
},
{
  title: "게임 화염 폭발 아이콘 이미지",
  category: "이미지",
  fileName: "icon_hwayeom.png",
  description: "게임 전투에서 화염 폭발 스킬을 나타내는 아이콘 이미지",
  source: "OpenAI ChatGPT 이미지 생성",
  usage: "게임 전투 화면 화염 폭발 스킬 버튼 아이콘",
  modified: "파일명 변경 및 스킬 버튼 크기에 맞게 CSS로 크기 조정",
  note: "ChatGPT 생성 이미지 / OpenAI 이용약관 증빙자료 확보"
},
{
  title: "엔딩 인트로 배경 이미지",
  category: "이미지",
  fileName: "bg-ending-library-golden.png",
  description: "고급 던전 클리어 후 지식창고의 문이 다시 열린 장면을 표현한 금빛 판타지 도서관 배경 이미지",
  source: "OpenAI ChatGPT 이미지 생성",
  usage: "학생 엔딩 인트로 화면 배경",
  modified: "파일명 변경 및 엔딩 화면 비율에 맞게 CSS로 밝기와 위치 조정",
  note: "ChatGPT 생성 이미지 / OpenAI 이용약관 증빙자료 확보"
},
{
  title: "엔딩 인트로 분한 용 이미지",
  category: "이미지",
  fileName: "char-ending-dragon-angry.png",
  description: "학생들이 지식창고의 지식을 되찾자 분해하는 용 캐릭터 이미지",
  source: "OpenAI ChatGPT 이미지 생성",
  usage: "학생 엔딩 인트로 1장면 용 캐릭터",
  modified: "파일명 변경, 배경 제거형 이미지로 사용, CSS 및 JavaScript로 화면 배치 조정",
  note: "ChatGPT 생성 이미지 / OpenAI 이용약관 증빙자료 확보"
},
{
  title: "엔딩 인트로 물러나는 용 이미지",
  category: "이미지",
  fileName: "char-ending-dragon-retreat.png",
  description: "책을 읽고 생각을 나누는 힘을 인정하고 물러나는 용 캐릭터 이미지",
  source: "OpenAI ChatGPT 이미지 생성",
  usage: "학생 엔딩 인트로 2장면 용 캐릭터",
  modified: "파일명 변경, 배경 제거형 이미지로 사용, CSS 및 JavaScript로 화면 배치 조정",
  note: "ChatGPT 생성 이미지 / OpenAI 이용약관 증빙자료 확보"
},
{
  title: "엔딩 인트로 승리 루미 이미지",
  category: "이미지",
  fileName: "char-ending-lumi-cheer.png",
  description: "지식창고의 문을 다시 연 학생들을 칭찬하는 루미 캐릭터 이미지",
  source: "OpenAI ChatGPT 이미지 생성",
  usage: "학생 엔딩 인트로 3장면 루미 캐릭터",
  modified: "파일명 변경, 배경 제거형 이미지로 사용, CSS 및 JavaScript로 화면 배치 조정",
  note: "ChatGPT 생성 이미지 / OpenAI 이용약관 증빙자료 확보"
},
{
  title: "엔딩 인트로 안내 루미 이미지",
  category: "이미지",
  fileName: "char-ending-lumi-guide.png",
  description: "모험이 끝난 뒤에도 계속 책을 읽도록 안내하는 루미 캐릭터 이미지",
  source: "OpenAI ChatGPT 이미지 생성",
  usage: "학생 엔딩 인트로 4장면 루미 캐릭터",
  modified: "파일명 변경, 배경 제거형 이미지로 사용, CSS 및 JavaScript로 화면 배치 조정",
  note: "ChatGPT 생성 이미지 / OpenAI 이용약관 증빙자료 확보"
},
  {
    title: "우리반이 읽을 책 소개 배경 이미지",
    category: "이미지",
    fileName: "read-before-intro-bg.png",
    description: "읽기 전 활동을 시작하기 전에 우리 반이 함께 읽을 책을 소개하는 밝은 도서관 배경 이미지",
    source: "OpenAI ChatGPT 이미지 생성",
    location: "학생 읽기 전 인트로 책 소개 화면 배경",
    modified: "파일명 변경, CSS로 화면 배경 배치 조정",
    note: "ChatGPT 생성 이미지 / OpenAI 이용약관 증빙자료 확보"
  },
  {
    title: "개별읽기 메인 루미 이미지",
    category: "이미지",
    fileName: "home-lumi.png",
    description: "개별읽기 메인 화면에서 학생에게 활동을 안내하는 루미 캐릭터 이미지",
    source: "OpenAI ChatGPT 이미지 생성",
    location: "학생 개별읽기 메인 화면 안내 캐릭터",
    modified: "파일명 변경, 배경 제거형 이미지로 사용, CSS로 화면 배치 조정",
    note: "ChatGPT 생성 이미지 / OpenAI 이용약관 증빙자료 확보"
  },
  {
    title: "책 추천받기 버튼 이미지",
    category: "이미지",
    fileName: "마법의_고대_책.png",
    description: "개별읽기 메인 화면에서 책 추천받기 버튼에 사용한 이미지",
    source: "OpenAI ChatGPT 이미지 생성",
    location: "학생 개별읽기 메인 화면 책 추천받기 버튼",
    modified: "파일명 변경, 배경 제거형 이미지로 사용, CSS로 버튼 안에 배치 조정",
    note: "ChatGPT 생성 이미지 / OpenAI 이용약관 증빙자료 확보"
  },
  {
    title: "오늘의 독서모험 버튼 이미지",
    category: "이미지",
    fileName: "금장색_식이_있는_그림.png",
    description: "개별읽기 메인 화면에서 오늘의 독서모험 버튼에 사용한 이미지",
    source: "OpenAI ChatGPT 이미지 생성",
    location: "학생 개별읽기 메인 화면 오늘의 독서모험 버튼",
    modified: "파일명 변경, 배경 제거형 이미지로 사용, CSS로 버튼 안에 배치 조정",
    note: "ChatGPT 생성 이미지 / OpenAI 이용약관 증빙자료 확보"
  },
  {
    title: "던전입장 버튼 이미지",
    category: "이미지",
    fileName: "고대_던전의_석문.png",
    description: "개별읽기 메인 화면에서 던전입장 버튼에 사용한 이미지",
    source: "OpenAI ChatGPT 이미지 생성",
    location: "학생 개별읽기 메인 화면 던전입장 버튼",
    modified: "파일명 변경, 배경 제거형 이미지로 사용, CSS로 버튼 안에 배치 조정",
    note: "ChatGPT 생성 이미지 / OpenAI 이용약관 증빙자료 확보"
  },
  {
  title: "능력치 설명 배경 이미지",
  type: "이미지",
  file: "ability-intro-bg.png",
  description: "능력치 설명 화면에 사용한 밝은 베이지 브라운톤의 신비로운 별빛 배경 이미지",
  source: "OpenAI ChatGPT 이미지 생성",
  location: "능력치 설명 화면 배경",
  modified: "화면 분위기에 맞게 밝기와 색감 조정",
  note: "ChatGPT 생성 이미지 / OpenAI 이용 약관 증빙 자료 확보"
},
{
  title: "읽기 전 질문 만들기 배경 이미지",
  type: "이미지",
  file: "before-reading-bg.png",
  description: "읽기 전 질문 만들기 화면에 사용한 밝은 숲속 모험 분위기의 배경 이미지",
  source: "OpenAI ChatGPT 이미지 생성",
  location: "읽기 전 질문 만들기 화면 배경",
  modified: "화면 분위기에 맞게 밝기와 색감 조정",
  note: "ChatGPT 생성 이미지 / OpenAI 이용 약관 증빙 자료 확보"
},
{
  title: "연습읽기 읽기중 배경 이미지",
  type: "이미지",
  fileName: "mystic-forest-bg.png",
  description: "연습읽기 읽기중 화면의 신비로운 숲 배경 이미지",
  source: "OpenAI ChatGPT 이미지 생성",
  usage: "연습읽기 읽기중 화면",
  edit: "문답책 화면 분위기와 화면 크기에 맞게 CSS로 조정",
  note: "ChatGPT 생성 이미지 / OpenAI 이용약관 증빙자료 확보"
},
];