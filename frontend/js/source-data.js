var sourceData = [
  {
    type: "그림",
    fileName: "dragon-main.png",
    name: "메인 드래곤 캐릭터 이미지",
    description: "지식창고를 빼앗은 메인 드래곤 캐릭터",
    source: "OpenAI ChatGPT 이미지 생성",
    usedIn: "로그인 화면 메인 캐릭터 영역",
    note: "확보: ChatGPT 생성 이미지 / OpenAI 이용약관 증빙자료 확보"
  },
  {
    type: "그림",
    fileName: "role-student.png",
    name: "학생 역할 선택 캐릭터 이미지",
    description: "학생 역할 선택 카드에 들어가는 학생 용사 이미지",
    source: "OpenAI ChatGPT 이미지 생성",
    usedIn: "로그인 화면 학생 역할 선택 카드 및 메인 캐릭터 장면",
    note: "확보: ChatGPT 생성 이미지 / OpenAI 이용약관 증빙자료 확보"
  },
  {
    type: "그림",
    fileName: "role-teacher.png",
    name: "교사 역할 선택 캐릭터 이미지",
    description: "교사 역할 선택 카드에 들어가는 교사 캐릭터 이미지",
    source: "OpenAI ChatGPT 이미지 생성",
    usedIn: "로그인 화면 교사 역할 선택 카드",
    note: "확보: ChatGPT 생성 이미지 / OpenAI 이용약관 증빙자료 확보"
  },
  {
    type: "그림",
    fileName: "logo-emblem.png",
    name: "문답책 로고 엠블럼 이미지",
    description: "책, 용, 질문표를 활용한 문답책 로고 엠블럼",
    source: "OpenAI ChatGPT 이미지 생성",
    usedIn: "로그인 화면 왼쪽 상단 로고 영역(logo-book.png 이름으로 학생·교사 화면 상단에서도 재사용)",
    note: "확보: ChatGPT 생성 이미지 / OpenAI 이용약관 증빙자료 확보"
  },
  {
    type: "폰트",
    fileName: "Google Fonts 웹폰트",
    name: "Noto Serif KR",
    description: "문답책 제목에 사용하는 한글 세리프 폰트",
    source: "Google Fonts / Noto Fonts",
    usedIn: "문답책 제목",
    note: "확보: Google Fonts FAQ / SIL Open Font License 1.1 증빙자료 확보"
  },
  {
    type: "폰트",
    fileName: "Google Fonts 웹폰트",
    name: "Noto Sans KR",
    description: "문제, 설명, 본문에 사용하는 기본 한글 폰트",
    source: "Google Fonts / Noto Fonts",
    usedIn: "문제, 설명, 본문 등 기본 텍스트",
    note: "확보: Google Fonts FAQ / SIL Open Font License 1.1 증빙자료 확보"
  },
  {
    type: "폰트",
    fileName: "pretendard-dynamic-subset.css",
    name: "Pretendard",
    description: "버튼, 카드 제목, 메뉴 등 UI 요소에 사용하는 폰트",
    source: "Pretendard 공식 GitHub / jsDelivr CDN",
    usedIn: "버튼, 카드 제목, 메뉴 등 UI 요소",
    note: "확보: Pretendard GitHub LICENSE / SIL Open Font License 1.1 증빙자료 확보"
  },
  {
    type: "그림",
    fileName: "bg-story-01~06",
    name: "스토리 인트로 배경 이미지 6종",
    description: "용의 지식 욕심, 지식창고 습격, 텅 빈 도서관, 문답책, 질문의 힘, 훈련 시작 장면을 표현한 배경 이미지",
    source: "OpenAI ChatGPT 이미지 생성",
    usedIn: "스토리 인트로 화면",
    note: "확보: ChatGPT 생성 이미지 / OpenAI 이용약관 증빙자료 확보"
  },{
  title: "마법력 아이콘",
  category: "그림",
  file: "ability-magic.png",
  description: "독서 활동에서 마법력 능력치를 나타내는 별빛 마법 아이콘 이미지",
  source: "OpenAI ChatGPT 이미지 생성",
  usage: "능력치 안내 화면, 개별읽기 보상 화면",
  note: "ChatGPT 생성 이미지 / OpenAI 이용약관 증빙자료 확보"
},
{
  title: "체력 아이콘",
  category: "그림",
  file: "ability-health.png",
  description: "독서 활동에서 체력 능력치를 나타내는 하트 아이콘 이미지",
  source: "OpenAI ChatGPT 이미지 생성",
  usage: "능력치 안내 화면, 개별읽기 보상 화면",
  note: "ChatGPT 생성 이미지 / OpenAI 이용약관 증빙자료 확보"
},
{
  title: "지혜 아이콘",
  category: "그림",
  file: "ability-wisdom.png",
  description: "독서 활동에서 지혜 능력치를 나타내는 책 아이콘 이미지",
  source: "OpenAI ChatGPT 이미지 생성",
  usage: "능력치 안내 화면, 개별읽기 보상 화면",
  note: "ChatGPT 생성 이미지 / OpenAI 이용약관 증빙자료 확보"
},
{
  title: "용기 아이콘",
  category: "그림",
  file: "ability-courage.png",
  description: "독서 활동에서 용기 능력치를 나타내는 방패 아이콘 이미지",
  source: "OpenAI ChatGPT 이미지 생성",
  usage: "능력치 안내 화면, 개별읽기 보상 화면, 책 친구에게 소개 글쓰기 화면",
  note: "ChatGPT 생성 이미지 / OpenAI 이용약관 증빙자료 확보"

  },{
  title: "연습읽기 도서관 배경 이미지",
  category: "그림",
  file: "home-library-bg.png",
  description: "책 속 모험 분위기를 나타내는 판타지 도서관 배경 이미지",
  source: "OpenAI ChatGPT 이미지 생성",
  usage: "연습읽기 메인 및 읽기 전 활동 화면 배경",
  note: "ChatGPT 생성 이미지 / OpenAI 이용약관 증빙자료 확보"
},
{
    title: "연습하기 - 읽기 전 단계 이미지",
    category: "그림",
    fileName: "practice-before-reading.png",
    description: "읽기 전 생각 열기 활동을 나타내는 책 위의 빛나는 전구 이미지",
    source: "OpenAI ChatGPT 이미지 생성",
    usage: "학생 연습하기 화면 읽기 전 카드",
    note: "ChatGPT 생성 이미지 / OpenAI 이용약관 증빙자료 확보"
  },
  {
    title: "연습하기 - 읽기 중 단계 이미지",
    category: "그림",
    fileName: "practice-during-reading.png",
    description: "읽기 중 질문하며 깊이 읽기 활동을 나타내는 열린 책, 돋보기, 물음표 이미지",
    source: "OpenAI ChatGPT 이미지 생성",
    usage: "학생 연습하기 화면 읽기 중 카드, 연습읽기 읽기 중 안내 화면",
    note: "ChatGPT 생성 이미지 / OpenAI 이용약관 증빙자료 확보"
  },
  {
    title: "연습하기 - 읽기 후 단계 이미지",
    category: "그림",
    fileName: "practice-after-reading.png",
    description: "읽기 후 내용을 간추리는 활동을 나타내는 책, 두루마리, 깃펜 이미지",
    source: "OpenAI ChatGPT 이미지 생성",
    usage: "학생 연습하기 화면 읽기 후 카드",
    note: "ChatGPT 생성 이미지 / OpenAI 이용약관 증빙자료 확보"
  },    
  {
    title: "읽기 전 인트로 배경 이미지",
    category: "그림",
    fileName: "pre-reading-bg-magic-greenhouse.png",
    description: "읽기 전 활동을 시작하기 전에 보여주는 판타지 도서관 배경 이미지",
    source: "OpenAI ChatGPT 이미지 생성",
    usage: "학생 읽기 전·읽기 중 인트로 화면 배경",
    note: "ChatGPT 생성 이미지 / OpenAI 이용약관 증빙자료 확보"
  },
  {
    title: "읽기 전 인트로 1단계 루미 안내 이미지",
    category: "그림",
    fileName: "pre-reading-lumi-clue-book.png",
    description: "책을 펼치기 전 표지와 제목에 숨은 단서를 살펴보도록 안내하는 루미 캐릭터 이미지",
    source: "OpenAI ChatGPT 이미지 생성",
    usage: "학생 읽기 전 인트로 1단계 및 읽기 전 질문 활동 안내 영역",
    note: "ChatGPT 생성 이미지 / OpenAI 이용약관 증빙자료 확보"
  },
  {
    title: "읽기 전 인트로 2단계 루미 안내 이미지",
    category: "그림",
    fileName: "pre-reading-lumi-thinking.png",
    description: "책을 읽기 전에 궁금한 점을 떠올리도록 안내하는 루미 캐릭터 이미지",
    source: "OpenAI ChatGPT 이미지 생성",
    usage: "학생 읽기 전 인트로 2단계 및 함께 읽을 책 소개 화면 안내 영역",
    note: "ChatGPT 생성 이미지 / OpenAI 이용약관 증빙자료 확보"
  },
  {
    title: "읽기 전 인트로 3단계 루미 안내 이미지",
    category: "그림",
    fileName: "pre-reading-lumi-question-note.png",
    description: "읽기 전 질문을 만들어 보도록 안내하는 루미 캐릭터 이미지",
    source: "OpenAI ChatGPT 이미지 생성",
    usage: "학생 읽기 전 인트로 3단계 및 읽기 전 질문 피드백 영역, 읽기후 인트로 화면 2장면 캐릭터(lumi-after-intro-02-serious.png로 복사)",
    note: "ChatGPT 생성 이미지 / OpenAI 이용약관 증빙자료 확보"
  },
{
  title: "읽기 중 인트로 루미 장면 1 이미지",
  category: "그림",
  fileName: "during-reading-lumi-01-goodjob.png",
  description: "읽기 전 활동을 끝낸 학생을 칭찬하는 루미 캐릭터 이미지",
  source: "OpenAI ChatGPT 이미지 생성",
  usage: "학생 읽기 중 활동 인트로 1장면",
  note: "ChatGPT 생성 이미지 / OpenAI 이용약관 증빙자료 확보"
},
{
  title: "읽기 중 인트로 루미 장면 2 이미지",
  category: "그림",
  fileName: "during-reading-lumi-02-question-note.png",
  description: "책을 읽으며 생긴 궁금함에 답해 보는 활동을 안내하는 루미 캐릭터 이미지",
  source: "OpenAI ChatGPT 이미지 생성",
  usage: "학생 읽기 중 활동 인트로 2장면(today-reading-lumi-reading.png 및 after-read-lumi-guide.png 이름으로 개별읽기 화면에서도 재사용)",
  note: "ChatGPT 생성 이미지 / OpenAI 이용약관 증빙자료 확보"
},
{
  title: "읽기 중 인트로 루미 장면 3 이미지",
  category: "그림",
  fileName: "during-reading-lumi-03-practice.png",
  description: "읽기 중 질문의 종류를 하나씩 연습하고 책읽기에 적용해 보자고 안내하는 루미 캐릭터 이미지",
  source: "OpenAI ChatGPT 이미지 생성",
  usage: "학생 읽기 중 활동 인트로 3장면, 읽기 중 질문 연습 방법 알기 화면 및 기본 연습 피드백 박스(today-reading-lumi-guide.png 이름으로 개별읽기 화면에서도 재사용)",
  note: "ChatGPT 생성 이미지 / OpenAI 이용약관 증빙자료 확보"
},
{
  category: "그림",
  name: "책 내용에서 바로 답을 찾는 질문 아이콘",
  file: "frontend/assets/images/during-reading/intro/icon-direct-answer.png",
  description: "책 속 문장에서 바로 답을 찾는 질문 유형을 나타내는 아이콘",
  source: "ChatGPT 생성 이미지",
  license: "OpenAI 이용약관에 따름",
  usage: "읽기 중 질문 연습 - 질문 종류 선택 화면",
  createdDate: "2026-06-04",
  note: "ChatGPT 생성 이미지 / OpenAI 이용약관 증빙자료 확보"
},
{
  category: "그림",
  name: "책 내용에서 답을 짐작하는 질문 아이콘",
  file: "frontend/assets/images/during-reading/intro/icon-infer-answer.png",
  description: "글 속 단서를 바탕으로 답을 짐작하는 질문 유형을 나타내는 아이콘",
  source: "ChatGPT 생성 이미지",
  license: "OpenAI 이용약관에 따름",
  usage: "읽기 중 질문 연습 - 질문 종류 선택 화면",
  createdDate: "2026-06-04",
  note: "ChatGPT 생성 이미지 / OpenAI 이용약관 증빙자료 확보"
},
{
  category: "그림",
  name: "생각이나 느낌 질문 아이콘",
  file: "frontend/assets/images/during-reading/intro/icon-thought-feeling.png",
  description: "책을 읽고 떠오른 생각이나 느낌을 묻는 질문 유형을 나타내는 아이콘",
  source: "ChatGPT 생성 이미지",
  license: "OpenAI 이용약관에 따름",
  usage: "읽기 중 질문 연습 - 질문 종류 선택 화면",
  createdDate: "2026-06-04",
  note: "ChatGPT 생성 이미지 / OpenAI 이용약관 증빙자료 확보"
},
{
  category: "그림",
  name: "삶과 관련짓는 질문 아이콘",
  file: "frontend/assets/images/during-reading/intro/icon-life-connection.png",
  description: "책 속 내용과 자신의 경험을 연결하는 질문 유형을 나타내는 아이콘",
  source: "ChatGPT 생성 이미지",
  license: "OpenAI 이용약관에 따름",
  usage: "읽기 중 질문 연습 - 질문 종류 선택 화면",
  createdDate: "2026-06-04",
  note: "ChatGPT 생성 이미지 / OpenAI 이용약관 증빙자료 확보"
},
{
title: "우리반 책읽기 배경 이미지",
category: "그림",
fileName: "class-reading-bg.png",
description: "우리반 책읽기 메인 화면의 어두운 판타지 도서관 분위기 배경 이미지",
source: "ChatGPT 생성 이미지",
usage: "우리반 책읽기 메인 화면 배경",
note: "ChatGPT 생성 이미지 / OpenAI 이용약관 증빙자료 확보"
},
{
title: "우리반 책읽기 루미 이미지",
category: "그림",
fileName: "class-reading-lumi.png",
description: "우리반 책읽기 메인 화면에서 안내 역할을 하는 루미 캐릭터 이미지",
source: "ChatGPT 생성 이미지",
usage: "우리반 책읽기 메인 화면 안내 영역(class-reading-lumi-feedback.png 이름으로 질문 피드백 영역에서도 재사용)",
note: "ChatGPT 생성 이미지 / OpenAI 이용약관 증빙자료 확보"
},
{
title: "질문 만들기 아이콘",
category: "그림",
fileName: "class-reading-icon-create.png",
description: "우리반 책읽기 메인 화면의 질문 만들기 활동을 나타내는 아이콘",
source: "ChatGPT 생성 이미지",
usage: "우리반 책읽기 메인 화면 - 질문 만들기 카드",
note: "ChatGPT 생성 이미지 / OpenAI 이용약관 증빙자료 확보"
},
{
title: "질문 나누기 아이콘",
category: "그림",
fileName: "class-reading-icon-share.png",
description: "우리반 책읽기 메인 화면의 질문 나누기 활동을 나타내는 아이콘",
source: "ChatGPT 생성 이미지",
usage: "우리반 책읽기 메인 화면 - 질문 나누기 카드",
note: "ChatGPT 생성 이미지 / OpenAI 이용약관 증빙자료 확보"
},
{
title: "질문 만들기 밝은 도서관 배경 이미지",
category: "그림",
fileName: "class-reading-question-bg.png",
description: "질문 만들기, 질문 나누기, 나의 질문 모음 화면에 사용되는 밝은 판타지 도서관 배경 이미지",
source: "ChatGPT 생성 이미지",
usage: "우리반 책읽기 - 질문 만들기 / 질문 나누기 / 나의 질문 모음 화면 배경",
note: "ChatGPT 생성 이미지 / OpenAI 이용약관 증빙자료 확보"
},
{
    category: "그림",
    title: "개별읽기 메인 배경 이미지",
    fileName: "individual-reading.png",
    description: "개별읽기 메인 화면의 밝은 판타지 도서관 분위기를 나타내는 배경 이미지",
    source: "ChatGPT 생성 이미지",
    usage: "개별읽기 메인 화면 배경",
    note: "ChatGPT 생성 이미지 / OpenAI 이용약관 증빙자료 확보"
  },
  {
    category: "그림",
    title: "책 추천 받기 배경 이미지",
    fileName: "book-recommend-bg.png",
    description: "책 추천 받기 화면의 따뜻하고 신비로운 도서관 분위기를 나타내는 배경 이미지",
    source: "ChatGPT 생성 이미지",
    usage: "책 추천 받기 화면 배경",
    note: "ChatGPT 생성 이미지 / OpenAI 이용약관 증빙자료 확보"
  },
  {
    category: "그림",
    title: "오늘의 독서 모험 배경 이미지",
    fileName: "today-reading-adventure-bg.png",
    description: "오늘의 독서 모험 화면의 밝고 신비로운 분위기를 나타내는 배경 이미지",
    source: "ChatGPT 생성 이미지",
    usage: "오늘의 독서 모험 화면 배경",
    note: "ChatGPT 생성 이미지 / OpenAI 이용약관 증빙자료 확보"
  },
  {
    category: "그림",
    title: "읽기 전 활동 아이콘 이미지",
    fileName: "today-reading-before-icon.png",
    description: "읽기 전 활동을 나타내는 책과 질문 아이콘 이미지",
    source: "ChatGPT 생성 이미지",
    usage: "오늘의 독서 모험 읽기 전 카드 아이콘",
    note: "ChatGPT 생성 이미지 / OpenAI 이용약관 증빙자료 확보"
  },
  {
    category: "그림",
    title: "읽기 중 활동 아이콘 이미지",
    fileName: "today-reading-during-icon.png",
    description: "읽기 중 활동을 나타내는 책과 기록 아이콘 이미지",
    source: "ChatGPT 생성 이미지",
    usage: "오늘의 독서 모험 읽기 중 카드 아이콘",
    note: "ChatGPT 생성 이미지 / OpenAI 이용약관 증빙자료 확보"
  },
  {
    category: "그림",
    title: "읽기 후 활동 아이콘 이미지",
    fileName: "today-reading-after-icon.png",
    description: "읽기 후 활동을 나타내는 기록지와 정리 활동 아이콘 이미지",
    source: "ChatGPT 생성 이미지",
    usage: "오늘의 독서 모험 읽기 후 카드 아이콘",
    note: "ChatGPT 생성 이미지 / OpenAI 이용약관 증빙자료 확보"
  },
  {
    category: "그림",
    title: "오늘의 독서 모험 루미 강조 이미지",
    fileName: "today-reading-lumi-point.png",
    description: "오늘의 독서 모험 화면에서 특정 활동을 강조하며 안내하는 용사 루미 캐릭터 이미지",
    source: "ChatGPT 생성 이미지",
    usage: "오늘의 독서 모험 활동 안내 캐릭터, 개별읽기 보상 화면(reward-lumi-cheer.png로 복사)",
    note: "ChatGPT 생성 이미지 / OpenAI 이용약관 증빙자료 확보"
  },
  {
    category: "그림",
    title: "개별읽기 읽기 중 루미 안내 이미지",
    fileName: "individual-during-lumi-guide.png",
    description: "개별읽기 읽기 중 화면에서 질문 만들기와 독서 진행을 안내하는 용사 루미 캐릭터 이미지",
    source: "ChatGPT 생성 이미지",
    usage: "개별읽기 읽기 중 메인 및 피드백 안내 캐릭터",
    note: "ChatGPT 생성 이미지 / OpenAI 이용약관 증빙자료 확보"
  },
  {
    category: "그림",
    title: "읽기 중 질문 만들기 메뉴 아이콘 이미지",
    fileName: "individual-during-menu-question.png",
    description: "읽기 중 메인 화면의 질문 만들기 메뉴를 나타내는 책과 질문 아이콘 이미지",
    source: "ChatGPT 생성 이미지",
    usage: "개별읽기 읽기 중 메인 질문 만들기 메뉴 아이콘",
    note: "ChatGPT 생성 이미지 / OpenAI 이용약관 증빙자료 확보"
  },
  {
    category: "그림",
    title: "읽기 중 질문 나누기 메뉴 아이콘 이미지",
    fileName: "individual-during-menu-share.png",
    description: "읽기 중 메인 화면의 질문 나누기 메뉴를 나타내는 소통 아이콘 이미지",
    source: "ChatGPT 생성 이미지",
    usage: "개별읽기 읽기 중 메인 질문 나누기 메뉴 아이콘",
    note: "ChatGPT 생성 이미지 / OpenAI 이용약관 증빙자료 확보"
  },
  {
    category: "그림",
    title: "읽기 중 나의 질문 모음 메뉴 아이콘 이미지",
    fileName: "individual-during-menu-collection.png",
    description: "읽기 중 메인 화면의 나의 질문 모음 메뉴를 나타내는 책장과 기록 아이콘 이미지",
    source: "ChatGPT 생성 이미지",
    usage: "개별읽기 읽기 중 메인 나의 질문 모음 메뉴 아이콘",
    note: "ChatGPT 생성 이미지 / OpenAI 이용약관 증빙자료 확보"
  },
  {
  title: "게임 던전 지도 배경 이미지",
  category: "그림",
  fileName: "bg_map.png",
  description: "던전 선택 화면에서 모험의 시작 분위기를 보여주는 판타지 지도 배경 이미지",
  source: "Microsoft Copilot 이미지 생성",
  usage: "게임 던전 선택 화면 배경",
  note: "Copilot 생성 이미지 / Microsoft Copilot 이용약관 증빙자료 확보"
},
{
  title: "게임 초급 던전 배경 이미지",
  category: "그림",
  fileName: "bg_hatchling.png",
  description: "초급 던전 전투 화면에 사용되는 해츨링 던전 배경 이미지",
  source: "Microsoft Copilot 이미지 생성",
  usage: "게임 초급 던전 전투 화면 배경",
  note: "Copilot 생성 이미지 / Microsoft Copilot 이용약관 증빙자료 확보"
},
{
  title: "게임 중급 던전 배경 이미지",
  category: "그림",
  fileName: "bg_dragon.png",
  description: "중급 던전 전투 화면에 사용되는 드래곤 던전 배경 이미지",
  source: "Microsoft Copilot 이미지 생성",
  usage: "게임 중급 던전 전투 화면 배경",
  note: "Copilot 생성 이미지 / Microsoft Copilot 이용약관 증빙자료 확보"
},
{
  title: "게임 고급 던전 배경 이미지",
  category: "그림",
  fileName: "bg_elder.png",
  description: "고급 던전 전투 화면에 사용되는 엘더 드래곤 던전 배경 이미지",
  source: "Microsoft Copilot 이미지 생성",
  usage: "게임 고급 던전 전투 화면 배경",
  note: "Copilot 생성 이미지 / Microsoft Copilot 이용약관 증빙자료 확보"
},
{
  title: "게임 용사 기본 자세 이미지",
  category: "그림",
  fileName: "hero_idle.png",
  description: "전투 화면에서 용사가 기본 자세로 서 있는 상태를 표현한 캐릭터 이미지",
  source: "Microsoft Copilot 이미지 생성",
  usage: "게임 전투 화면 용사 기본 상태",
  note: "Copilot 생성 이미지 / Microsoft Copilot 이용약관 증빙자료 확보"
},
{
  title: "게임 용사 공격 자세 이미지",
  category: "그림",
  fileName: "hero_attack.png",
  description: "전투 화면에서 용사가 공격하는 동작을 표현한 캐릭터 이미지",
  source: "Microsoft Copilot 이미지 생성",
  usage: "게임 전투 화면 용사 공격 모션",
  note: "Copilot 생성 이미지 / Microsoft Copilot 이용약관 증빙자료 확보"
},
{
  title: "게임 용사 피격 이미지",
  category: "그림",
  fileName: "hero_hit.png",
  description: "전투 화면에서 용사가 공격을 받아 피격된 상태를 표현한 캐릭터 이미지",
  source: "Microsoft Copilot 이미지 생성",
  usage: "게임 전투 화면 용사 피격 모션",
  note: "Copilot 생성 이미지 / Microsoft Copilot 이용약관 증빙자료 확보"
},
{
  title: "게임 용사 쓰러짐 이미지",
  category: "그림",
  fileName: "hero_dead.png",
  description: "전투 실패 시 용사가 쓰러진 상태를 표현한 캐릭터 이미지",
  source: "Microsoft Copilot 이미지 생성",
  usage: "게임 전투 실패 화면 및 용사 쓰러짐 연출",
  note: "Copilot 생성 이미지 / Microsoft Copilot 이용약관 증빙자료 확보"
},
{
  title: "게임 초급 용 기본 자세 이미지",
  category: "그림",
  fileName: "hatchling_idle.png",
  description: "초급 던전 몬스터인 초급 용의 기본 자세를 표현한 이미지",
  source: "Microsoft Copilot 이미지 생성",
  usage: "게임 초급 던전 전투 화면 몬스터 기본 상태",
  note: "Copilot 생성 이미지 / Microsoft Copilot 이용약관 증빙자료 확보"
},
{
  title: "게임 초급 용 공격 이미지",
  category: "그림",
  fileName: "hatchling_attack.png",
  description: "초급 던전 몬스터인 초급 용이 공격하는 동작을 표현한 이미지",
  source: "Microsoft Copilot 이미지 생성",
  usage: "게임 초급 던전 전투 화면 몬스터 공격 모션",
  note: "Copilot 생성 이미지 / Microsoft Copilot 이용약관 증빙자료 확보"
},
{
  title: "게임 초급 용 피격 이미지",
  category: "그림",
  fileName: "hatchling_hit.png",
  description: "초급 던전 몬스터인 초급 용이 공격을 받아 피격된 상태를 표현한 이미지",
  source: "Microsoft Copilot 이미지 생성",
  usage: "게임 초급 던전 전투 화면 몬스터 피격 모션",
  note: "Copilot 생성 이미지 / Microsoft Copilot 이용약관 증빙자료 확보"
},
{
  title: "게임 초급 용 쓰러짐 이미지",
  category: "그림",
  fileName: "hatchling_dead.png",
  description: "초급 던전 몬스터인 초급 용이 쓰러진 상태를 표현한 이미지",
  source: "Microsoft Copilot 이미지 생성",
  usage: "게임 초급 던전 전투 승리 화면 및 몬스터 쓰러짐 연출",
  note: "Copilot 생성 이미지 / Microsoft Copilot 이용약관 증빙자료 확보"
},
{
  title: "게임 중급 용 기본 자세 이미지",
  category: "그림",
  fileName: "dragon_idle.png",
  description: "중급 던전 몬스터인 중급 용의 기본 자세를 표현한 이미지",
  source: "Microsoft Copilot 이미지 생성",
  usage: "게임 중급 던전 전투 화면 몬스터 기본 상태",
  note: "Copilot 생성 이미지 / Microsoft Copilot 이용약관 증빙자료 확보"
},
{
  title: "게임 중급 용 공격 이미지",
  category: "그림",
  fileName: "dragon_attack.png",
  description: "중급 던전 몬스터인 중급 용이 공격하는 동작을 표현한 이미지",
  source: "Microsoft Copilot 이미지 생성",
  usage: "게임 중급 던전 전투 화면 몬스터 공격 모션",
  note: "Copilot 생성 이미지 / Microsoft Copilot 이용약관 증빙자료 확보"
},
{
  title: "게임 중급 용 피격 이미지",
  category: "그림",
  fileName: "dragon_hit.png",
  description: "중급 던전 몬스터인 중급 용이 공격을 받아 피격된 상태를 표현한 이미지",
  source: "Microsoft Copilot 이미지 생성",
  usage: "게임 중급 던전 전투 화면 몬스터 피격 모션",
  note: "Copilot 생성 이미지 / Microsoft Copilot 이용약관 증빙자료 확보"
},
{
  title: "게임 중급 용 쓰러짐 이미지",
  category: "그림",
  fileName: "dragon_dead.png",
  description: "중급 던전 몬스터인 중급 용이 쓰러진 상태를 표현한 이미지",
  source: "Microsoft Copilot 이미지 생성",
  usage: "게임 중급 던전 전투 승리 화면 및 몬스터 쓰러짐 연출",
  note: "Copilot 생성 이미지 / Microsoft Copilot 이용약관 증빙자료 확보"
},
{
  title: "게임 고급 용 드래곤 기본 자세 이미지",
  category: "그림",
  fileName: "elder_idle.png",
  description: "고급 던전 몬스터인 고급 용의 기본 자세를 표현한 이미지",
  source: "Microsoft Copilot 이미지 생성",
  usage: "게임 고급 던전 전투 화면 몬스터 기본 상태",
  note: "Copilot 생성 이미지 / Microsoft Copilot 이용약관 증빙자료 확보"
},
{
  title: "게임 고급 용 드래곤 공격 이미지",
  category: "그림",
  fileName: "elder_attack.png",
  description: "고급 던전 몬스터인 고급 용이 공격하는 동작을 표현한 이미지",
  source: "Microsoft Copilot 이미지 생성",
  usage: "게임 고급 던전 전투 화면 몬스터 공격 모션",
  note: "Copilot 생성 이미지 / Microsoft Copilot 이용약관 증빙자료 확보"
},
{
  title: "게임 고급 용 드래곤 피격 이미지",
  category: "그림",
  fileName: "elder_hit.png",
  description: "고급 던전 몬스터인 고급 용이 공격을 받아 피격된 상태를 표현한 이미지",
  source: "Microsoft Copilot 이미지 생성",
  usage: "게임 고급 던전 전투 화면 몬스터 피격 모션",
  note: "Copilot 생성 이미지 / Microsoft Copilot 이용약관 증빙자료 확보"
},
{
  title: "게임 고급 용 쓰러짐 이미지",
  category: "그림",
  fileName: "elder_dead.png",
  description: "고급 던전 몬스터인 고급 용ㄴ이 쓰러진 상태를 표현한 이미지",
  source: "Microsoft Copilot 이미지 생성",
  usage: "게임 고급 던전 전투 승리 화면 및 몬스터 쓰러짐 연출",
  note: "Copilot 생성 이미지 / Microsoft Copilot 이용약관 증빙자료 확보"
},
{
  title: "게임 방어 아이콘 이미지",
  category: "그림",
  fileName: "icon_bangeo.png",
  description: "게임 전투에서 일반 방어 스킬을 나타내는 아이콘 이미지",
  source: "Microsoft Copilot 이미지 생성",
  usage: "게임 전투 화면 방어 스킬 버튼 아이콘",
  note: "Copilot 생성 이미지 / Microsoft Copilot 이용약관 증빙자료 확보"
},
{
  title: "게임 튜토리얼 아이콘 이미지",
  category: "그림",
  fileName: "icon_tutorial.png",
  description: "게임 화면에서 튜토리얼 또는 도움말 기능을 나타내는 아이콘 이미지",
  source: "OpenAI ChatGPT 이미지 생성",
  usage: "게임 전투 화면 튜토리얼 바 아이콘",
  note: "ChatGPT 생성 이미지 / OpenAI 이용약관 증빙자료 확보"
},
{
  title: "게임 일격 아이콘 이미지",
  category: "그림",
  fileName: "icon_ilgyeok.png",
  description: "게임 전투에서 일격 스킬을 나타내는 아이콘 이미지",
  source: "OpenAI ChatGPT 이미지 생성",
  usage: "게임 전투 화면 일격 스킬 버튼 아이콘",
  note: "ChatGPT 생성 이미지 / OpenAI 이용약관 증빙자료 확보"
},
{
  title: "게임 연속 베기 아이콘 이미지",
  category: "그림",
  fileName: "icon_yeonsoek.png",
  description: "게임 전투에서 연속 베기 스킬을 나타내는 아이콘 이미지",
  source: "OpenAI ChatGPT 이미지 생성",
  usage: "게임 전투 화면 연속 베기 스킬 버튼 아이콘",
  note: "ChatGPT 생성 이미지 / OpenAI 이용약관 증빙자료 확보"
},
{
  title: "게임 철벽 아이콘 이미지",
  category: "그림",
  fileName: "icon_cheolbyeok.png",
  description: "게임 전투에서 철벽 스킬을 나타내는 아이콘 이미지",
  source: "OpenAI ChatGPT 이미지 생성",
  usage: "게임 전투 화면 철벽 스킬 버튼 아이콘",
  note: "ChatGPT 생성 이미지 / OpenAI 이용약관 증빙자료 확보"
},
{
  title: "게임 불꽃 베기 아이콘 이미지",
  category: "그림",
  fileName: "icon_bulkkot.png",
  description: "게임 전투에서 불꽃 베기 스킬을 나타내는 아이콘 이미지",
  source: "OpenAI ChatGPT 이미지 생성",
  usage: "게임 전투 화면 불꽃 베기 스킬 버튼 아이콘",
  note: "ChatGPT 생성 이미지 / OpenAI 이용약관 증빙자료 확보"
},
{
  title: "게임 화염 폭발 아이콘 이미지",
  category: "그림",
  fileName: "icon_hwayeom.png",
  description: "게임 전투에서 화염 폭발 스킬을 나타내는 아이콘 이미지",
  source: "OpenAI ChatGPT 이미지 생성",
  usage: "게임 전투 화면 화염 폭발 스킬 버튼 아이콘",
  note: "ChatGPT 생성 이미지 / OpenAI 이용약관 증빙자료 확보"
},
{
  title: "엔딩 인트로 배경 이미지",
  category: "그림",
  fileName: "bg-ending-library-golden.png",
  description: "고급 던전 클리어 후 지식창고의 문이 다시 열린 장면을 표현한 금빛 판타지 도서관 배경 이미지",
  source: "OpenAI ChatGPT 이미지 생성",
  usage: "학생 엔딩 인트로 화면 배경",
  note: "ChatGPT 생성 이미지 / OpenAI 이용약관 증빙자료 확보"
},
{
  title: "엔딩 인트로 분한 용 이미지",
  category: "그림",
  fileName: "char-ending-dragon-angry.png",
  description: "학생들이 지식창고의 지식을 되찾자 분해하는 용 캐릭터 이미지",
  source: "OpenAI ChatGPT 이미지 생성",
  usage: "학생 엔딩 인트로 1장면 용 캐릭터",
  note: "ChatGPT 생성 이미지 / OpenAI 이용약관 증빙자료 확보"
},
{
  title: "엔딩 인트로 물러나는 용 이미지",
  category: "그림",
  fileName: "char-ending-dragon-retreat.png",
  description: "책을 읽고 생각을 나누는 힘을 인정하고 물러나는 용 캐릭터 이미지",
  source: "OpenAI ChatGPT 이미지 생성",
  usage: "학생 엔딩 인트로 2장면 용 캐릭터",
  note: "ChatGPT 생성 이미지 / OpenAI 이용약관 증빙자료 확보"
},
{
  title: "엔딩 인트로 승리 루미 이미지",
  category: "그림",
  fileName: "char-ending-lumi-cheer.png",
  description: "지식창고의 문을 다시 연 학생들을 칭찬하는 루미 캐릭터 이미지",
  source: "OpenAI ChatGPT 이미지 생성",
  usage: "학생 엔딩 인트로 3장면 루미 캐릭터",
  note: "ChatGPT 생성 이미지 / OpenAI 이용약관 증빙자료 확보"
},
{
  title: "엔딩 인트로 안내 루미 이미지",
  category: "그림",
  fileName: "char-ending-lumi-guide.png",
  description: "모험이 끝난 뒤에도 계속 책을 읽도록 안내하는 루미 캐릭터 이미지",
  source: "OpenAI ChatGPT 이미지 생성",
  usage: "학생 엔딩 인트로 4장면 루미 캐릭터",
  note: "ChatGPT 생성 이미지 / OpenAI 이용약관 증빙자료 확보"
},
  {
    title: "우리반이 읽을 책 소개 배경 이미지",
    category: "그림",
    fileName: "read-before-intro-bg.png",
    description: "읽기 전 활동을 시작하기 전에 우리 반이 함께 읽을 책을 소개하는 밝은 도서관 배경 이미지",
    source: "OpenAI ChatGPT 이미지 생성",
    location: "학생 읽기 전 인트로 책 소개 화면 배경",
    note: "ChatGPT 생성 이미지 / OpenAI 이용약관 증빙자료 확보"
  },
  {
    title: "책 추천받기 버튼 이미지",
    category: "그림",
    fileName: "고풍스러운_필기_도구와_책.png",
    description: "개별읽기 메인 화면에서 책 추천받기 버튼에 사용한 이미지",
    source: "OpenAI ChatGPT 이미지 생성",
    location: "학생 개별읽기 메인 화면 책 추천받기 버튼",
    note: "ChatGPT 생성 이미지 / OpenAI 이용약관 증빙자료 확보"
  },
  {
    title: "오늘의 독서모험 버튼 이미지",
    category: "그림",
    fileName: "마법의_나침반_장식.png",
    description: "개별읽기 메인 화면에서 오늘의 독서모험 버튼에 사용한 이미지",
    source: "OpenAI ChatGPT 이미지 생성",
    location: "학생 개별읽기 메인 화면 오늘의 독서모험 버튼",
    note: "ChatGPT 생성 이미지 / OpenAI 이용약관 증빙자료 확보"
  },
  {
    title: "던전입장 버튼 이미지",
    category: "그림",
    fileName: "고대_던전의_석문.png",
    description: "개별읽기 메인 화면에서 던전입장 버튼에 사용한 이미지",
    source: "OpenAI ChatGPT 이미지 생성",
    location: "학생 개별읽기 메인 화면 던전입장 버튼",
    note: "ChatGPT 생성 이미지 / OpenAI 이용약관 증빙자료 확보"
  },
  {
  title: "능력치 설명 배경 이미지",
  type: "그림",
  file: "ability-intro-bg.png",
  description: "능력치 설명 화면에 사용한 밝은 베이지 브라운톤의 신비로운 별빛 배경 이미지",
  source: "OpenAI ChatGPT 이미지 생성",
  location: "능력치 설명 화면 배경",
  note: "ChatGPT 생성 이미지 / OpenAI 이용 약관 증빙 자료 확보"
},
{
  title: "읽기 전 질문 만들기 배경 이미지",
  type: "그림",
  file: "before-reading-bg.png",
  description: "읽기 전 질문 만들기 화면에 사용한 밝은 숲속 모험 분위기의 배경 이미지",
  source: "OpenAI ChatGPT 이미지 생성",
  location: "읽기 전 질문 만들기 화면 배경",
  note: "ChatGPT 생성 이미지 / OpenAI 이용 약관 증빙 자료 확보"
},
{
  title: "연습읽기 읽기중 배경 이미지",
  type: "그림",
  fileName: "during-reading-magic-space-bg.png",
  description: "연습읽기 읽기중 화면에 사용하는 밝은 베이지 톤의 우주 마법 별빛 배경 이미지",
  source: "OpenAI ChatGPT 이미지 생성",
  usage: "연습읽기 읽기중 화면",
  note: "ChatGPT 생성 이미지 / OpenAI 이용약관 증빙자료 확보"
},
{
  title: "책수다방 배경 이미지",
  category: "그림",
  fileName: "book-chat-bg.png",
  description: "연습읽기 책수다방 화면에 사용되는 구름 속 용의 기운이 느껴지는 베이지톤 배경 이미지",
  source: "OpenAI ChatGPT 이미지 생성",
  usage: "연습읽기 책수다방 화면 배경",
  note: "ChatGPT 생성 이미지 / OpenAI 이용약관 증빙자료 확보"
},
{
  title: "읽기후 신비로운 책 우주 배경 이미지",
  category: "그림",
  fileName: "after-reading-space-bg.png",
  description: "읽기후 활동 화면에서 사용하는 밝은 베이지톤의 신비로운 우주 배경 이미지",
  source: "OpenAI ChatGPT 이미지 생성",
  usage: "연습읽기 읽기후 활동 화면 배경 이미지",
  note: "ChatGPT 생성 이미지 / OpenAI 이용약관 증빙자료 확보"
},
{
  title: "스토리 인트로 루미 캐릭터 이미지 6종",
  category: "그림",
  fileName: "char-lumi-01-basic.png, char-lumi-02-surprised.png, char-lumi-03-serious.png, char-lumi-04-mysterious.png, char-lumi-05-confident.png, char-lumi-06-smile.png",
  description: "스토리 인트로 6장면(bg-story-01~06 배경)에 등장하는 루미 캐릭터 이미지. char-lumi-05-confident.png는 book-recommend-lumi.png 이름으로 책 추천 화면에서도 재사용",
  source: "OpenAI ChatGPT 이미지 생성",
  usage: "스토리 인트로 화면 1~6장면 캐릭터, char-lumi-01-basic.png는 개별읽기 메인 화면 연습읽기(온책읽기) 카드에도 재사용",
  note: "ChatGPT 생성 이미지 / OpenAI 이용약관 증빙자료 확보"
},
{
  title: "읽기후 책 유형 아이콘 이미지 3종",
  category: "그림",
  fileName: "after-read-icon-story.png, after-read-icon-info.png, after-read-icon-opinion.png",
  description: "읽기후 활동 화면에서 이야기책·정보책·주장책 유형을 구분해 나타내는 아이콘 이미지",
  source: "OpenAI ChatGPT 이미지 생성",
  usage: "학생 읽기후 활동 화면 - 책 유형 선택 영역",
  note: "ChatGPT 생성 이미지 / OpenAI 이용약관 증빙자료 확보"
},
{
  title: "읽기후 메인 배경 이미지",
  category: "그림",
  fileName: "after-reading-main-bg.png",
  description: "읽기후 관련 화면에서 사용하는 배경 이미지",
  source: "OpenAI ChatGPT 이미지 생성",
  usage: "교사 홈 화면 배경, 교사 회원가입 화면 배경",
  note: "ChatGPT 생성 이미지 / OpenAI 이용약관 증빙자료 확보"
},
{
  title: "읽기후 인트로 배경 이미지",
  category: "그림",
  fileName: "bg-after-reading-intro.png",
  description: "읽기후 인트로 화면에서 사용하는 배경 이미지",
  source: "OpenAI ChatGPT 이미지 생성",
  usage: "학생 읽기후 인트로 화면 배경",
  note: "ChatGPT 생성 이미지 / OpenAI 이용약관 증빙자료 확보"
},
{
  title: "읽기후 인트로 루미 캐릭터 이미지 3종",
  category: "그림",
  fileName: "lumi-after-intro-01-smile.png",
  description: "읽기후 활동을 시작하는 학생에게 읽기 전·중·후 생각을 이어 간추리도록 안내하는 루미 캐릭터 이미지. 2·3장면은 최초 출처 항목에 기록된 루미 그림을 재사용",
  source: "OpenAI ChatGPT 이미지 생성",
  usage: "학생 읽기후 인트로 화면 1장면 캐릭터",
  note: "ChatGPT 생성 이미지 / OpenAI 이용약관 증빙자료 확보"
},
{
  title: "개별읽기 진행 화면 공통 배경 이미지",
  category: "그림",
  fileName: "individual-during-home-bg.png",
  description: "개별읽기 읽기중·읽기후·기록보관함 화면에서 공통으로 사용하는 배경 이미지",
  source: "OpenAI ChatGPT 이미지 생성",
  usage: "개별읽기 읽기중 화면, 개별읽기 읽기후 화면, 개별읽기 기록보관함 화면 배경",
  note: "ChatGPT 생성 이미지 / OpenAI 이용약관 증빙자료 확보"
},
{
  title: "로그인 화면 모험 배경 이미지",
  category: "그림",
  fileName: "login-adventure-bg.png",
  description: "로그인 화면 왼쪽 영역에 사용하는 모험 분위기 배경 이미지",
  source: "OpenAI ChatGPT 이미지 생성",
  usage: "로그인 첫 화면 왼쪽 배경",
  note: "ChatGPT 생성 이미지 / OpenAI 이용약관 증빙자료 확보"
},
{
title: "심사계정 연습읽기 책표지 이미지",
category: "그림",
fileName: "demo_class_book_cover.png",
description: "심사계정 연습읽기에서 사용하는 가상 도서 ‘나만의 보물 찾기’의 책표지 이미지",
source: "OpenAI ChatGPT 이미지 생성",
usage: "심사계정 연습읽기 책 선택 및 연습읽기 관련 화면의 도서 표지",
note: "ChatGPT 생성 이미지 / OpenAI 이용약관 증빙자료 확보"
},

];
