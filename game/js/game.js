const IMAGES = {
  hero: {
    idle:   'images/Copilot_20260520_145541.png',
    attack: 'images/Copilot_20260520_145707.png',
    hit:    'images/Copilot_20260520_145735.png',
    dead:   'images/Copilot_20260520_145812.png'
  },
  enemy1: {
    idle:   'images/Copilot_20260520_145843.png',
    attack: 'images/Copilot_20260520_150000.png',
    hit:    'images/Copilot_20260520_150048.png',
    dead:   'images/Copilot_20260520_150131.png'
  },
  enemy2: {
    idle:   'images/Copilot_20260520_150244.png',
    attack: 'images/Copilot_20260520_150314.png',
    hit:    'images/Copilot_20260520_150350.png',
    dead:   'images/Copilot_20260520_150419.png'
  },
  enemy3: {
    idle:   'images/Copilot_20260520_150501.png',
    attack: 'images/Copilot_20260520_150551.png',
    hit:    'images/Copilot_20260520_150622.png',
    dead:   'images/Copilot_20260520_150648.png'
  }
};

const DUNGEONS = [
  {
    name: '헤츨링', enemyKey: 'enemy1',
    bg: 'images/Copilot_20260520_145314.png',
    hp: 60, atk: 8,
    questions: [
      { q: '책에서 바로 찾을 수 있는 질문은?', choices: ['주인공은 어디 살아?','왜 이 책을 썼을까?','너라면 어떻게 했을까?','이 책의 주제는?'], answer: 0 },
      { q: "'자신의 삶과 관련짓는 질문'의 예시는?", choices: ['주인공 이름은?','결말은 어떻게 됐어?','나도 비슷한 경험이 있었는데...','배경은 어디야?'], answer: 2 },
      { q: '좋은 질문의 특징은?', choices: ['짧을수록 좋다','예/아니오로만 답할 수 있다','생각을 깊게 하게 만든다','정답이 하나뿐이다'], answer: 2 }
    ]
  },
  {
    name: '용', enemyKey: 'enemy2',
    bg: 'images/Copilot_20260520_145422.png',
    hp: 100, atk: 15,
    questions: [
      { q: "책 내용에서 답을 '짐작'할 수 있는 질문은?", choices: ['주인공 나이는?','왜 그런 선택을 했을까?','책 제목이 뭐야?','몇 페이지야?'], answer: 1 },
      { q: '읽기 전 활동으로 가장 적절한 것은?', choices: ['줄거리 외우기','제목 보고 내용 상상하기','결말 먼저 읽기','모르는 단어 찾기'], answer: 1 },
      { q: '이야기책 간추리기에서 중요한 것은?', choices: ['글쓴이 정보','시간과 장소의 변화','책 가격','페이지 수'], answer: 1 }
    ]
  },
  {
    name: '나이많은 용', enemyKey: 'enemy3',
    bg: 'images/Copilot_20260520_145503.png',
    hp: 150, atk: 25,
    questions: [
      { q: '가장 깊이 있는 질문 유형은?', choices: ['책에서 찾는 질문','예/아니오 질문','삶과 연결하는 질문','사실 확인 질문'], answer: 2 },
      { q: 'AI 피드백 후에도 꼭 필요한 것은?', choices: ['다시 AI에게','교사 피드백','친구와 비교','책 다시 읽기'], answer: 1 },
      { q: '오완독 챌린지의 목적은?', choices: ['빨리 읽기','꾸준한 독서 습관 만들기','어려운 책 읽기','시험 대비'], answer: 1 }
    ]
  }
];

let state = {
  heroHp: 100, heroMax: 100,
  enemyHp: 0, enemyMax: 0,
  qIdx: 0, dungeon: null,
  magic: 10, wisdom: 10,
  wins: 0, busy: false
};

function showScreen(id) {
  document.querySelectorAll('.screen').forEach(function(s) { s.classList.remove('active'); });
  document.getElementById(id).classList.add('active');
}

function goToMap() { showScreen('s-map'); }

function goToBattle(idx) {
  var d = DUNGEONS[idx];
  var prevMagic = state.magic;
  var prevWisdom = state.wisdom;
  var prevWins = state.wins;
  state = {
    heroHp: 100, heroMax: 100,
    enemyHp: d.hp, enemyMax: d.hp,
    qIdx: 0, dungeon: d,
    magic: prevMagic || 10,
    wisdom: prevWisdom || 10,
    wins: prevWins,
    busy: false
  };
  document.getElementById('battle-bg').style.backgroundImage = "url('" + d.bg + "')";
  setAnim('hero-spr', 'idle');
  setAnim('enemy-spr', 'idle');
  updateBars();
  updateStats();
  document.getElementById('enemy-lbl').textContent = d.name;
  setLog('⚔️ ' + d.name + ' 등장! 질문을 맞춰 공격하세요!');
  showScreen('s-battle');
  setTimeout(showQuestion, 600);
}

function setAnim(id, animName) {
  var el = document.getElementById(id);
  el.className = 'sprite ' + animName;
  if (id === 'hero-spr') {
    el.src = IMAGES.hero[animName] || IMAGES.hero.idle;
  } else if (id === 'enemy-spr') {
    var key = state.dungeon ? state.dungeon.enemyKey : 'enemy1';
    el.src = IMAGES[key][animName] || IMAGES[key].idle;
  }
}

function updateBars() {
  var h = Math.max(0, state.heroHp);
  var e = Math.max(0, state.enemyHp);
  document.getElementById('hero-bar').style.width = (h / state.heroMax * 100) + '%';
  document.getElementById('hero-num').textContent = h + '/' + state.heroMax;
  document.getElementById('enemy-bar').style.width = (e / state.enemyMax * 100) + '%';
  document.getElementById('enemy-num').textContent = e + '/' + state.enemyMax;
}

function updateStats() {
  document.getElementById('sv-magic').textContent = state.magic;
  document.getElementById('sv-hp').textContent = Math.max(0, state.heroHp);
  document.getElementById('sv-wisdom').textContent = state.wisdom;
}

function setLog(text) { document.getElementById('log-text').textContent = text; }

function showDmgPopup(isEnemy, dmg) {
  var bg = document.getElementById('battle-bg');
  var pop = document.createElement('div');
  pop.className = 'dmg-popup ' + (isEnemy ? 'enemy-dmg' : 'hero-dmg');
  pop.textContent = '-' + dmg;
  pop.style.cssText = isEnemy ? 'right:100px;top:20px;' : 'left:100px;top:20px;';
  bg.appendChild(pop);
  setTimeout(function() { pop.remove(); }, 900);

  var impact = document.createElement('div');
  impact.className = 'impact';
  impact.textContent = isEnemy ? '💥' : '⚡';
  impact.style.cssText = isEnemy ? 'right:60px;top:50px;' : 'left:60px;top:50px;';
  bg.appendChild(impact);
  setTimeout(function() { impact.remove(); }, 500);

  if (!isEnemy) {
    var battle = document.getElementById('s-battle');
    battle.classList.add('screen-shake');
    setTimeout(function() { battle.classList.remove('screen-shake'); }, 400);
    var battleBg = document.getElementById('battle-bg');
    battleBg.classList.add('hero-hit');
    setTimeout(function() { battleBg.classList.remove('hero-hit'); }, 400);
  }
}

function showQuestion() {
  var q = state.dungeon.questions[state.qIdx % state.dungeon.questions.length];
  document.getElementById('q-text').textContent = 'Q. ' + q.q;
  var choicesEl = document.getElementById('choices');
  choicesEl.innerHTML = '';
  q.choices.forEach(function(text, i) {
    var btn = document.createElement('button');
    btn.className = 'c-btn';
    btn.textContent = text;
    btn.onclick = function() { answer(i); };
    choicesEl.appendChild(btn);
  });
}

function answer(idx) {
  if (state.busy) return;
  state.busy = true;

  var q = state.dungeon.questions[state.qIdx % state.dungeon.questions.length];
  var btns = document.querySelectorAll('.c-btn');
  btns.forEach(function(b) { b.disabled = true; });

  if (idx === q.answer) {
    btns[idx].classList.add('ok');
    var dmg = 15 + Math.floor(state.magic * 0.5);
    state.enemyHp -= dmg;
    state.magic += 2;
    state.wisdom += 1;
    setLog('✨ 정답! ' + dmg + ' 데미지! 마법력이 올랐어요!');
    setAnim('hero-spr', 'attack');

    setTimeout(function() {
      showDmgPopup(true, dmg);
      setAnim('enemy-spr', 'hit');
      updateBars();
      updateStats();

      setTimeout(function() {
        setAnim('hero-spr', 'idle');
        setAnim('enemy-spr', 'idle');

        if (state.enemyHp <= 0) {
          // 승리 - busy 해제하고 바로 victory 호출
          state.busy = false;
          victory();
        } else {
          state.qIdx++;
          enemyTurn();
        }
      }, 600);
    }, 300);

  } else {
    btns[idx].classList.add('no');
    btns[q.answer].classList.add('ok');
    setLog('❌ 틀렸어요! 정답을 확인하세요. 적이 반격합니다!');
    setTimeout(function() { enemyTurn(); }, 800);
  }
}

function enemyTurn() {
  var dmg = state.dungeon.atk;
  state.heroHp -= dmg;
  setAnim('enemy-spr', 'attack');

  setTimeout(function() {
    showDmgPopup(false, dmg);
    setAnim('hero-spr', 'hit');
    updateBars();
    updateStats();

    setTimeout(function() {
      setAnim('enemy-spr', 'idle');
      setAnim('hero-spr', 'idle');

      if (state.heroHp <= 0) {
        state.busy = false;
        defeat();
      } else {
        state.busy = false;
        setLog('용사의 턴! 질문을 맞춰 공격하세요!');
        showQuestion();
      }
    }, 500);
  }, 300);
}

function victory() {
  state.wins++;
  setAnim('enemy-spr', 'dead');
  setLog('🎉 승리! 지식을 되찾았습니다!');
  document.getElementById('r-emoji').textContent = '🏆';
  document.getElementById('r-title').textContent = state.dungeon.name + ' 처치 성공!';
  document.getElementById('r-sub').textContent = '세계 지식 창고의 지식을 되찾았습니다!';
  document.getElementById('r-rewards').innerHTML =
    '<div class="reward-chip">✨ 마법력 +' + state.magic + '</div>' +
    '<div class="reward-chip">📚 지혜 +' + state.wisdom + '</div>' +
    '<div class="reward-chip">🏅 던전 클리어!</div>';
  setTimeout(function() { showScreen('s-result'); }, 1200);
}

function defeat() {
  setAnim('hero-spr', 'dead');
  setLog('💀 용사가 쓰러졌습니다...');
  document.getElementById('r-emoji').textContent = '💀';
  document.getElementById('r-title').textContent = '패배...';
  document.getElementById('r-sub').textContent = '더 많이 공부하고 다시 도전하세요!';
  document.getElementById('r-rewards').innerHTML =
    '<div class="reward-chip">다시 도전하면 더 강해질 거예요 💪</div>';
  setTimeout(function() { showScreen('s-result'); }, 1200);
}