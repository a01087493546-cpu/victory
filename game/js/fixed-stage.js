(function () {
  const STAGE_WIDTH = 1920;
  const STAGE_HEIGHT = 1080;

  let currentScale = 1;

  function addFixedStageStyle() {
    if (document.getElementById("gm-fixed-stage-style")) {
      return;
    }

    const style = document.createElement("style");
    style.id = "gm-fixed-stage-style";
    style.textContent = `
      html,
      body {
        width: 100%;
        height: 100%;
        overflow: hidden !important;
      }

      body.gm-fixed-stage-body {
        margin: 0 !important;
      }

      .gm-fixed-stage-viewport {
        width: 100vw;
        height: 100vh;
        overflow: hidden;
        display: grid;
        justify-items: center;
        align-items: center;
        background: #070503;
      }

      .gm-fixed-stage-frame {
        position: relative;
        width: calc(1920px * var(--stage-scale, 1));
        height: calc(1080px * var(--stage-scale, 1));
      }

      .gm-fixed-stage-page {
        width: 1920px !important;
        height: 1080px !important;
        transform: scale(var(--stage-scale, 1));
        transform-origin: top left;
        overflow: hidden !important;
      }
    `;

    document.head.appendChild(style);
  }

  function disableResponsiveMediaRules() {
    Array.from(document.styleSheets).forEach((sheet) => {
      let rules;

      try {
        rules = sheet.cssRules;
      } catch (error) {
        return;
      }

      if (!rules) {
        return;
      }

      Array.from(rules).forEach((rule) => {
        if (!rule.media || typeof rule.media.mediaText !== "string") {
          return;
        }

        const mediaText = rule.media.mediaText;

        if (/max-(width|height)/i.test(mediaText)) {
          rule.media.mediaText = "not all";
        }
      });
    });
  }

  function updateScale() {
    const viewport = document.querySelector(".gm-fixed-stage-viewport");
    const frame = document.querySelector(".gm-fixed-stage-frame");
    const page = document.querySelector(".gm-fixed-stage-page");

    if (!viewport || !frame || !page) {
      return;
    }

    currentScale = Math.min(window.innerWidth / STAGE_WIDTH, window.innerHeight / STAGE_HEIGHT, 1);

    frame.style.setProperty("--stage-scale", currentScale);
    page.style.setProperty("--stage-scale", currentScale);
    frame.style.width = `${STAGE_WIDTH * currentScale}px`;
    frame.style.height = `${STAGE_HEIGHT * currentScale}px`;
  }

  function wrapPage() {
    addFixedStageStyle();
    disableResponsiveMediaRules();

    document.body.classList.add("gm-fixed-stage-body");

    const viewport = document.createElement("div");
    viewport.className = "gm-fixed-stage-viewport";

    const frame = document.createElement("div");
    frame.className = "gm-fixed-stage-frame";

    const page = document.createElement("div");
    page.className = "gm-fixed-stage-page";

    const movableNodes = Array.from(document.body.childNodes).filter((node) => {
      return !(node.nodeType === Node.ELEMENT_NODE && node.tagName === "SCRIPT");
    });

    movableNodes.forEach((node) => page.appendChild(node));
    frame.appendChild(page);
    viewport.appendChild(frame);
    document.body.insertBefore(viewport, document.body.firstChild);

    updateScale();
    window.addEventListener("resize", updateScale);
    window.addEventListener("load", updateScale);
    setTimeout(updateScale, 80);
    setTimeout(updateScale, 300);
  }

  // 데미지/텍스트 팝업처럼 getBoundingClientRect(실제 화면 px)로 좌표를 구한 뒤
  // 스케일이 적용되지 않은 로컬 좌표계에 그대로 대입하는 코드에서
  // 축소 배율을 나눠 정규화할 수 있도록 현재 스케일 값을 노출합니다.
  window.getStageScale = function () {
    return currentScale;
  };

  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", wrapPage);
  } else {
    wrapPage();
  }
})();
