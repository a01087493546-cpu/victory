// battle-engine.js
// 전투 로직 담당 (순수 계산만, DOM 없음)

const BattleEngine = (() => {

  // ── 공격 스킬 (마법력 담당) ──
  // 일격: 기본 베기
  function skillIlgyeok(magic) {
    const isCrit = PlayerStats.isCritical(magic);
    const base = PlayerStats.calcNormalDamage(magic);
    const damage = isCrit ? Math.floor(base * 2) : base;
    return { damage, isCrit, type: 'normal' };
  }

  // 연속베기: 실제 2타 공격
  function skillYeonsoek(magic) {
    const firstCrit = PlayerStats.isCritical(magic);
    const secondCrit = PlayerStats.isCritical(magic);

    const base = PlayerStats.calcNormalDamage(magic);

    // 1타, 2타를 따로 계산해서 실제로 두 번 맞는 느낌을 줌
    const firstBase = Math.floor(base * 0.75);
    const secondBase = Math.floor(base * 0.85);

    const firstDamage = firstCrit ? Math.floor(firstBase * 2) : firstBase;
    const secondDamage = secondCrit ? Math.floor(secondBase * 2) : secondBase;

    return {
      damage: firstDamage + secondDamage,
      hits: [
        { damage: firstDamage, isCrit: firstCrit },
        { damage: secondDamage, isCrit: secondCrit },
      ],
      isCrit: firstCrit || secondCrit,
      type: 'multi'
    };
  }

  // ── 방어 스킬 (용기 담당) ──
  // 방어: 기본 막기 (피해 일부 감소)
  function skillBangeo(dungeonAtk, courage) {
    const defenseRate = PlayerStats.calcDefenseRate(courage);
    const damage = Math.floor(dungeonAtk * (1 - defenseRate / 100));
    return { damage, blocked: true, type: 'defend' };
  }

  // 철벽: 완전 방어 (피해 0)
  function skillCheolbyeok() {
    return { damage: 0, blocked: true, type: 'perfect' };
  }

  // ── 필살 스킬 (지혜 담당) ──
  // 불꽃베기: 강한 공격
  function skillBulkkot(wisdom) {
    const isCrit = PlayerStats.isCritical(wisdom);
    const base = PlayerStats.calcHeavyDamage(wisdom);
    const damage = isCrit ? Math.floor(base * 2) : base;
    return { damage, isCrit, type: 'heavy' };
  }

  // 화염폭발: 초강력 (2배 데미지, 긴 쿨타임)
  function skillHwayeom(wisdom) {
    const isCrit = PlayerStats.isCritical(wisdom);
    const base = Math.floor(PlayerStats.calcHeavyDamage(wisdom) * 2);
    const damage = isCrit ? Math.floor(base * 2) : base;
    return { damage, isCrit, type: 'ultimate' };
  }

  // 기존 호환용
  function playerNormalAttack(magic) { return skillIlgyeok(magic); }
  function playerHeavyAttack(wisdom) { return skillBulkkot(wisdom); }

  function enemyNormalAttack(dungeonAtk, isDefending, defenseMode, courage) {
  // 철벽 상태면 피해를 0으로 막음
  if (isDefending && defenseMode === 'ironWall') {
    return { damage: 0, blocked: true, defenseMode: 'ironWall' };
  }

  // 일반 방어 상태면 피해 감소
  if (isDefending) {
    const defenseRate = PlayerStats.calcDefenseRate(courage);
    const damage = Math.floor(dungeonAtk * (1 - defenseRate / 100));
    return { damage, blocked: true, defenseMode: 'guard' };
  }

  return { damage: dungeonAtk, blocked: false, defenseMode: null };
}

  function enemyHeavyAttack(dungeonHeavyAtk, isDefending, defenseMode, courage) {
  // 철벽 상태면 강한 공격도 피해를 0으로 막음
  if (isDefending && defenseMode === 'ironWall') {
    return { damage: 0, blocked: true, defenseMode: 'ironWall' };
  }

  // 일반 방어 상태면 피해 감소
  if (isDefending) {
    const defenseRate = PlayerStats.calcDefenseRate(courage);
    const damage = Math.floor(dungeonHeavyAtk * (1 - defenseRate / 100));
    return { damage, blocked: true, defenseMode: 'guard' };
  }

  return { damage: dungeonHeavyAtk, blocked: false, defenseMode: null };
}

  function checkBattleEnd(playerHp, enemyHp, timeLeft) {
    if (enemyHp <= 0)  return 'victory';
    if (playerHp <= 0) return 'defeat';
    if (timeLeft <= 0) return 'timeout';
    return null;
  }

  return {
    skillIlgyeok,
    skillYeonsoek,
    skillBangeo,
    skillCheolbyeok,
    skillBulkkot,
    skillHwayeom,
    playerNormalAttack,
    playerHeavyAttack,
    enemyNormalAttack,
    enemyHeavyAttack,
    checkBattleEnd,
  };

})();