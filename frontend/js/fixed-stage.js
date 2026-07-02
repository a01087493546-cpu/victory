(function () {
  const STAGE_WIDTH = 1920;
  const STAGE_HEIGHT = 1080;

  const existingStagePairs = [
    [".mq-stage-viewport", ".mq-stage-frame", ".mq-login-page"],
    [".story-stage-viewport", ".story-stage-frame", ".story-intro-page"],
    [".stage-viewport", ".stage-frame", ".class-reading-page"],
    [".student-stage-viewport", ".student-stage-frame", ".student-home-page"],
    [".tr-stage-viewport", ".tr-stage-frame", ".tr-page"],
    [".teacher-stage-viewport", ".teacher-stage-frame", ".practice-page"]
  ];

  function addFixedStageStyle() {
    if (document.getElementById("mq-fixed-stage-style")) {
      return;
    }

    const style = document.createElement("style");
    style.id = "mq-fixed-stage-style";
    style.textContent = `
      html,
      body {
        width: 100%;
        height: 100%;
        overflow: hidden !important;
      }

      body.mq-fixed-stage-body {
        margin: 0 !important;
      }

      .mq-fixed-stage-viewport {
        width: 100vw;
        height: 100vh;
        overflow-x: hidden;
        overflow-y: auto;
        display: grid;
        justify-items: center;
        align-items: start;
        background-position: center !important;
        background-size: cover !important;
      }

      .mq-fixed-stage-frame {
        position: relative;
        width: calc(1920px * var(--stage-scale, 1));
        height: calc(1080px * var(--stage-scale, 1));
      }

      .mq-fixed-stage-page {
        width: 1920px !important;
        height: 1080px !important;
        min-height: 1080px !important;
        transform: scale(var(--stage-scale, 1));
        transform-origin: top left;
        overflow: hidden !important;
      }

      .mq-fixed-stage-page > main,
      .mq-fixed-stage-page > div:not(.home-modal-backdrop):not(.fw-question-modal):not(.fw-complete-modal):not(.fw-reward-overlay):not(.wr-guide-modal) {
        width: 1920px !important;
        max-width: none !important;
        min-height: 1080px !important;
      }

      .mq-fixed-stage-page .shell,
      .mq-fixed-stage-page .page-inner,
      .mq-fixed-stage-page .br-shell,
      .mq-fixed-stage-page .fs-shell,
      .mq-fixed-stage-page .fw-shell,
      .mq-fixed-stage-page .tra-shell,
      .mq-fixed-stage-page .wr-shell,
      .mq-fixed-stage-page .bs-shell {
        width: 1680px !important;
        max-width: 1680px !important;
        margin-left: auto !important;
        margin-right: auto !important;
      }

      .mq-fixed-stage-page.mq-page-during-read .shell {
        height: 1038px !important;
      }

      .mq-fixed-stage-page.mq-page-book-chat .shell,
      .mq-fixed-stage-page.mq-page-friend-question .shell,
      .mq-fixed-stage-page.mq-page-book-chat-manage .shell,
      .mq-fixed-stage-page.mq-page-individual-book-chat-manage .shell {
        min-height: 980px !important;
      }

      .mq-fixed-stage-page .ar-shell {
        width: 1680px !important;
        max-width: 1680px !important;
        margin-left: auto !important;
        margin-right: auto !important;
      }

      .mq-fixed-stage-page .page {
        width: 1920px !important;
        min-height: 1080px !important;
      }

      .mq-fixed-stage-page .ar-page {
        width: 1920px !important;
        min-height: 1080px !important;
      }

      .mq-fixed-stage-page .step-list {
        grid-template-columns: repeat(4, 1fr) !important;
      }

      .mq-fixed-stage-page .tr-menu-grid {
        grid-template-columns: repeat(3, 1fr) !important;
      }

      .mq-fixed-stage-page .home-choice-area {
        grid-template-columns: 1fr !important;
      }

      .mq-fixed-stage-page .top {
        grid-template-columns: 220px 1fr 300px !important;
      }

      .mq-fixed-stage-page .hero {
        grid-template-columns: 0.9fr 1.1fr !important;
      }

      .mq-fixed-stage-page .board-grid {
        grid-template-columns: 0.88fr 1.12fr !important;
      }

      .mq-fixed-stage-page .filter-row {
        grid-template-columns: repeat(4, 1fr) 160px !important;
      }

      .mq-fixed-stage-page.mq-page-friend-question .filter-row {
        grid-template-columns: repeat(5, 1fr) 150px !important;
      }

      .mq-fixed-stage-page .wr-dashboard {
        grid-template-columns: 1.42fr 0.92fr !important;
      }

      .mq-fixed-stage-page .bs-dashboard {
        grid-template-columns: 1.25fr 0.85fr !important;
      }

      .mq-fixed-stage-page .br-main {
        grid-template-columns: 300px minmax(0, 1fr) !important;
      }

      .mq-fixed-stage-page .tra-main {
        grid-template-columns: minmax(0, 1fr) 340px !important;
      }

      .mq-fixed-stage-page .fs-main {
        grid-template-columns: 320px minmax(0, 1fr) !important;
      }

      .mq-fixed-stage-page .fw-main {
        grid-template-columns: 340px minmax(0, 1fr) !important;
      }

      .mq-fixed-stage-page .ibr-main,
      .mq-fixed-stage-page .idr-main,
      .mq-fixed-stage-page .iar-main {
        grid-template-columns: minmax(0, 1fr) 340px !important;
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

  function getStagePage(viewport) {
    return viewport.querySelector(".mq-fixed-stage-page");
  }

  function updateExistingStageScale(viewport, frame, page) {
    const scale = Math.min(window.innerWidth / STAGE_WIDTH, window.innerHeight / STAGE_HEIGHT, 1);

    viewport.style.width = "100vw";
    viewport.style.height = "100vh";
    viewport.style.overflowX = "hidden";
    viewport.style.overflowY = "auto";
    viewport.style.display = "grid";
    viewport.style.justifyItems = "center";
    viewport.style.alignItems = "start";

    frame.style.setProperty("--stage-scale", scale);
    frame.style.width = `${STAGE_WIDTH * scale}px`;
    frame.style.height = `${STAGE_HEIGHT * scale}px`;

    page.style.setProperty("--stage-scale", scale);
    page.style.width = `${STAGE_WIDTH}px`;
    page.style.height = `${STAGE_HEIGHT}px`;
    page.style.minHeight = `${STAGE_HEIGHT}px`;
    page.style.transform = `scale(${scale})`;
    page.style.transformOrigin = "top left";
    page.style.overflow = "hidden";
  }

  function normalizeExistingStage() {
    for (const [viewportSelector, frameSelector, pageSelector] of existingStagePairs) {
      const viewport = document.querySelector(viewportSelector);
      const frame = document.querySelector(frameSelector);
      const page = document.querySelector(pageSelector);

      if (!viewport || !frame || !page) {
        continue;
      }

      const update = () => updateExistingStageScale(viewport, frame, page);
      update();
      window.addEventListener("resize", update);
      window.addEventListener("load", update);
      setTimeout(update, 80);
      setTimeout(update, 300);
      return true;
    }

    return false;
  }

  function updateScale() {
    const viewport = document.querySelector(".mq-fixed-stage-viewport");
    const frame = document.querySelector(".mq-fixed-stage-frame");
    const page = viewport ? getStagePage(viewport) : null;

    if (!viewport || !frame || !page) {
      return;
    }

    const scale = Math.min(window.innerWidth / STAGE_WIDTH, window.innerHeight / STAGE_HEIGHT, 1);

    frame.style.setProperty("--stage-scale", scale);
    page.style.setProperty("--stage-scale", scale);
    frame.style.width = `${STAGE_WIDTH * scale}px`;
    frame.style.height = `${STAGE_HEIGHT * scale}px`;
  }

  function wrapPage() {
    addFixedStageStyle();
    disableResponsiveMediaRules();

    if (normalizeExistingStage()) {
      return;
    }

    document.body.classList.add("mq-fixed-stage-body");

    const viewport = document.createElement("div");
    viewport.className = "mq-fixed-stage-viewport";

    const frame = document.createElement("div");
    frame.className = "mq-fixed-stage-frame";

    const page = document.createElement("div");
    const pageName = location.pathname
      .split("/")
      .pop()
      .replace(/\.html$/, "")
      .replace(/[^a-z0-9-]/gi, "");

    page.className = `mq-fixed-stage-page mq-page-${pageName}`;

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

  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", wrapPage);
  } else {
    wrapPage();
  }
})();
