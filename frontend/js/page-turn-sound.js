// Preload once; start the effect in the same click as the scene change.
(() => {
  const sound = new Audio(new URL('../assets/audio/page-turn.wav', document.currentScript.src).href);
  sound.preload = 'auto';
  sound.load();

  window.playPageTurnSound = function () {
    try {
      sound.currentTime = 0;
      const playback = sound.play();
      // Audio availability must never block tutorial navigation.
      if (playback) playback.catch(() => {});
    } catch (_) {
      // Continue navigating even if the browser cannot play the effect.
    }
  };
})();
