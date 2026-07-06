// game-state.js
// studentId 기준 게임 상태 관리

const GameState = (() => {

  let _state = null;

  function init(dungeonIdx) {
    const studentId = sessionStorage.getItem('studentId') || '1';
    const playerData = GameAPI.getInitialPlayerState(studentId);
    const dungeon = GameAPI.getDungeon(dungeonIdx);

    _state = {
      studentId:   playerData.studentId,
      dungeon:     dungeon,

      // 플레이어 상태
      player: {
        hp:       PlayerStats.calcMaxHp(playerData.stamina),
        maxHp:    PlayerStats.calcMaxHp(playerData.stamina),
        magic:    playerData.magic,
        stamina:  playerData.stamina,
        courage:  playerData.courage,
        wisdom:   playerData.wisdom,
        isDefending: false,
        defenseMode: null, // 현재 방어 종류: null / 'guard' / 'ironWall'
      },

      // 적 상태
      enemy: {
        hp:     dungeon.enemy.maxHp,
        maxHp:  dungeon.enemy.maxHp,
      },

      // 타이머
      timeLeft: dungeon.timeLimit,

      // 쿨타임 (ms)
      cooldowns: {
        normal:     0,
        heavy:      0,
        ilgyeok:    0,
        yeonsoek:   0,
        bangeo:     0,
        cheolbyeok: 0,
        bulkkot:    0,
        hwayeom:    0,
      },

      // 전투 진행 여부
      isRunning: false,
    };
  }

  function get() { return _state; }

  // 플레이어 HP 변경
  function damagePlayer(amount) {
    if (!_state) return;
    _state.player.hp = Math.max(0, _state.player.hp - amount);
  }

  // 적 HP 변경
  function damageEnemy(amount) {
    if (!_state) return;
    _state.enemy.hp = Math.max(0, _state.enemy.hp - amount);
  }

  // 방어 상태 변경
  // mode 값:
  // - 'guard'    : 일반 방어
  // - 'ironWall' : 철벽 방어
  // - null       : 방어 해제
  function setDefending(value, mode) {
    if (!_state) return;

    _state.player.isDefending = value;
    _state.player.defenseMode = value ? (mode || 'guard') : null;
  }

  // 타이머 감소
  function tickTimer() {
    if (!_state) return;
    _state.timeLeft = Math.max(0, _state.timeLeft - 1);
  }

  // 쿨타임 설정
  function setCooldown(type, ms) {
    if (!_state) return;
    _state.cooldowns[type] = ms;
  }

  // 쿨타임 감소
  function tickCooldown(type, ms) {
    if (!_state) return;
    _state.cooldowns[type] = Math.max(0, _state.cooldowns[type] - ms);
  }

  // 전투 시작/종료
  function setRunning(value) {
    if (!_state) return;
    _state.isRunning = value;
  }

  // 저장용 데이터 추출
  // 나중에: PUT /api/students/{studentId}/game-state
  function toSaveData() {
    if (!_state) return null;
    return {
      studentId: _state.studentId,
      stats: {
        magic:   _state.player.magic,
        stamina: _state.player.stamina,
        courage: _state.player.courage,
        wisdom:  _state.player.wisdom,
      }
    };
  }

  return {
    init,
    get,
    damagePlayer,
    damageEnemy,
    setDefending,
    tickTimer,
    setCooldown,
    tickCooldown,
    setRunning,
    toSaveData,
  };

})();