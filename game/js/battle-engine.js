// battle-engine.js
// 전투 로직 담당 (순수 계산만, DOM 없음)

const BattleEngine = (() => {

  // 플레이어 일반 공격 처리
  function playerNormalAttack(magic) {
    const isCrit = PlayerStats.isCritical(magic);
    const baseDmg = PlayerStats.calcNormalDamage(magic);
    const damage = isCrit ? Math.floor(baseDmg * 2) : baseDmg;
    return { damage, isCrit };
  }

  // 플레이어 강한 공격 처리
  function playerHeavyAttack(magic) {
    const isCrit = PlayerStats.isCritical(magic);
    const baseDmg = PlayerStats.calcHeavyDamage(magic);
    const damage = isCrit ? Math.floor(baseDmg * 2) : baseDmg;
    return { damage, isCrit };
  }

  // 적 일반 공격 처리
  function enemyNormalAttack(dungeonAtk, isDefending) {
    const defenseRate = PlayerStats.calcDefenseRate();
    const damage = isDefending
      ? Math.floor(dungeonAtk * (1 - defenseRate / 100))
      : dungeonAtk;
    return { damage, blocked: isDefending };
  }

  // 적 강한 공격 처리
  function enemyHeavyAttack(dungeonHeavyAtk, isDefending) {
    const defenseRate = PlayerStats.calcDefenseRate();
    const damage = isDefending
      ? Math.floor(dungeonHeavyAtk * (1 - defenseRate / 100))
      : dungeonHeavyAtk;
    return { damage, blocked: isDefending };
  }

  // 전투 종료 여부 확인
  function checkBattleEnd(playerHp, enemyHp, timeLeft) {
    if (enemyHp <= 0)   return 'victory';
    if (playerHp <= 0)  return 'defeat';
    if (timeLeft <= 0)  return 'timeout';
    return null;
  }

  return {
    playerNormalAttack,
    playerHeavyAttack,
    enemyNormalAttack,
    enemyHeavyAttack,
    checkBattleEnd,
  };

})();