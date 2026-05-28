// game-state.js
// studentId 기준 게임 상태 관리
// 전역 변수 하나로 관리하던 방식 → studentId별로 분리

const GameState = (() => {

  // 현재 게임 중인 상태 (메모리)
  let _state = null;

  // 게임 상태 초기화 (던전 입장 시)
  // studentId는 나중에 auth.js 로그인 정보에서 받아옴
  // 지금은 임시로 1 고정
  function init(dungeonIdx) {
    const studentId = 1; // 나중에: sessionStorage.getItem('studentId')
    const playerData = GameAPI.getInitialPlayerState(studentId);
    const dungeon = GameAPI.getDungeon(dungeonIdx);

    _state = {
      studentId:  playerData.studentId,
      heroHp:     playerData.heroHp,
      heroMax:    playerData.heroMax,
      enemyHp:    dungeon.hp,
      enemyMax:   dungeon.hp,
      magic:      playerData.magic,
      wisdom:     playerData.wisdom,
      wins:       playerData.wins,
      qIdx:       0,
      dungeon:    dungeon,
      busy:       false
    };
  }

  // 상태 읽기
  function get() {
    return _state;
  }

  // 정답 결과 반영
  function applyAnswerResult(result) {
    if (!_state) return;
    _state.enemyHp -= result.damage;
    _state.magic   += result.magicGain;
    _state.wisdom  += result.wisdomGain;
    _state.qIdx++;
  }

  // 적 공격 결과 반영
  function applyEnemyAttack(result) {
    if (!_state) return;
    _state.heroHp -= result.damage;
  }

  // 승리 처리
  function applyVictory() {
    if (!_state) return;
    _state.wins++;
  }

  // 현재 질문 반환
  function getCurrentQuestion() {
    if (!_state) return null;
    const questions = _state.dungeon.questions;
    return questions[_state.qIdx % questions.length];
  }

  // busy 상태 변경
  function setBusy(value) {
    if (!_state) return;
    _state.busy = value;
  }

  return {
    init,
    get,
    applyAnswerResult,
    applyEnemyAttack,
    applyVictory,
    getCurrentQuestion,
    setBusy
  };

})();