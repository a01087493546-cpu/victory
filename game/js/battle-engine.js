// battle-engine.js
// 전투 로직 담당 (순수 계산만, DOM 없음)
// 화면 제어는 dungeon-ui.js에서만 함

const BattleEngine = (() => {

  // 정답 처리
  // 반환값: { correct: true/false, damage: 숫자, magicGain: 숫자, wisdomGain: 숫자 }
  function processAnswer(selectedIdx, correctIdx, magic) {
    if (selectedIdx === correctIdx) {
      return {
        correct: true,
        damage: PlayerStats.calcAttackDamage(magic),
        magicGain: PlayerStats.getMagicGain(),
        wisdomGain: PlayerStats.getWisdomGain()
      };
    } else {
      return {
        correct: false,
        damage: 0,
        magicGain: 0,
        wisdomGain: 0
      };
    }
  }

  // 적 공격 처리
  // 반환값: { damage: 숫자 }
  function processEnemyAttack(dungeonAtk) {
    return {
      damage: dungeonAtk
    };
  }

  // 전투 종료 여부 확인
  function checkBattleEnd(heroHp, enemyHp) {
    if (enemyHp <= 0) return 'victory';
    if (heroHp <= 0)  return 'defeat';
    return null; // 전투 계속
  }

  return {
    processAnswer,
    processEnemyAttack,
    checkBattleEnd
  };

})();