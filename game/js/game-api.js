// game-api.js
// 데이터 제공 담당
// 나중에 Spring Boot 연동 시 fetch 호출로 교체할 부분

const GameAPI = (() => {

  // 이미지 경로 데이터
  // 나중에 서버에서 받아올 수 있음
  const IMAGES = {
    hero: {
      idle:   'images/Copilot_20260520_145541.png',
      attack: 'images/Copilot_20260520_145707.png',
      hit:    'images/Copilot_20260520_145735.png',
      dead:   'images/Copilot_20260520_145812.png'
    },
    enemy1: {
      idle:   'images/Copilot_20260520_145843.png',
      attack: 'images/Copilot_20260520_150000.png',
      hit:    'images/Copilot_20260520_150048.png',
      dead:   'images/Copilot_20260520_150131.png'
    },
    enemy2: {
      idle:   'images/Copilot_20260520_150244.png',
      attack: 'images/Copilot_20260520_150314.png',
      hit:    'images/Copilot_20260520_150350.png',
      dead:   'images/Copilot_20260520_150419.png'
    },
    enemy3: {
      idle:   'images/Copilot_20260520_150501.png',
      attack: 'images/Copilot_20260520_150551.png',
      hit:    'images/Copilot_20260520_150622.png',
      dead:   'images/Copilot_20260520_150648.png'
    }
  };

  // 던전 데이터
  // 나중에 GET /api/dungeons 로 교체 예정
  const DUNGEONS = [
    {
      name: '헤츨링', enemyKey: 'enemy1',
      bg: 'images/Copilot_20260520_145314.png',
      hp: 60, atk: 8,
      questions: [
        { q: '책에서 바로 찾을 수 있는 질문은?', choices: ['주인공은 어디 살아?','왜 이 책을 썼을까?','너라면 어떻게 했을까?','이 책의 주제는?'], answer: 0 },
        { q: "'자신의 삶과 관련짓는 질문'의 예시는?", choices: ['주인공 이름은?','결말은 어떻게 됐어?','나도 비슷한 경험이 있었는데...','배경은 어디야?'], answer: 2 },
        { q: '좋은 질문의 특징은?', choices: ['짧을수록 좋다','예/아니오로만 답할 수 있다','생각을 깊게 하게 만든다','정답이 하나뿐이다'], answer: 2 }
      ]
    },
    {
      name: '용', enemyKey: 'enemy2',
      bg: 'images/Copilot_20260520_145422.png',
      hp: 100, atk: 15,
      questions: [
        { q: "책 내용에서 답을 '짐작'할 수 있는 질문은?", choices: ['주인공 나이는?','왜 그런 선택을 했을까?','책 제목이 뭐야?','몇 페이지야?'], answer: 1 },
        { q: '읽기 전 활동으로 가장 적절한 것은?', choices: ['줄거리 외우기','제목 보고 내용 상상하기','결말 먼저 읽기','모르는 단어 찾기'], answer: 1 },
        { q: '이야기책 간추리기에서 중요한 것은?', choices: ['글쓴이 정보','시간과 장소의 변화','책 가격','페이지 수'], answer: 1 }
      ]
    },
    {
      name: '나이많은 용', enemyKey: 'enemy3',
      bg: 'images/Copilot_20260520_145503.png',
      hp: 150, atk: 25,
      questions: [
        { q: '가장 깊이 있는 질문 유형은?', choices: ['책에서 찾는 질문','예/아니오 질문','삶과 연결하는 질문','사실 확인 질문'], answer: 2 },
        { q: 'AI 피드백 후에도 꼭 필요한 것은?', choices: ['다시 AI에게','교사 피드백','친구와 비교','책 다시 읽기'], answer: 1 },
        { q: '오완독 챌린지의 목적은?', choices: ['빨리 읽기','꾸준한 독서 습관 만들기','어려운 책 읽기','시험 대비'], answer: 1 }
      ]
    }
  ];

  // 이미지 데이터 반환
  // 나중에: return fetch('/api/images').then(r => r.json())
  function getImages() {
    return IMAGES;
  }

  // 던전 목록 반환
  // 나중에: return fetch('/api/dungeons').then(r => r.json())
  function getDungeons() {
    return DUNGEONS;
  }

  // 특정 던전 반환
  function getDungeon(idx) {
    return DUNGEONS[idx];
  }

  // 학생 초기 상태 반환
  // 나중에: return fetch('/api/students/' + studentId + '/game-state').then(r => r.json())
  function getInitialPlayerState(studentId) {
    return {
      studentId: studentId,
      heroHp:  100,
      heroMax: 100,
      magic:   10,
      wisdom:  10,
      wins:    0
    };
  }

  // 외부에 공개할 함수만 반환
  return {
    getImages,
    getDungeons,
    getDungeon,
    getInitialPlayerState
  };

})();