// dungeon-ui.js
// v3 - 스킬 6개 탭 UI

const DungeonUI = (() => {

  let _timerInterval    = null;
  let _enemyNormalTimer = null;
  let _enemyHeavyTimer  = null;
  let _cooldownInterval = null;

  const SKILL_COOLDOWNS = {
    ilgyeok:    800,
    yeonsoek:   1200,
    bangeo:     0,
    cheolbyeok: 3000,
    bulkkot:    1000,
    hwayeom:    5000,
  };

  function $(id) { return document.getElementById(id); }

  function showScreen(id) {
    document.querySelectorAll('.screen').forEach(s => s.classList.remove('active'));
    const target = $(id);
    if (target) target.classList.add('active');
  }

  function setAnim(id, animName) {
    const images = GameAPI.getImages();
    const el = $(id);
    if (!el) return;
    el.className = 'sprite ' + animName;
    const s = GameState.get();
    if (id === 'hero-spr') {
      el.src = images.hero[animName] || images.hero.idle;
    } else if (id === 'enemy-spr') {
      const key = s ? s.dungeon.enemyKey : 'hatchling';
      el.src = images[key][animName] || images[key].idle;
    }
  }

  function updateBars() {
    const s = GameState.get();
    if (!s) return;
    const heroHp   = Math.max(0, s.player.hp);
    const enemyHp  = Math.max(0, s.enemy.hp);
    const heroP    = Math.max(0, heroHp  / s.player.maxHp * 100);
    const enemyP   = Math.max(0, enemyHp / s.enemy.maxHp  * 100);
    const heroBar  = $('hero-bar');
    const enemyBar = $('enemy-bar');
    if (heroBar)  { heroBar.style.width  = heroP  + '%'; heroBar.classList.toggle('hp-low',  heroP  <= 25); }
    if (enemyBar) { enemyBar.style.width = enemyP + '%'; enemyBar.classList.toggle('hp-low', enemyP <= 25); }
    if ($('hero-num'))  $('hero-num').textContent  = heroHp  + ' / ' + s.player.maxHp;
    if ($('enemy-num')) $('enemy-num').textContent = enemyHp + ' / ' + s.enemy.maxHp;
  }

  function updateTimer() {
    const s = GameState.get();
    if (!s) return;
    const min = Math.floor(s.timeLeft / 60);
    const sec = s.timeLeft % 60;
    const timer = $('timer');
    if (!timer) return;
    timer.textContent = min + ':' + (sec < 10 ? '0' + sec : sec);
    timer.classList.toggle('danger', s.timeLeft <= 30);
  }

  function updateButtons() {
    const s = GameState.get();
    if (!s) return;

    const skillDefaults = {
      ilgyeok: '기본 베기', yeonsoek: '연속 공격',
      cheolbyeok: '완전 방어', bulkkot: '강한 공격', hwayeom: '초강력 필살기'
    };

    ['ilgyeok','yeonsoek','cheolbyeok','bulkkot','hwayeom'].forEach(id => {
      const btn = $('btn-' + id);
      if (!btn) return;
      const cd = s.cooldowns[id] || 0;
      if (cd > 0) {
        btn.disabled = true;
        btn.classList.add('disabled');
        const small = btn.querySelector('.skill-text small');
        if (small) small.textContent = Math.ceil(cd / 1000) + '초 후 사용 가능';
      } else {
        btn.disabled = false;
        btn.classList.remove('disabled');
        const small = btn.querySelector('.skill-text small');
        if (small) small.textContent = skillDefaults[id] || '';
      }
    });

    const isDefending = s.player.isDefending;
    ['btn-bangeo','btn-cheolbyeok'].forEach(id => {
      const btn = $(id);
      if (!btn) return;
      btn.classList.toggle('defending', isDefending);
    });
    setFighterClass('hero', 'defense-stance', isDefending);
  }

  function updateBattleHud() {
    const s = GameState.get();
    if (!s) return;
    if ($('battle-hud-magic'))   $('battle-hud-magic').textContent   = s.player.magic;
    if ($('battle-hud-stamina')) $('battle-hud-stamina').textContent = s.player.maxHp;
    if ($('battle-hud-courage')) $('battle-hud-courage').textContent = s.player.courage + '%';
  }

  function addLog(msg, type) {
    const log = $('battle-log');
    if (!log) return;
    const line = document.createElement('div');
    line.className = 'log-line ' + (type || '');
    line.textContent = msg;
    log.appendChild(line);
    while (log.children.length > 16) log.removeChild(log.firstChild);
    log.scrollTop = log.scrollHeight;
  }

  // ── 이펙트 ──

  function getTargetPoint(target) {
    const bg = $('battle-bg');
    const targetEl = target === 'enemy' ? $('enemy-spr') : $('hero-spr');
    if (!bg || !targetEl) return { x: 300, y: 260 };
    const bgRect = bg.getBoundingClientRect();
    const rect   = targetEl.getBoundingClientRect();
    return {
      x: rect.left - bgRect.left + rect.width  * 0.5,
      y: rect.top  - bgRect.top  + rect.height * 0.42
    };
  }

  function showDmgPopup(target, dmg, isCrit) {
    const bg = $('battle-bg');
    if (!bg) return;
    if (target === true)  target = 'enemy';
    if (target === false) target = 'hero';
    const point = getTargetPoint(target);
    const pop = document.createElement('div');
    pop.className = 'dmg-popup ' + (target === 'enemy' ? 'enemy-dmg' : 'hero-dmg') + (isCrit ? ' crit' : '');
    pop.innerHTML = isCrit
      ? '<span class="critical-label">CRITICAL!</span><span class="dmg-num">' + dmg + '</span>'
      : '<span class="dmg-num">' + dmg + '</span>';
    const rx = Math.floor(Math.random() * 54) - 27;
    const ry = Math.floor(Math.random() * 30) - 15;
    pop.style.left = (point.x + (target==='enemy'?64:-36) + rx) + 'px';
    pop.style.top  = (point.y + (target==='enemy'?-42:-58) + ry) + 'px';
    bg.appendChild(pop);
    setTimeout(() => pop.remove(), 1000);
  }

  function showTextPopup(target, text, type) {
    const bg = $('battle-bg');
    if (!bg) return;
    const point = getTargetPoint(target);
    const pop = document.createElement('div');
    pop.className = 'text-popup ' + (type || '');
    pop.textContent = text;
    pop.style.left = point.x + 'px';
    pop.style.top  = point.y + 'px';
    bg.appendChild(pop);
    setTimeout(() => pop.remove(), 900);
  }

  function showImpact(target, type) {
    const bg = $('battle-bg');
    if (!bg) return;
    const point = getTargetPoint(target);
    const effect = document.createElement('div');
    effect.className = 'impact-effect ' + type;
    effect.style.left = point.x + 'px';
    effect.style.top  = point.y + 'px';
    bg.appendChild(effect);
    setTimeout(() => effect.remove(), 700);
  }

  function showWhiteFlash() {
    const battle = $('s-battle');
    if (!battle) return;
    const flash = document.createElement('div');
    flash.style.cssText = 'position:fixed;inset:0;z-index:9999;pointer-events:none;background:rgba(255,255,255,0.88);animation:whiteFlashFade 0.38s ease forwards;';
    battle.appendChild(flash);
    setTimeout(() => flash.remove(), 400);
  }

  function showShockwaves(target) {
    const bg = $('battle-bg');
    if (!bg) return;
    const point = getTargetPoint(target);
    [0,100,200].forEach(delay => {
      setTimeout(() => {
        const wave = document.createElement('div');
        wave.className = 'impact-effect shockwave';
        wave.style.left = point.x + 'px';
        wave.style.top  = point.y + 'px';
        bg.appendChild(wave);
        setTimeout(() => wave.remove(), 700);
      }, delay);
    });
  }

  function showSparks(target) {
    const bg = $('battle-bg');
    if (!bg) return;
    const point = getTargetPoint(target);
    for (let i = 0; i < 12; i++) {
      const spark = document.createElement('div');
      spark.className = 'spark-particle';
      spark.style.left = point.x + 'px';
      spark.style.top  = point.y + 'px';
      const angle = (Math.PI * 2 / 12) * i;
      const dist  = 60 + Math.random() * 60;
      spark.style.setProperty('--dx', Math.cos(angle) * dist + 'px');
      spark.style.setProperty('--dy', Math.sin(angle) * dist + 'px');
      spark.style.animationDelay = Math.random() * 80 + 'ms';
      bg.appendChild(spark);
      setTimeout(() => spark.remove(), 600);
    }
  }

  function shakeScreen(isStrong) {
    const battle = $('s-battle');
    if (!battle) return;
    battle.classList.remove('screen-shake','screen-shake-strong');
    void battle.offsetWidth;
    battle.classList.add(isStrong ? 'screen-shake-strong' : 'screen-shake');
    setTimeout(() => battle.classList.remove('screen-shake','screen-shake-strong'), isStrong ? 500 : 330);
  }

  function showHeroDamageVignette() {
    const battle = $('s-battle');
    if (!battle) return;
    battle.classList.add('hero-damaged');
    setTimeout(() => battle.classList.remove('hero-damaged'), 480);
  }

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
    const sprite  = type === 'enemy' ? $('enemy-spr') : $('hero-spr');
    if (!fighter || !sprite) return;
    fighter.classList.remove('fighter-hit');
    sprite.classList.remove('taking-hit');
    void fighter.offsetWidth;
    fighter.classList.add('fighter-hit');
    sprite.classList.add('taking-hit');
    setTimeout(() => {
      fighter.classList.remove('fighter-hit');
      sprite.classList.remove('taking-hit');
    }, 390);
  }

  function showCriticalBanner(dmg) {
    const battle = $('s-battle');
    if (!battle) return;
    const banner = document.createElement('div');
    banner.style.cssText = 'position:fixed;top:50%;left:50%;transform:translate(-50%,-50%) scale(0) rotate(-6deg);z-index:9998;pointer-events:none;display:flex;flex-direction:column;align-items:center;gap:8px;animation:critBannerIn 0.28s cubic-bezier(.18,.84,.28,1.4) forwards;';
    banner.innerHTML =
      '<div style="font-family:\'Noto Serif KR\',serif;font-size:52px;font-weight:900;color:#ff4422;letter-spacing:0.08em;text-shadow:0 0 30px rgba(255,68,34,0.9),0 0 60px rgba(255,68,34,0.5),-2px -2px 0 #fff8d0,2px -2px 0 #fff8d0;">CRITICAL!</div>' +
      '<div style="font-family:\'Noto Serif KR\',serif;font-size:80px;font-weight:900;color:#ffd875;text-shadow:0 0 24px rgba(255,68,34,0.8),0 4px 0 rgba(80,20,0,0.9),-1px -1px 0 #fff,1px -1px 0 #fff;line-height:1;">' + dmg + '</div>';
    battle.appendChild(banner);
    setTimeout(() => {
      banner.style.animation = 'critBannerOut 0.4s ease forwards';
      setTimeout(() => banner.remove(), 400);
    }, 900);
  }

  function showHeavyWarning() {
    const warning = $('heavy-warning');
    if (warning) {
      warning.classList.add('visible');
      setTimeout(() => warning.classList.remove('visible'), 2000);
    }
    addLog('⚠️ 적이 강한 공격을 준비하고 있습니다!', 'warning');
  }

  // ── 전투 시작 / 타이머 ──

  function startBattle(dungeonIdx) {
    clearTimers();
    GameState.init(dungeonIdx);
    const s = GameState.get();
    if (!s) return;
    GameState.setRunning(true);

    if ($('battle-bg')) $('battle-bg').style.backgroundImage = "url('" + s.dungeon.bg + "')";

    const titleMap = ['초급 던전','중급 던전','고급 던전'];
    if ($('enemy-lbl'))    $('enemy-lbl').textContent    = titleMap[dungeonIdx] || s.dungeon.name;
    if ($('enemy-lbl2'))   $('enemy-lbl2').textContent   = s.dungeon.name;
    if ($('stat-magic'))   $('stat-magic').textContent   = s.player.magic;
    if ($('stat-stamina')) $('stat-stamina').textContent = s.player.stamina;
    if ($('stat-courage')) $('stat-courage').textContent = s.player.courage;

    setAnim('hero-spr', 'idle');
    setAnim('enemy-spr', 'idle');
    updateBars(); updateTimer(); updateBattleHud();
    switchSkillTabInternal('atk');

    if ($('battle-log')) $('battle-log').innerHTML = '';
    addLog('⚔️ ' + s.dungeon.name + ' 등장! 전투 시작!', 'system');
    addLog('좋은 질문으로 용을 혼란에 빠뜨리세요.', 'system');

    showScreen('s-battle');
    startTimers();
  }

  function startTimers() {
    const s = GameState.get();
    if (!s) return;
    const dungeon = s.dungeon;

    _timerInterval = setInterval(() => {
      if (!s.isRunning) return;
      GameState.tickTimer(); updateTimer();
      const r = BattleEngine.checkBattleEnd(s.player.hp, s.enemy.hp, s.timeLeft);
      if (r) endBattle(r);
    }, 1000);

    _cooldownInterval = setInterval(() => {
      if (!s.isRunning) return;
      Object.keys(SKILL_COOLDOWNS).forEach(id => GameState.tickCooldown(id, 100));
      updateButtons();
    }, 100);

    _enemyNormalTimer = setInterval(() => {
      if (!s.isRunning) return;
      setAnim('enemy-spr', 'attack');
      playFighterMotion('enemy', 'enemy-lunge', 480);
      setTimeout(() => {
        if (!s.isRunning) return;
        const result = BattleEngine.enemyNormalAttack(
          dungeon.enemy.normalAtk,
          s.player.isDefending,
          s.player.defenseMode
        );
        GameState.damagePlayer(result.damage);
        if (result.blocked) {
          showImpact('hero','shield'); showTextPopup('hero','BLOCK','block');
          addLog('🛡️ 방어 성공! ' + result.damage + ' 데미지만 받았습니다.', 'defend');
        } else {
          showImpact('hero','burst'); showDmgPopup('hero', result.damage, false);
          playHitEffect('hero'); showHeroDamageVignette(); shakeScreen(false);
          addLog('🐉 ' + s.dungeon.name + ' 의 공격! ' + result.damage + ' 데미지!', 'enemy');
        }
        updateBars();
        setTimeout(() => setAnim('enemy-spr','idle'), 260);
        const r2 = BattleEngine.checkBattleEnd(s.player.hp, s.enemy.hp, s.timeLeft);
        if (r2) endBattle(r2);
      }, 260);
    }, dungeon.enemy.normalAtkInterval);

    _enemyHeavyTimer = setInterval(() => {
      if (!s.isRunning) return;
      showHeavyWarning();
      setTimeout(() => {
        if (!s.isRunning) return;
        setAnim('enemy-spr','attack');
        playFighterMotion('enemy','enemy-lunge',520);
        setTimeout(() => {
          if (!s.isRunning) return;
          const result = BattleEngine.enemyHeavyAttack(
            dungeon.enemy.heavyAtk,
            s.player.isDefending,
            s.player.defenseMode
          );
          GameState.damagePlayer(result.damage);
          if (result.blocked) {
            showImpact('hero','shield'); showTextPopup('hero','BLOCK','block');
            addLog('🛡️ 강공격 방어 성공! ' + result.damage + ' 데미지만 받았습니다.', 'defend');
          } else {
            showWhiteFlash(); showImpact('hero','burst'); showShockwaves('hero');
            showDmgPopup('hero', result.damage, true); playHitEffect('hero');
            showHeroDamageVignette(); shakeScreen(true);
            addLog('💢 ' + s.dungeon.name + ' 의 강한 공격!! ' + result.damage + ' 데미지!', 'enemy');
          }
          updateBars();
          setTimeout(() => setAnim('enemy-spr','idle'), 260);
          const r2 = BattleEngine.checkBattleEnd(s.player.hp, s.enemy.hp, s.timeLeft);
          if (r2) endBattle(r2);
        }, 300);
      }, 2000);
    }, dungeon.enemy.heavyAtkInterval);
  }

  function clearTimers() {
    clearInterval(_timerInterval); clearInterval(_enemyNormalTimer);
    clearInterval(_enemyHeavyTimer); clearInterval(_cooldownInterval);
    _timerInterval = _enemyNormalTimer = _enemyHeavyTimer = _cooldownInterval = null;
  }

  // ── 스킬 사용 ──

  function useSkill(skillName) {
    const s = GameState.get();
    if (!s || !s.isRunning) return;
    if ((s.cooldowns[skillName] || 0) > 0) return;

    if (skillName === 'bangeo' || skillName === 'cheolbyeok') {
      _doDefendSkill(skillName, s); return;
    }
    _doAttackSkill(skillName, s);
  }

  function _doDefendSkill(skillName, s) {
    const cd = SKILL_COOLDOWNS[skillName] || 0;
    if (cd > 0) GameState.setCooldown(skillName, cd);
    // 방어와 철벽을 구분해서 저장
    GameState.setDefending(true, skillName === 'cheolbyeok' ? 'ironWall' : 'guard');
    updateButtons();
    if (skillName === 'cheolbyeok') {
      showImpact('hero','shield'); showImpact('hero','shield');
      showTextPopup('hero','철벽!','block');
      addLog('🔒 철벽! 완전 방어 자세!', 'defend');
    } else {
      showImpact('hero','shield');
      showTextPopup('hero','GUARD','block');
      addLog('🛡️ 방어 자세!', 'defend');
    }
  }

  function _doAttackSkill(skillName, s) {
    const cd = SKILL_COOLDOWNS[skillName] || 0;
    if (cd > 0) GameState.setCooldown(skillName, cd);

    if (skillName === 'hwayeom') { _doHwayeom(s); return; }
    if (skillName === 'bulkkot') { _doBulkkot(s); return; }

    // 일격은 기존처럼 한 번만 공격 모션을 보여줌
    // 연속 베기는 각 타격 순간마다 따로 모션을 보여줄 예정
    if (skillName === 'ilgyeok') {
      setAnim('hero-spr','attack');
      playFighterMotion('hero','hero-lunge',470);
    }

    setTimeout(() => {
      if (!s.isRunning) return;
      const result = skillName === 'ilgyeok'
        ? BattleEngine.skillIlgyeok(s.player.magic)
        : BattleEngine.skillYeonsoek(s.player.magic);

      // 연속 베기는 실제 2타로 나눠서 보여줌
      if (skillName === 'yeonsoek' && result.hits) {
        result.hits.forEach((hit, index) => {
          setTimeout(() => {
            if (!s.isRunning) return;

            // 연속 베기는 1타와 2타의 모션을 다르게 보여줌
            setAnim('hero-spr','attack');

            if (index === 0) {
              // 1타: 기존처럼 왼쪽에서 오른쪽으로 베는 기본 모션
              playFighterMotion('hero','hero-lunge',260);
            } else {
              // 2타: 검 휘두르는 방향이 반대로 보이도록 만든 전용 모션
              playFighterMotion('hero','hero-combo-second',640);
            }

            GameState.damageEnemy(hit.damage);

            if (hit.isCrit) {
            showWhiteFlash();
            showImpact('enemy', 'burst');
            showSparks('enemy');
          } else {
            showImpact('enemy', index === 1 ? 'burst' : 'slash');
          }

            showDmgPopup('enemy', hit.damage, hit.isCrit);
            playHitEffect('enemy');
            shakeScreen(hit.isCrit);

            // 로그는 나중에 다시 다듬을 예정이라 지금은 최소만 표시
            addLog('연속 베기 ' + (index + 1) + '타: ' + hit.damage + ' 피해', hit.isCrit ? 'crit' : 'player');

            updateBars();

            const be = BattleEngine.checkBattleEnd(s.player.hp, s.enemy.hp, s.timeLeft);
            if (be) endBattle(be);
          }, index * 360);
        });
      } else {
        GameState.damageEnemy(result.damage);

        if (result.isCrit) {
          showWhiteFlash();
          showImpact('enemy', 'burst');
          showShockwaves('enemy');
          showSparks('enemy');
        } else {
          showImpact('enemy', 'slash');
        }

        showDmgPopup('enemy', result.damage, result.isCrit);
        playHitEffect('enemy');
        shakeScreen(result.isCrit);

        // 로그는 나중에 다시 다듬을 예정이라 지금은 최소만 표시
        addLog('일격: ' + result.damage + ' 피해', result.isCrit ? 'crit' : 'player');

        updateBars();

        const be = BattleEngine.checkBattleEnd(s.player.hp, s.enemy.hp, s.timeLeft);
        if (be) endBattle(be);
      }
    }, 230);

    // 일격과 연속 베기의 공격 종료 타이밍을 다르게 처리
    setTimeout(() => setAnim('hero-spr','idle'), skillName === 'yeonsoek' ? 880 : 540);
  }

  function _doBulkkot(s) {
    const result = BattleEngine.skillBulkkot(s.player.magic);
    const slots = ['...숨을 죽이고','...모든 걸 걸어','...지금 이 순간'];
    const heroFighter = getFighter('hero');
    if (heroFighter) heroFighter.classList.add('charging');
    const bg = $('battle-bg');
    if (bg) { bg.style.transition='filter 0.4s ease'; bg.style.filter='brightness(0.45)'; }
    [0,380,760].forEach((d,i) => setTimeout(() => showTextPopup('hero',slots[i],'charging-slot'), d));
    setTimeout(() => {
      if (heroFighter) heroFighter.classList.remove('charging');
      if (bg) { bg.style.filter='brightness(1)'; bg.style.transition='filter 0.2s ease'; }
      setAnim('hero-spr','attack'); playFighterMotion('hero','hero-lunge',520); showWhiteFlash();
      setTimeout(() => {
        if (!s.isRunning) return;
        GameState.damageEnemy(result.damage);
        showImpact('enemy','burst'); showShockwaves('enemy'); showSparks('enemy');
        showCriticalBanner(result.damage); playHitEffect('enemy'); shakeScreen(true);
        addLog('🔥 불꽃베기 CRITICAL!! ' + result.damage + ' 데미지!', 'crit');
        updateBars();
        const be = BattleEngine.checkBattleEnd(s.player.hp, s.enemy.hp, s.timeLeft);
        if (be) endBattle(be);
      }, 260);
      setTimeout(() => setAnim('hero-spr','idle'), 580);
    }, 1200);
  }

  function _doHwayeom(s) {
    const result = BattleEngine.skillHwayeom(s.player.magic);
    const slots = ['운명을 건다...','모든 걸 담아...','이 한 방에...'];
    const heroFighter = getFighter('hero');
    if (heroFighter) heroFighter.classList.add('charging');
    const bg = $('battle-bg');
    if (bg) { bg.style.transition='filter 0.5s ease'; bg.style.filter='brightness(0.3)'; }
    [0,380,760].forEach((d,i) => setTimeout(() => showTextPopup('hero',slots[i],'charging-slot'), d));
    setTimeout(() => showWhiteFlash(), 100);
    setTimeout(() => {
      if (heroFighter) heroFighter.classList.remove('charging');
      if (bg) { bg.style.filter='brightness(1)'; bg.style.transition='filter 0.2s ease'; }
      setAnim('hero-spr','attack'); playFighterMotion('hero','hero-lunge',520); showWhiteFlash();
      setTimeout(() => {
        if (!s.isRunning) return;
        GameState.damageEnemy(result.damage);
        showImpact('enemy','burst'); showImpact('enemy','burst');
        showShockwaves('enemy'); showSparks('enemy');
        showCriticalBanner(result.damage); playHitEffect('enemy'); shakeScreen(true);
        addLog('💥 화염폭발 CRITICAL!! 용이 크게 흔들립니다. ' + result.damage + ' 데미지!', 'crit');
        updateBars();
        const be = BattleEngine.checkBattleEnd(s.player.hp, s.enemy.hp, s.timeLeft);
        if (be) endBattle(be);
      }, 260);
      setTimeout(() => setAnim('hero-spr','idle'), 580);
    }, 1200);
  }

  function stopDefend() {
    const s = GameState.get();
    if (!s) return;
    GameState.setDefending(false);
    updateButtons();
  }

  function switchSkillTabInternal(tab) {
    document.querySelectorAll('.skill-slots').forEach(el => el.classList.add('hidden'));
    document.querySelectorAll('.skill-tab').forEach(el => el.classList.remove('active'));
    const slot = document.getElementById('slot-' + tab);
    const tabBtn = document.querySelector('.' + tab + '-tab');
    if (slot) slot.classList.remove('hidden');
    if (tabBtn) tabBtn.classList.add('active');
  }

  // ── 전투 종료 / 결과 ──

  function endBattle(result) {
    const s = GameState.get();
    if (!s || !s.isRunning) return;
    GameState.setRunning(false); clearTimers();
    if (result === 'victory') {
      setAnim('enemy-spr','dead'); showTextPopup('enemy','VICTORY','block');
      addLog('🎉 승리! 지식을 되찾았습니다!', 'system'); showResult(true, false);
    } else if (result === 'defeat') {
      setAnim('hero-spr','dead');
      addLog('💀 용사가 쓰러졌습니다...', 'system'); showResult(false, false);
    } else if (result === 'timeout') {
      addLog('⏰ 시간 초과! 패배...', 'system'); showResult(false, true);
    }
  }

  function showResult(isWin, isTimeout) {
    setTimeout(() => {
      const s = GameState.get();
      if (!s) return;
      if (isWin) {
        $('r-emoji').textContent = '🏆';
        $('r-title').textContent = s.dungeon.name + ' 처치 성공!';
        $('r-sub').textContent   = '세계 지식 창고의 지식을 되찾았습니다!';
        $('r-rewards').innerHTML =
          '<div class="reward-chip-result">✨ 마법력 +' + s.player.magic + '</div>' +
          '<div class="reward-chip-result">💪 체력 +' + s.player.stamina + '</div>' +
          '<div class="reward-chip-result">🏅 던전 클리어!</div>';
      } else {
        $('r-emoji').textContent = isTimeout ? '⏰' : '💀';
        $('r-title').textContent = isTimeout ? '시간 초과!' : '패배...';
        $('r-sub').textContent   = '더 많이 책을 읽고 능력치를 올려보세요!';
        $('r-rewards').innerHTML = '<div class="reward-chip-result">다시 도전하면 더 강해질 거예요 💪</div>';
      }
      showScreen('s-result');
    }, 1200);
  }

  function goToMap() { clearTimers(); showScreen('s-map'); }

  return {
    startBattle, useSkill, stopDefend, goToMap, showScreen,
    switchSkillTab: switchSkillTabInternal,
    playerAttack:      () => useSkill('ilgyeok'),
    playerHeavyAttack: () => useSkill('bulkkot'),
    startDefend:       () => useSkill('bangeo'),
  };

})();

function goToMap()           { DungeonUI.goToMap(); }
function goToBattle(idx)     { DungeonUI.startBattle(idx); }
function playerAttack()      { DungeonUI.playerAttack(); }
function playerHeavyAttack() { DungeonUI.playerHeavyAttack(); }
function startDefend()       { DungeonUI.startDefend(); }
function stopDefend()        { DungeonUI.stopDefend(); }
function useSkill(name)      { DungeonUI.useSkill(name); }
function switchSkillTab(tab) { DungeonUI.switchSkillTab(tab); }