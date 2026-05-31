// dungeon-ui.js
// 화면 제어 담당 (DOM 조작은 이 파일에서만)
// 전투 화면 타격감 강화를 위해 데미지 팝업, 충격파, 돌진 모션, 피격 흔들림을 이 파일에서 처리합니다.

const DungeonUI = (() => {

  // 인터벌 ID 저장
  let _timerInterval    = null;
  let _enemyNormalTimer = null;
  let _enemyHeavyTimer  = null;
  let _cooldownInterval = null;

  // ─── 화면 전환 ───
  function showScreen(id) {
    document.querySelectorAll('.screen').forEach(s => s.classList.remove('active'));
    document.getElementById(id).classList.add('active');
  }

  // ─── 스프라이트 변경 ───
  function setAnim(id, animName) {
    const images = GameAPI.getImages();
    const el = document.getElementById(id);
    if (!el) return;

    // sprite 기본 클래스 + 현재 애니메이션 이름만 유지합니다.
    // 피격/돌진 같은 일시 효과는 부모 fighter에 붙여서 className 초기화와 충돌하지 않게 했습니다.
    el.className = 'sprite ' + animName;

    const s = GameState.get();
    if (id === 'hero-spr') {
      el.src = images.hero[animName] || images.hero.idle;
    } else if (id === 'enemy-spr') {
      const key = s ? s.dungeon.enemyKey : 'hatchling';
      el.src = images[key][animName] || images[key].idle;
    }
  }

  // ─── HP 바 업데이트 ───
  function updateBars() {
    const s = GameState.get();
    if (!s) return;

    const heroHp  = Math.max(0, s.player.hp);
    const enemyHp = Math.max(0, s.enemy.hp);

    const heroPercent = heroHp / s.player.maxHp * 100;
    const enemyPercent = enemyHp / s.enemy.maxHp * 100;

    const heroBar = document.getElementById('hero-bar');
    const enemyBar = document.getElementById('enemy-bar');

    heroBar.style.width = heroPercent + '%';
    enemyBar.style.width = enemyPercent + '%';

    heroBar.classList.toggle('hp-low', heroPercent <= 25);
    enemyBar.classList.toggle('hp-low', enemyPercent <= 25);

    document.getElementById('hero-num').textContent =
      heroHp + ' / ' + s.player.maxHp;

    document.getElementById('enemy-num').textContent =
      enemyHp + ' / ' + s.enemy.maxHp;
  }

  // ─── 타이머 표시 ───
  function updateTimer() {
    const s = GameState.get();
    if (!s) return;

    const min = Math.floor(s.timeLeft / 60);
    const sec = s.timeLeft % 60;
    const timer = document.getElementById('timer');

    timer.textContent = min + ':' + (sec < 10 ? '0' + sec : sec);
    timer.classList.toggle('danger', s.timeLeft <= 30);
  }

  // ─── 스킬 버튼 HTML 생성 ───
  function makeSkillButtonHTML(key, icon, title, desc) {
    return (
      '<span class="skill-key">' + key + '</span>' +
      '<span class="skill-icon">' + icon + '</span>' +
      '<span class="skill-text">' +
        '<strong>' + title + '</strong>' +
        '<small>' + desc + '</small>' +
      '</span>'
    );
  }

  // ─── 쿨타임 버튼 업데이트 ───
  function updateButtons() {
    const s = GameState.get();
    if (!s) return;

    const attackBtn = document.getElementById('btn-attack');
    const heavyBtn = document.getElementById('btn-heavy');
    const defendBtn = document.getElementById('btn-defend');

    if (attackBtn) {
      attackBtn.innerHTML = makeSkillButtonHTML('1', '⚔', '질문 공격', '기본 피해를 줍니다');
    }

    // 강한 공격 쿨타임
    if (s.cooldowns.heavy > 0) {
      heavyBtn.disabled = true;
      heavyBtn.innerHTML = makeSkillButtonHTML(
        '3',
        '💥',
        '깊은 질문',
        Math.ceil(s.cooldowns.heavy / 1000) + '초 후 사용 가능'
      );
    } else {
      heavyBtn.disabled = false;
      heavyBtn.innerHTML = makeSkillButtonHTML('3', '💥', '깊은 질문', '높은 피해와 크리티컬');
    }

    // 방어 버튼 상태
    if (s.player.isDefending) {
      defendBtn.classList.add('defending');
      defendBtn.innerHTML = makeSkillButtonHTML('2', '🛡', '방어 중', '피해를 크게 줄입니다');
      setFighterClass('hero', 'defense-stance', true);
    } else {
      defendBtn.classList.remove('defending');
      defendBtn.innerHTML = makeSkillButtonHTML('2', '🛡', '집중 방어', '받는 피해를 줄입니다');
      setFighterClass('hero', 'defense-stance', false);
    }
  }

  // ─── 전투 로그 ───
  function addLog(msg, type) {
    const log = document.getElementById('battle-log');
    if (!log) return;

    const line = document.createElement('div');
    line.className = 'log-line ' + (type || '');
    line.textContent = msg;
    log.appendChild(line);

    while (log.children.length > 16) {
      log.removeChild(log.firstChild);
    }
    log.scrollTop = log.scrollHeight;
  }

  // ─── 대상 스프라이트의 화면 내 중심 좌표 계산 ───
  function getTargetPoint(target) {
    const bg = document.getElementById('battle-bg');
    const targetEl = target === 'enemy'
      ? document.getElementById('enemy-spr')
      : document.getElementById('hero-spr');

    const bgRect = bg.getBoundingClientRect();
    const rect = targetEl.getBoundingClientRect();

    return {
      x: rect.left - bgRect.left + rect.width * 0.5,
      y: rect.top - bgRect.top + rect.height * 0.42,
    };
  }

  // ─── 데미지 팝업 ───
  // target: 'enemy'면 적에게 뜨고, 'hero'면 용사에게 뜹니다.
  function showDmgPopup(target, dmg, isCrit) {
    const bg = document.getElementById('battle-bg');
    const point = getTargetPoint(target);

    const pop = document.createElement('div');
    pop.className =
      'dmg-popup ' +
      (target === 'enemy' ? 'enemy-dmg' : 'hero-dmg') +
      (isCrit ? ' crit' : '');

    if (isCrit) {
      pop.innerHTML =
        '<span class="critical-label">CRITICAL!</span>' +
        '<span class="dmg-num">' + dmg + '</span>';
    } else {
      pop.innerHTML = '<span class="dmg-num">' + dmg + '</span>';
    }

    // 숫자가 항상 같은 위치에 뜨면 밋밋해서 약간 랜덤하게 흩뿌립니다.
    const randomX = Math.floor(Math.random() * 40) - 20;
    const randomY = Math.floor(Math.random() * 24) - 12;

    pop.style.left = (point.x + randomX) + 'px';
    pop.style.top = (point.y + randomY) + 'px';

    bg.appendChild(pop);
    setTimeout(() => pop.remove(), 950);
  }

  // ─── BLOCK, MISS 같은 텍스트 팝업 ───
  function showTextPopup(target, text, type) {
    const bg = document.getElementById('battle-bg');
    const point = getTargetPoint(target);

    const pop = document.createElement('div');
    pop.className = 'text-popup ' + (type || '');
    pop.textContent = text;
    pop.style.left = point.x + 'px';
    pop.style.top = point.y + 'px';

    bg.appendChild(pop);
    setTimeout(() => pop.remove(), 850);
  }

  // ─── 충격파 / 검기 / 방어막 이펙트 생성 ───
  function showImpact(target, type) {
    const bg = document.getElementById('battle-bg');
    const point = getTargetPoint(target);

    const effect = document.createElement('div');
    effect.className = 'impact-effect ' + type;
    effect.style.left = point.x + 'px';
    effect.style.top = point.y + 'px';

    bg.appendChild(effect);
    setTimeout(() => effect.remove(), 650);
  }

  // ─── 화면 흔들림 ───
  function shakeScreen(isStrong) {
    const battle = document.getElementById('s-battle');
    battle.classList.remove('screen-shake');

    // 같은 클래스를 연속으로 붙여도 애니메이션이 다시 실행되도록 reflow를 강제로 발생시킵니다.
    void battle.offsetWidth;

    battle.classList.add('screen-shake');
    setTimeout(() => battle.classList.remove('screen-shake'), isStrong ? 430 : 330);
  }

  // ─── 용사가 맞았을 때 붉은 화면 효과 ───
  function showHeroDamageVignette() {
    const battle = document.getElementById('s-battle');
    battle.classList.add('hero-damaged');
    setTimeout(() => battle.classList.remove('hero-damaged'), 460);
  }

  // ─── fighter 부모 클래스 제어 ───
  function getFighter(type) {
    return document.querySelector(type === 'enemy' ? '.fighter.right' : '.fighter.left');
  }

  function setFighterClass(type, className, enabled) {
    const fighter = getFighter(type);
    if (!fighter) return;
    fighter.classList.toggle(className, enabled);
  }

  function playFighterMotion(type, className, duration) {
    const fighter = getFighter(type);
    if (!fighter) return;

    fighter.classList.remove(className);
    void fighter.offsetWidth;
    fighter.classList.add(className);

    setTimeout(() => fighter.classList.remove(className), duration);
  }

  function playHitEffect(type) {
    const fighter = getFighter(type);
    const sprite = document.getElementById(type === 'enemy' ? 'enemy-spr' : 'hero-spr');
    if (!fighter || !sprite) return;

    fighter.classList.remove('fighter-hit');
    sprite.classList.remove('taking-hit');
    void fighter.offsetWidth;

    fighter.classList.add('fighter-hit');
    sprite.classList.add('taking-hit');

    setTimeout(() => {
      fighter.classList.remove('fighter-hit');
      sprite.classList.remove('taking-hit');
    }, 380);
  }

  // ─── 적 강공격 예고 ───
  function showHeavyWarning() {
    const warning = document.getElementById('heavy-warning');
    warning.classList.add('visible');
    addLog('⚠️ 적이 강한 공격을 준비하고 있습니다!', 'warning');
    setTimeout(() => warning.classList.remove('visible'), 2000);
  }

  // ─── 전투 시작 ───
  function startBattle(dungeonIdx) {
    clearTimers();
    GameState.init(dungeonIdx);
    const s = GameState.get();
    GameState.setRunning(true);

    document.getElementById('battle-bg').style.backgroundImage =
      "url('" + s.dungeon.bg + "')";
    document.getElementById('enemy-lbl').textContent = s.dungeon.name;
    document.getElementById('enemy-lbl2').textContent = s.dungeon.name;

    document.getElementById('stat-magic').textContent = s.player.magic;
    document.getElementById('stat-stamina').textContent = s.player.stamina;
    document.getElementById('stat-courage').textContent = s.player.courage;

    setAnim('hero-spr',  'idle');
    setAnim('enemy-spr', 'idle');
    updateBars();
    updateTimer();
    updateButtons();

    document.getElementById('battle-log').innerHTML = '';
    addLog('⚔️ ' + s.dungeon.name + ' 등장! 전투 시작!', 'system');
    addLog('좋은 질문으로 용을 혼란에 빠뜨리세요.', 'system');

    showScreen('s-battle');
    startTimers();
  }

  // ─── 타이머 시작 ───
  function startTimers() {
    const s = GameState.get();
    const dungeon = s.dungeon;

    // 1초마다 타이머 감소
    _timerInterval = setInterval(() => {
      if (!s.isRunning) return;
      GameState.tickTimer();
      updateTimer();
      const result = BattleEngine.checkBattleEnd(
        s.player.hp, s.enemy.hp, s.timeLeft
      );
      if (result) endBattle(result);
    }, 1000);

    // 쿨타임 감소 (100ms마다)
    _cooldownInterval = setInterval(() => {
      if (!s.isRunning) return;
      GameState.tickCooldown('normal', 100);
      GameState.tickCooldown('heavy', 100);
      updateButtons();
    }, 100);

    // 적 일반 공격
    _enemyNormalTimer = setInterval(() => {
      if (!s.isRunning) return;

      setAnim('enemy-spr', 'attack');
      playFighterMotion('enemy', 'enemy-lunge', 480);

      // 적이 달려드는 타이밍에 맞춰 실제 피해와 이펙트를 조금 늦게 발생시킵니다.
      setTimeout(() => {
        if (!s.isRunning) return;

        const result = BattleEngine.enemyNormalAttack(
          dungeon.enemy.normalAtk,
          s.player.isDefending
        );

        GameState.damagePlayer(result.damage);

        if (result.blocked) {
          showImpact('hero', 'shield');
          showTextPopup('hero', 'BLOCK', 'block');
          addLog('🛡️ 방어 성공! ' + result.damage + ' 데미지만 받았습니다.', 'defend');
        } else {
          showImpact('hero', 'burst');
          showDmgPopup('hero', result.damage, false);
          playHitEffect('hero');
          showHeroDamageVignette();
          shakeScreen(false);
          addLog('🐉 ' + s.dungeon.name + ' 의 공격! ' + result.damage + ' 데미지!', 'enemy');
        }

        updateBars();
        setTimeout(() => setAnim('enemy-spr', 'idle'), 260);

        const result2 = BattleEngine.checkBattleEnd(
          s.player.hp, s.enemy.hp, s.timeLeft
        );
        if (result2) endBattle(result2);
      }, 260);
    }, dungeon.enemy.normalAtkInterval);

    // 적 강한 공격 (예고 2초 전)
    _enemyHeavyTimer = setInterval(() => {
      if (!s.isRunning) return;
      showHeavyWarning();

      setTimeout(() => {
        if (!s.isRunning) return;

        setAnim('enemy-spr', 'attack');
        playFighterMotion('enemy', 'enemy-lunge', 520);

        setTimeout(() => {
          if (!s.isRunning) return;

          const result = BattleEngine.enemyHeavyAttack(
            dungeon.enemy.heavyAtk,
            s.player.isDefending
          );

          GameState.damagePlayer(result.damage);

          if (result.blocked) {
            showImpact('hero', 'shield');
            showTextPopup('hero', 'BLOCK', 'block');
            addLog('🛡️ 강공격 방어 성공! ' + result.damage + ' 데미지만 받았습니다.', 'defend');
          } else {
            showImpact('hero', 'burst');
            showDmgPopup('hero', result.damage, true);
            playHitEffect('hero');
            showHeroDamageVignette();
            shakeScreen(true);
            addLog('💢 ' + s.dungeon.name + ' 의 강한 공격!! ' + result.damage + ' 데미지!', 'enemy');
          }

          updateBars();
          setTimeout(() => setAnim('enemy-spr', 'idle'), 260);

          const result2 = BattleEngine.checkBattleEnd(
            s.player.hp, s.enemy.hp, s.timeLeft
          );
          if (result2) endBattle(result2);
        }, 300);
      }, 2000);
    }, dungeon.enemy.heavyAtkInterval);
  }

  // ─── 타이머 정리 ───
  function clearTimers() {
    clearInterval(_timerInterval);
    clearInterval(_enemyNormalTimer);
    clearInterval(_enemyHeavyTimer);
    clearInterval(_cooldownInterval);

    _timerInterval = null;
    _enemyNormalTimer = null;
    _enemyHeavyTimer = null;
    _cooldownInterval = null;
  }

  // ─── 플레이어 일반 공격 ───
  function playerAttack() {
    const s = GameState.get();
    if (!s || !s.isRunning || s.cooldowns.normal > 0) return;

    const result = BattleEngine.playerNormalAttack(s.player.magic);
    GameState.setCooldown('normal', 850);

    setAnim('hero-spr', 'attack');
    playFighterMotion('hero', 'hero-lunge', 460);

    // 돌진 후 검기가 닿는 타이밍에 피해를 적용합니다.
    setTimeout(() => {
      if (!s.isRunning) return;

      GameState.damageEnemy(result.damage);
      showImpact('enemy', result.isCrit ? 'burst' : 'slash');
      showDmgPopup('enemy', result.damage, result.isCrit);
      playHitEffect('enemy');
      shakeScreen(result.isCrit);

      if (result.isCrit) {
        addLog('✨ 크리티컬! 질문이 깊게 꽂혔습니다. ' + result.damage + ' 데미지!', 'crit');
      } else {
        addLog('⚔️ 질문 공격! ' + result.damage + ' 데미지!', 'player');
      }

      updateBars();

      const battleEnd = BattleEngine.checkBattleEnd(
        s.player.hp, s.enemy.hp, s.timeLeft
      );
      if (battleEnd) endBattle(battleEnd);
    }, 230);

    setTimeout(() => setAnim('hero-spr', 'idle'), 520);
  }

  // ─── 플레이어 강한 공격 ───
  function playerHeavyAttack() {
    const s = GameState.get();
    if (!s || !s.isRunning || s.cooldowns.heavy > 0) return;

    const cooldown = PlayerStats.calcHeavyCooldown(s.player.courage);
    const result = BattleEngine.playerHeavyAttack(s.player.magic);
    GameState.setCooldown('heavy', cooldown);

    setAnim('hero-spr', 'attack');
    playFighterMotion('hero', 'hero-lunge', 500);

    setTimeout(() => {
      if (!s.isRunning) return;

      GameState.damageEnemy(result.damage);
      showImpact('enemy', 'burst');
      showDmgPopup('enemy', result.damage, result.isCrit);
      playHitEffect('enemy');
      shakeScreen(true);

      if (result.isCrit) {
        addLog('💥 깊은 질문 크리티컬!! 용이 크게 흔들립니다. ' + result.damage + ' 데미지!', 'crit');
      } else {
        addLog('💥 깊은 질문! ' + result.damage + ' 데미지!', 'player');
      }

      updateBars();

      const battleEnd = BattleEngine.checkBattleEnd(
        s.player.hp, s.enemy.hp, s.timeLeft
      );
      if (battleEnd) endBattle(battleEnd);
    }, 260);

    setTimeout(() => setAnim('hero-spr', 'idle'), 560);
  }

  // ─── 방어 ───
  function startDefend() {
    const s = GameState.get();
    if (!s || !s.isRunning) return;

    GameState.setDefending(true);
    updateButtons();
    showImpact('hero', 'shield');
    showTextPopup('hero', 'GUARD', 'block');
    addLog('🛡️ 집중 방어 자세!', 'defend');
  }

  function stopDefend() {
    const s = GameState.get();
    if (!s) return;

    GameState.setDefending(false);
    updateButtons();
  }

  // ─── 전투 종료 ───
  function endBattle(result) {
    const s = GameState.get();
    if (!s || !s.isRunning) return;

    GameState.setRunning(false);
    clearTimers();

    if (result === 'victory') {
      setAnim('enemy-spr', 'dead');
      showTextPopup('enemy', 'VICTORY', 'block');
      addLog('🎉 승리! 지식을 되찾았습니다!', 'system');
      showResult(true, false);
    } else if (result === 'defeat') {
      setAnim('hero-spr', 'dead');
      addLog('💀 용사가 쓰러졌습니다...', 'system');
      showResult(false, false);
    } else if (result === 'timeout') {
      addLog('⏰ 시간 초과! 패배...', 'system');
      showResult(false, true);
    }
  }

  // ─── 결과 화면 ───
  function showResult(isWin, isTimeout) {
    setTimeout(() => {
      const s = GameState.get();

      if (isWin) {
        document.getElementById('r-emoji').textContent = '🏆';
        document.getElementById('r-title').textContent = s.dungeon.name + ' 처치 성공!';
        document.getElementById('r-sub').textContent = '세계 지식 창고의 지식을 되찾았습니다!';
        document.getElementById('r-rewards').innerHTML =
          '<div class="reward-chip">✨ 마법력 +' + s.player.magic + '</div>' +
          '<div class="reward-chip">💪 체력 +' + s.player.stamina + '</div>' +
          '<div class="reward-chip">🏅 던전 클리어!</div>';
      } else {
        document.getElementById('r-emoji').textContent = isTimeout ? '⏰' : '💀';
        document.getElementById('r-title').textContent = isTimeout ? '시간 초과!' : '패배...';
        document.getElementById('r-sub').textContent = '더 많이 책을 읽고 능력치를 올려보세요!';
        document.getElementById('r-rewards').innerHTML =
          '<div class="reward-chip">다시 도전하면 더 강해질 거예요 💪</div>';
      }

      showScreen('s-result');
    }, 1200);
  }

  // ─── 던전 선택 화면 ───
  function goToMap() {
    clearTimers();
    showScreen('s-map');
  }

  return {
    startBattle,
    playerAttack,
    playerHeavyAttack,
    startDefend,
    stopDefend,
    goToMap,
    showScreen,
  };

})();

// index.html onclick에서 직접 호출
function goToMap()             { DungeonUI.goToMap(); }
function goToBattle(idx)       { DungeonUI.startBattle(idx); }
function playerAttack()        { DungeonUI.playerAttack(); }
function playerHeavyAttack()   { DungeonUI.playerHeavyAttack(); }
function startDefend()         { DungeonUI.startDefend(); }
function stopDefend()          { DungeonUI.stopDefend(); }