// game-api.js
// 데이터 제공 담당

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

  // 전투 밸런스 참고용 데이터(서버가 관리하지 않는 값들만).
  // difficulty 값('초급'/'중급'/'고급')으로 서버 응답(GET /api/dungeons)과 매칭한다.
  const ENEMY_CONFIG = [
    {
      difficulty: '초급',
      enemyKey: 'hatchling',
      bg: 'images/bg_hatchling.png',
      enemy: {
        maxHp: 300,
        normalAtk: 8,
        heavyAtk: 25,
        normalAtkInterval: 2000,
        heavyAtkInterval: 15000,
      },
      timeLimit: 180,
    },
    {
      difficulty: '중급',
      enemyKey: 'dragon',
      bg: 'images/bg_dragon.png',
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
      difficulty: '고급',
      enemyKey: 'elder',
      bg: 'images/bg_elder.png',
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

  // 서버에서 받아온 던전 목록 캐시. fetchDungeonsFromServer()가 채운다.
  let _cachedDungeons = [];

  function getImages() { return IMAGES; }

  function getApiBaseUrl() {
    if (window.location.hostname === '127.0.0.1') {
      return 'http://127.0.0.1:8080';
    }
    return 'http://localhost:8080';
  }

  function authHeaders() {
    return { Authorization: 'Bearer ' + sessionStorage.getItem('token') };
  }

  function mergeWithEnemyConfig(serverDungeon) {
    const config = ENEMY_CONFIG.find(function (c) {
      return c.difficulty === serverDungeon.difficulty;
    });

    return {
      // 서버 응답 필드
      apiId: serverDungeon.id,
      name: serverDungeon.name,
      description: serverDungeon.description,
      difficulty: serverDungeon.difficulty,
      requiredBooks: serverDungeon.requiredBooks,
      requiredStatAvg: serverDungeon.requiredStatAvg,
      bookCount: serverDungeon.bookCount,
      statAverage: serverDungeon.statAverage,
      cleared: serverDungeon.cleared,
      eligible: serverDungeon.eligible,
      blockedReasons: serverDungeon.blockedReasons,
      attemptsLeftToday: serverDungeon.attemptsLeftToday,

      // 전투 밸런스 참고용(클라이언트 전용)
      enemyKey: config ? config.enemyKey : null,
      bg: config ? config.bg : null,
      enemy: config ? config.enemy : null,
      timeLimit: config ? config.timeLimit : null,
    };
  }

  async function fetchDungeonsFromServer() {
    const response = await fetch(getApiBaseUrl() + '/api/dungeons', {
      headers: authHeaders()
    });

    if (response.status === 401) {
      alert('로그인이 필요합니다.');
      window.location.href = '../frontend/index.html';
      throw new Error('로그인이 필요합니다.');
    }

    if (!response.ok) {
      throw new Error('던전 목록 조회 실패: ' + response.status);
    }

    const serverDungeons = await response.json();
    _cachedDungeons = serverDungeons.map(mergeWithEnemyConfig);

    return _cachedDungeons;
  }

  function getDungeon(idx) { return _cachedDungeons[idx]; }

  async function submitBattleResult(apiId, result) {
    try {
      const response = await fetch(getApiBaseUrl() + '/api/dungeons/' + apiId + '/battle-result', {
        method: 'POST',
        headers: Object.assign({ 'Content-Type': 'application/json' }, authHeaders()),
        body: JSON.stringify({ result: result })
      });

      if (response.status === 409) {
        alert('오늘 이 던전은 더 도전할 수 없어요.');
        return null;
      }

      if (!response.ok) {
        console.error('전투 결과 제출 실패', response.status);
        return null;
      }

      return await response.json();
    } catch (error) {
      console.error('전투 결과 제출 중 오류', error);
      return null;
    }
  }

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
      wisdom:  10,  // 지혜   → 방어 시 데미지 감소율 보너스
      books:  0,    // 등록한 책 수 → 던전 입장 조건
    };
  }

  return {
    getImages,
    getDungeon,
    getInitialPlayerState,
    fetchDungeonsFromServer,
    submitBattleResult
  };

})();
