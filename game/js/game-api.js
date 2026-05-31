// game-api.js
// 데이터 제공 담당
// 나중에 Spring Boot 연동 시 fetch 호출로 교체할 부분

const GameAPI = (() => {

  // 이미지 경로 데이터
  const IMAGES = {
    hero: {
      idle:   'images/hero_idle.png',
      attack: 'images/hero_attack.png',
      hit:    'images/hero_hit.png',
      dead:   'images/hero_dead.png'
    },
    hatchling: {
      idle:   'images/hatchling_idle.png',
      attack: 'images/hatchling_attack.png',
      hit:    'images/hatchling_hit.png',
      dead:   'images/hatchling_dead.png'
    },
    dragon: {
      idle:   'images/dragon_idle.png',
      attack: 'images/dragon_attack.png',
      hit:    'images/dragon_hit.png',
      dead:   'images/dragon_dead.png'
    },
    elder: {
      idle:   'images/elder_idle.png',
      attack: 'images/elder_attack.png',
      hit:    'images/elder_hit.png',
      dead:   'images/elder_dead.png'
    }
  };

  // 던전 데이터
  // 나중에 GET /api/dungeons 로 교체 예정
  const DUNGEONS = [
    {
      id: 'hatchling',
      name: '헤츨링',
      enemyKey: 'hatchling',
      bg: 'images/bg_hatchling.png',
      difficulty: '초급',
      requiredBooks: 10,
      // 적 스탯
      enemy: {
        maxHp: 300,
        normalAtk: 8,        // 일반 공격 데미지
        heavyAtk: 25,        // 강한 공격 데미지
        normalAtkInterval: 2000,   // 일반 공격 주기 (2초)
        heavyAtkInterval: 15000,   // 강한 공격 주기 (15초)
      },
      // 제한 시간 (초)
      timeLimit: 180,
    },
    {
      id: 'dragon',
      name: '용',
      enemyKey: 'dragon',
      bg: 'images/bg_dragon.png',
      difficulty: '중급',
      requiredBooks: 30,
      enemy: {
        maxHp: 600,
        normalAtk: 15,
        heavyAtk: 45,
        normalAtkInterval: 2000,
        heavyAtkInterval: 12000,
      },
      timeLimit: 300,
    },
    {
      id: 'elder',
      name: '나이많은 용',
      enemyKey: 'elder',
      bg: 'images/bg_elder.png',
      difficulty: '고급',
      requiredBooks: 50,
      enemy: {
        maxHp: 1000,
        normalAtk: 25,
        heavyAtk: 80,
        normalAtkInterval: 2000,
        heavyAtkInterval: 10000,
      },
      timeLimit: 420,
    }
  ];

  function getImages() { return IMAGES; }
  function getDungeons() { return DUNGEONS; }
  function getDungeon(idx) { return DUNGEONS[idx]; }

  // 학생 초기 상태 반환
  // 나중에: GET /api/students/{studentId}/game-state
  function getInitialPlayerState(studentId) {
    return {
      studentId: studentId,
      maxHp:  100,
      hp:     100,
      magic:  10,   // 마법력 → 공격력, 크리티컬 확률
      stamina: 10,  // 체력   → 최대 HP
      courage: 0,   // 용기   → 강공격 쿨타임 감소
      books:  0,    // 등록한 책 수 → 던전 입장 조건
    };
  }

  return {
    getImages,
    getDungeons,
    getDungeon,
    getInitialPlayerState
  };

})();