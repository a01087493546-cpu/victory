// player-stats.js
// 능력치 계산 담당 (순수 계산 로직만, DOM 없음)
// 시나리오 변경 시 수치만 조정하면 됨

const PlayerStats = (() => {

  /*
   * 던전 전투 능력치 연계 시스템.
   * 능력치는 전투 시작 시 1회 복사되는 소모형 자원이다(마법력→공격,
   * 체력→HP, 지혜→필살, 용기→방어). 아래 SKILL_COST_RATIO는 던전
   * 기준 능력치(T = requiredStatAvg)에 대한 비율로 각 기술의 소모량을
   * 정한다 - 던전이 어려워질수록(T가 커질수록) 기술 비용도 함께
   * 커진다.
   */
  const SKILL_COST_RATIO = {
    ilgyeok:    0.10,
    yeonsoek:   0.20,
    bangeo:     0.10,
    cheolbyeok: 0.20,
    bulkkot:    0.15,
    hwayeom:    0.30,
  };

  // 던전 기준 능력치(T) 대비 기술 소모량. 최소 1을 보장한다.
  function calcSkillCost(skillName, T) {
    const ratio = SKILL_COST_RATIO[skillName] || 0;
    return Math.max(1, Math.round((T || 0) * ratio));
  }

  // 마법력 → 기본 베기/연속 공격 데미지
  // 마법력 높을수록 데미지 증가
  function calcNormalDamage(magic) {
    return 10 + Math.floor(magic * 0.8);
  }

  // 지혜 → 강한 공격/초강력 필살기 데미지
  function calcHeavyDamage(wisdom) {
    return 25 + Math.floor(wisdom * 1.5);
  }

  // 크리티컬 확률 (%). 기본/연속 공격은 마법력, 강한 공격/필살기는
  // 지혜를 넘겨 받는다 - 담당 능력치 하나만 크리티컬에 관여한다.
  // 기준 10 → 17%, 최대 60%
  function calcCritChance(stat) {
    return Math.min(5 + stat * 1.2, 60);
  }

  // 크리티컬 발동 여부 판정
  function isCritical(stat) {
    return Math.random() * 100 < calcCritChance(stat);
  }

  /*
   * 체력(전투용 소모 자원) → 최대 HP.
   * 던전 기준 능력치(T)만큼의 체력을 가지면 그 던전의 기본 HP를 그대로
   * 받는다. T보다 높으면 비율에 따라 HP가 늘고, 낮으면 줄어든다(상한
   * 1.6배, 하한 0.5배).
   */
  const BASE_HP_BY_T = { 20: 220, 55: 320, 85: 420 };
  const HP_SCALE_MIN = 0.5;
  const HP_SCALE_BASE = 0.3;
  const HP_SCALE_COEF = 0.7;
  const HP_SCALE_MAX = 1.6;

  function calcMaxHp(stamina, T) {
    const baseHp = BASE_HP_BY_T[T] || 220;
    const ratio = T > 0 ? stamina / T : 1;
    const scaled = Math.max(HP_SCALE_MIN, Math.min(HP_SCALE_MAX, HP_SCALE_BASE + HP_SCALE_COEF * ratio));
    return Math.round(baseHp * scaled);
  }

  // 용기 → 방어(기본 막기) 시 받는 데미지 감소율 (%)
  // 기준 10 → 83%, 최대 95%
  function calcDefenseRate(courage) {
    return Math.min(80 + Math.floor(courage * 0.3), 95);
  }

  return {
    calcSkillCost,
    calcNormalDamage,
    calcHeavyDamage,
    calcCritChance,
    isCritical,
    calcMaxHp,
    calcDefenseRate,
  };

})();