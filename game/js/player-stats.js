// player-stats.js
// 능력치 계산 담당 (순수 계산 로직만, DOM 없음)
// 시나리오 변경 시 수치만 조정하면 됨

const PlayerStats = (() => {

  // 정답 시 마법력 증가량
  // 기존 game.js: state.magic += 2
  function getMagicGain() {
    return 2;
  }

  // 정답 시 지혜 증가량
  // 기존 game.js: state.wisdom += 1
  function getWisdomGain() {
    return 1;
  }

  // 마법력 기반 공격력 계산
  // 기존 game.js: 15 + Math.floor(state.magic * 0.5)
  function calcAttackDamage(magic) {
    return 15 + Math.floor(magic * 0.5);
  }

  // 나중에 시나리오 확정되면 추가될 능력치들
  // 체력(인내심), 용기, 협동력 등
  // function calcStaminaBonus(stamina) { ... }
  // function calcCritChance(magic) { ... }

  return {
    getMagicGain,
    getWisdomGain,
    calcAttackDamage
  };

})();