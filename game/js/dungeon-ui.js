// dungeon-ui.js
// 화면 제어 담당 (DOM 조작은 이 파일에서만)
// 기존 game.js의 화면 관련 함수들을 여기로 이전

const DungeonUI = (() => {

  // 화면 전환
  function showScreen(id) {
    document.querySelectorAll('.screen').forEach(function(s) {
      s.classList.remove('active');
    });
    document.getElementById(id).classList.add('active');
  }

  // 스프라이트 이미지 변경
  function setAnim(id, animName) {
    const images = GameAPI.getImages();
    const el = document.getElementById(id);
    el.className = 'sprite ' + animName;

    if (id === 'hero-spr') {
      el.src = images.hero[animName] || images.hero.idle;
    } else if (id === 'enemy-spr') {
      const state = GameState.get();
      const key = state ? state.dungeon.enemyKey : 'enemy1';
      el.src = images[key][animName] || images[key].idle;
    }
  }

  // HP 바 업데이트
  function updateBars() {
    const s = GameState.get();
    if (!s) return;

    const heroHp  = Math.max(0, s.heroHp);
    const enemyHp = Math.max(0, s.enemyHp);

    document.getElementById('hero-bar').style.width  = (heroHp  / s.heroMax  * 100) + '%';
    document.getElementById('hero-num').textContent  = heroHp  + '/' + s.heroMax;
    document.getElementById('enemy-bar').style.width = (enemyHp / s.enemyMax * 100) + '%';
    document.getElementById('enemy-num').textContent = enemyHp + '/' + s.enemyMax;
  }

  // 능력치 표시 업데이트
  function updateStats() {
    const s = GameState.get();
    if (!s) return;

    document.getElementById('sv-magic').textContent  = s.magic;
    document.getElementById('sv-hp').textContent     = Math.max(0, s.heroHp);
    document.getElementById('sv-wisdom').textContent = s.wisdom;
  }

  // 전투 로그 텍스트 변경
  function setLog(text) {
    document.getElementById('log-text').textContent = text;
  }

  // 데미지 팝업 표시
  function showDmgPopup(isEnemy, dmg) {
    const bg  = document.getElementById('battle-bg');

    const pop = document.createElement('div');
    pop.className = 'dmg-popup ' + (isEnemy ? 'enemy-dmg' : 'hero-dmg');
    pop.textContent = '-' + dmg;
    pop.style.cssText = isEnemy ? 'right:100px;top:20px;' : 'left:100px;top:20px;';
    bg.appendChild(pop);
    setTimeout(function() { pop.remove(); }, 900);

    const impact = document.createElement('div');
    impact.className = 'impact';
    impact.textContent = isEnemy ? '💥' : '⚡';
    impact.style.cssText = isEnemy ? 'right:60px;top:50px;' : 'left:60px;top:50px;';
    bg.appendChild(impact);
    setTimeout(function() { impact.remove(); }, 500);

    if (!isEnemy) {
      const battle = document.getElementById('s-battle');
      battle.classList.add('screen-shake');
      setTimeout(function() { battle.classList.remove('screen-shake'); }, 400);
      const battleBg = document.getElementById('battle-bg');
      battleBg.classList.add('hero-hit');
      setTimeout(function() { battleBg.classList.remove('hero-hit'); }, 400);
    }
  }

  // 질문 표시
  function showQuestion() {
    const q = GameState.getCurrentQuestion();
    if (!q) return;

    document.getElementById('q-text').textContent = 'Q. ' + q.q;
    const choicesEl = document.getElementById('choices');
    choicesEl.innerHTML = '';

    q.choices.forEach(function(text, i) {
      const btn = document.createElement('button');
      btn.className = 'c-btn';
      btn.textContent = text;
      btn.onclick = function() { handleAnswer(i); };
      choicesEl.appendChild(btn);
    });
  }

  // 정답 선택 처리
  function handleAnswer(selectedIdx) {
    const s = GameState.get();
    if (!s || s.busy) return;

    GameState.setBusy(true);

    const q    = GameState.getCurrentQuestion();
    const btns = document.querySelectorAll('.c-btn');
    btns.forEach(function(b) { b.disabled = true; });

    const result = BattleEngine.processAnswer(selectedIdx, q.answer, s.magic);

    if (result.correct) {
      btns[selectedIdx].classList.add('ok');
      GameState.applyAnswerResult(result);
      setLog('✨ 정답! ' + result.damage + ' 데미지! 마법력이 올랐어요!');
      setAnim('hero-spr', 'attack');

      setTimeout(function() {
        showDmgPopup(true, result.damage);
        setAnim('enemy-spr', 'hit');
        updateBars();
        updateStats();

        setTimeout(function() {
          setAnim('hero-spr', 'idle');
          setAnim('enemy-spr', 'idle');

          const battleEnd = BattleEngine.checkBattleEnd(s.heroHp, s.enemyHp);
          if (battleEnd === 'victory') {
            GameState.setBusy(false);
            handleVictory();
          } else {
            handleEnemyTurn();
          }
        }, 600);
      }, 300);

    } else {
      btns[selectedIdx].classList.add('no');
      btns[q.answer].classList.add('ok');
      setLog('❌ 틀렸어요! 정답을 확인하세요. 적이 반격합니다!');
      setTimeout(function() { handleEnemyTurn(); }, 800);
    }
  }

  // 적 턴 처리
  function handleEnemyTurn() {
    const s = GameState.get();
    const result = BattleEngine.processEnemyAttack(s.dungeon.atk);
    GameState.applyEnemyAttack(result);
    setAnim('enemy-spr', 'attack');

    setTimeout(function() {
      showDmgPopup(false, result.damage);
      setAnim('hero-spr', 'hit');
      updateBars();
      updateStats();

      setTimeout(function() {
        setAnim('enemy-spr', 'idle');
        setAnim('hero-spr', 'idle');

        const battleEnd = BattleEngine.checkBattleEnd(s.heroHp, s.enemyHp);
        if (battleEnd === 'defeat') {
          GameState.setBusy(false);
          handleDefeat();
        } else {
          GameState.setBusy(false);
          setLog('용사의 턴! 질문을 맞춰 공격하세요!');
          showQuestion();
        }
      }, 500);
    }, 300);
  }

  // 승리 처리
  function handleVictory() {
    const s = GameState.get();
    GameState.applyVictory();
    setAnim('enemy-spr', 'dead');
    setLog('🎉 승리! 지식을 되찾았습니다!');

    document.getElementById('r-emoji').textContent = '🏆';
    document.getElementById('r-title').textContent = s.dungeon.name + ' 처치 성공!';
    document.getElementById('r-sub').textContent   = '세계 지식 창고의 지식을 되찾았습니다!';
    document.getElementById('r-rewards').innerHTML =
      '<div class="reward-chip">✨ 마법력 +' + s.magic + '</div>' +
      '<div class="reward-chip">📚 지혜 +' + s.wisdom + '</div>' +
      '<div class="reward-chip">🏅 던전 클리어!</div>';

    setTimeout(function() { showScreen('s-result'); }, 1200);
  }

  // 패배 처리
  function handleDefeat() {
    setAnim('hero-spr', 'dead');
    setLog('💀 용사가 쓰러졌습니다...');

    document.getElementById('r-emoji').textContent   = '💀';
    document.getElementById('r-title').textContent   = '패배...';
    document.getElementById('r-sub').textContent     = '더 많이 공부하고 다시 도전하세요!';
    document.getElementById('r-rewards').innerHTML   =
      '<div class="reward-chip">다시 도전하면 더 강해질 거예요 💪</div>';

    setTimeout(function() { showScreen('s-result'); }, 1200);
  }

  // 던전 입장 (던전 선택 화면에서 호출)
  function goToBattle(idx) {
    GameState.init(idx);
    const s = GameState.get();

    document.getElementById('battle-bg').style.backgroundImage =
      "url('" + s.dungeon.bg + "')";
    document.getElementById('enemy-lbl').textContent = s.dungeon.name;

    setAnim('hero-spr',  'idle');
    setAnim('enemy-spr', 'idle');
    updateBars();
    updateStats();
    setLog('⚔️ ' + s.dungeon.name + ' 등장! 질문을 맞춰 공격하세요!');
    showScreen('s-battle');

    setTimeout(showQuestion, 600);
  }

  // 던전 선택 화면으로 돌아가기
  function goToMap() {
    showScreen('s-map');
  }

  return {
    goToBattle,
    goToMap,
    showScreen
  };

})();

// index.html의 onclick에서 직접 호출할 수 있도록 전역 함수로 노출
function goToBattle(idx) { DungeonUI.goToBattle(idx); }
function goToMap()        { DungeonUI.goToMap(); }