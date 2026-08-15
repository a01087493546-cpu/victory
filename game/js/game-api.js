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

  /*
   * 전투 밸런스 참고용 데이터(서버가 관리하지 않는 값들만).
   * difficulty 값('초급'/'중급'/'고급')으로 서버 응답(GET /api/dungeons)과 매칭한다.
   *
   * maxHp는 던전 전투 능력치 연계 시스템(소모형 자원 + 강화된 쿨타임)을
   * 도입하면서 함께 재조정한 값이다 - 기존 300/600/1000은 새 자원
   * 시스템에서 몬스터가 사실상 즉사하는 수준이라, 던전 기준 능력치(T)로
   * 산출 가능한 최대 피해량 대비 몬스터가 견디는 시간이 늘어나도록
   * 자동 전투 시뮬레이션(스크립트 기반, 실제 UI 조작이 아님)으로
   * 초급 T=20/중급 T=55/고급 T=85 기준 "정상적으로 플레이하면 승리하되
   * 여유가 크지 않고, 기준보다 1.5배 모으면 확실히 유리해지는" 목표에
   * 맞춰 다시 산출했다.
   */
  const ENEMY_CONFIG = [
    {
      difficulty: '초급',
      enemyKey: 'hatchling',
      bg: 'images/bg_hatchling.png',
      enemy: {
        maxHp: 440,
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
        maxHp: 825,
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
        maxHp: 1240,
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
    if (window.location.hostname === 'localhost') {
      return 'http://localhost:8080';
    }
    return 'https://victory-production-f94d.up.railway.app';
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
      rewardValue: serverDungeon.rewardValue,
      /*
       * 심사계정은 공용 DB has_seen_ending을 절대 갖지 않으므로 서버는
       * 항상 hasEnded:false를 내려준다 - 이 브라우저의 로컬 엔딩 완료
       * 상태(mq_demo_hasSeenEnding)를 아는 쪽(호출부)에서 덮어써야 한다.
       */
      hasEnded: Boolean(serverDungeon.hasEnded),

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

  /*
   * 전투 시작 자원(능력치)을 반환한다. 던전 입장 조건 평균과는 별개로,
   * 여기서 반환하는 4개 값이 전투 중 실제로 소모되는 자원이 된다.
   *
   * - 일반계정: 학생이 실제 독서활동으로 모은 student_stats를 그대로
   *   사용한다(GET /api/students/{id}/stats, 나의 힘 화면과 같은
   *   엔드포인트 재사용). 이 값을 전투 중에 깎아도 원본 DB 값은
   *   전혀 건드리지 않는다 - 여기서 반환하는 객체는 매 전투 시작마다
   *   새로 만들어지는 사본이다.
   * - 심사계정: 모든 던전을 바로 체험해야 하므로 mq_demo_studentStats를
   *   쓰지 않고, 선택한 던전의 입장 기준 능력치(requiredStatAvg = T)를
   *   그대로 전투 시작값으로 임시 제공한다. 이 값은 mq_demo_studentStats에
   *   저장하지 않으며, 클리어 보상(10/15/0 SET)은 기존 로직 그대로
   *   mq_demo_studentStats에만 반영된다.
   */
  async function getInitialPlayerState(studentId, dungeon) {
    if (typeof isDemoAccount === 'function' && isDemoAccount()) {
      const T = Math.round((dungeon && dungeon.requiredStatAvg) || 20);
      return { studentId, magic: T, stamina: T, wisdom: T, courage: T };
    }

    try {
      const response = await fetch(getApiBaseUrl() + '/api/students/' + studentId + '/stats', {
        headers: authHeaders()
      });

      if (!response.ok) throw new Error('능력치 조회 실패: ' + response.status);

      const stats = await response.json();

      return {
        studentId,
        magic: Number(stats.magic) || 0,
        stamina: Number(stats.stamina) || 0,
        wisdom: Number(stats.wisdom) || 0,
        courage: Number(stats.courage) || 0,
      };
    } catch (error) {
      console.error('전투 시작 능력치 조회 실패, 던전 입장 조건 평균으로 대체합니다.', error);
      const fallback = Math.round((dungeon && dungeon.statAverage) || 10);
      return { studentId, magic: fallback, stamina: fallback, wisdom: fallback, courage: fallback };
    }
  }

  return {
    getImages,
    getDungeon,
    getInitialPlayerState,
    fetchDungeonsFromServer,
    submitBattleResult
  };

})();
