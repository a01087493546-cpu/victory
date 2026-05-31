// player-stats.js
// 능력치 계산 담당 (순수 계산 로직만, DOM 없음)
// 시나리오 변경 시 수치만 조정하면 됨

const PlayerStats = (() => {

  // 마법력 → 기본 공격 데미지
  // 마법력 높을수록 데미지 증가
  function calcNormalDamage(magic) {
    return 10 + Math.floor(magic * 0.8);
  }

  // 마법력 → 강한 공격 데미지
  function calcHeavyDamage(magic) {
    return 25 + Math.floor(magic * 1.5);
  }

  // 마법력 → 크리티컬 확률 (%)
  // 마법력 10 기준 5%, 최대 60%
  function calcCritChance(magic) {
    return Math.min(5 + magic * 1.2, 60);
  }

  // 크리티컬 발동 여부 판정
  function isCritical(magic) {
    return Math.random() * 100 < calcCritChance(magic);
  }

  // 체력(stamina) → 최대 HP
  // 체력 10 기준 100, 체력 1 증가마다 HP 5 증가
  function calcMaxHp(stamina) {
    return 100 + stamina * 5;
  }

  // 용기 → 강한 공격 쿨타임 (ms)
  // 용기 높을수록 쿨타임 감소, 최소 5초
  function calcHeavyCooldown(courage) {
    return Math.max(5000, 20000 - courage * 150);
  }

  // 방어 시 받는 데미지 감소율 (%)
  // 지금은 고정값, 나중에 능력치 연동 가능
  function calcDefenseRate() {
    return 80; // 방어 시 80% 데미지 감소
  }

  // 용기 100 → 다른 능력치 +3 전환
  function convertCourage(stats, targetStat) {
    if (stats.courage < 100) return null;
    const newStats = { ...stats, courage: 0 };
    if (targetStat === 'magic')   newStats.magic   = Math.min(100, newStats.magic   + 3);
    if (targetStat === 'stamina') newStats.stamina = Math.min(100, newStats.stamina + 3);
    return newStats;
  }

  return {
    calcNormalDamage,
    calcHeavyDamage,
    calcCritChance,
    isCritical,
    calcMaxHp,
    calcHeavyCooldown,
    calcDefenseRate,
    convertCourage,
  };

})();