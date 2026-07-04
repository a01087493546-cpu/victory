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
        background-color: #1f140d !important;
        background-position: center !important;
        background-size: cover !important;
      }

      body:has(.mq-page-read-before-intro) .mq-fixed-stage-viewport {
        background:
          linear-gradient(rgba(22, 14, 8, 0.18), rgba(22, 14, 8, 0.34)),
          url("../assets/images/read-before/intro/pre-reading-bg-magic-greenhouse.png") center / cover no-repeat !important;
      }

      body:has(.mq-page-individual-reading) .mq-fixed-stage-viewport {
        background:
          linear-gradient(
            90deg,
            rgba(24, 17, 9, 0.38) 0%,
            rgba(255, 245, 219, 0.56) 42%,
            rgba(31, 21, 10, 0.34) 100%
          ),
          url("../assets/images/individual-reading/individual-reading.png") center / cover no-repeat !important;
      }

      body:has(.mq-page-read-before-book-intro) .mq-fixed-stage-viewport {
        background:
          linear-gradient(rgba(255, 248, 235, 0.5), rgba(255, 241, 215, 0.68)),
          url("../assets/images/read-before/intro/read-before-intro-bg.png") center / cover no-repeat !important;
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

      .mq-fixed-stage-page.mq-page-read-before-intro .intro-page {
        width: 1920px !important;
        height: 1080px !important;
        min-height: 1080px !important;
      }

      .mq-fixed-stage-page.mq-page-read-before-intro .story-box {
        left: 145px !important;
        bottom: 132px !important;
        width: 640px !important;
        min-height: 332px !important;
        padding: 30px 30px 26px !important;
      }

      .mq-fixed-stage-page.mq-page-read-before-intro .story-text {
        min-height: 156px !important;
        font-size: 31px !important;
        line-height: 1.48 !important;
      }

      .mq-fixed-stage-page.mq-page-read-before-intro .intro-lumi {
        right: 140px !important;
        bottom: 0 !important;
        width: 520px !important;
        max-height: 910px !important;
      }

      .mq-fixed-stage-page.mq-page-read-before-intro .next-button,
      .mq-fixed-stage-page.mq-page-read-before-intro .prev-button {
        min-width: 154px !important;
        padding: 16px 28px !important;
        font-size: 18px !important;
      }

      .mq-fixed-stage-page .ar-page {
        width: 1920px !important;
        min-height: 1080px !important;
      }

      .mq-fixed-stage-page.mq-page-individual-reading .ir-page {
        width: 1920px !important;
        height: 1080px !important;
        min-height: 1080px !important;
        padding: 24px 72px 34px !important;
      }

      .mq-fixed-stage-page.mq-page-individual-reading .ir-topbar,
      .mq-fixed-stage-page.mq-page-individual-reading .ir-shell {
        width: 1500px !important;
        max-width: 1500px !important;
      }

      .mq-fixed-stage-page.mq-page-individual-reading .ir-shell {
        height: 940px !important;
        padding: 28px 34px 28px !important;
      }

      .mq-fixed-stage-page.mq-page-individual-reading .ir-title {
        height: 88px !important;
      }

      .mq-fixed-stage-page.mq-page-individual-reading .ir-title h1 {
        font-size: 58px !important;
      }

      .mq-fixed-stage-page.mq-page-individual-reading .ir-main-row {
        height: 270px !important;
        grid-template-columns: 560px 1fr !important;
        gap: 28px !important;
        margin-bottom: 22px !important;
      }

      .mq-fixed-stage-page.mq-page-individual-reading .ir-power-body {
        grid-template-columns: 190px minmax(0, 1fr) !important;
        gap: 20px !important;
      }

      .mq-fixed-stage-page.mq-page-individual-reading .ir-practice-card {
        grid-template-columns: 240px minmax(0, 1fr) !important;
        gap: 28px !important;
        padding: 22px 42px !important;
      }

      .mq-fixed-stage-page.mq-page-individual-reading .ir-practice-lumi {
        width: 220px !important;
        height: 224px !important;
      }

      .mq-fixed-stage-page.mq-page-individual-reading .ir-chart-card {
        height: 285px !important;
        margin-bottom: 22px !important;
      }

      .mq-fixed-stage-page.mq-page-individual-reading .ir-flow {
        height: 146px !important;
        grid-template-columns: 1fr 30px 1fr 30px 1fr !important;
        gap: 12px !important;
      }

      .mq-fixed-stage-page.mq-page-individual-reading .ir-flow-card {
        height: 146px !important;
        grid-template-columns: 104px minmax(0, 1fr) 38px !important;
        padding: 18px 22px !important;
      }

      .mq-fixed-stage-page.mq-page-individual-reading .ir-flow-card img {
        width: 92px !important;
        height: 92px !important;
      }

      .mq-fixed-stage-page.mq-page-before-reading .page {
        width: 1920px !important;
        height: 1080px !important;
        min-height: 1080px !important;
      }

      .mq-fixed-stage-page.mq-page-before-reading .page-inner {
        width: 1680px !important;
        max-width: 1680px !important;
      }

      .mq-fixed-stage-page.mq-page-before-reading .activity-layout {
        grid-template-columns: 1fr !important;
      }

      .mq-fixed-stage-page.mq-page-before-reading .side-steps {
        display: none !important;
      }

      .mq-fixed-stage-page.mq-page-before-reading .activity-card {
        width: 1460px !important;
        min-height: 640px !important;
        padding: 58px 72px !important;
        background: #f4ead8 !important;
      }

      .mq-fixed-stage-page.mq-page-before-reading .activity-card.is-example .content-grid,
      .mq-fixed-stage-page.mq-page-before-reading .activity-card.is-write .content-grid {
        min-height: 560px !important;
      }

      .mq-fixed-stage-page.mq-page-before-reading .example-panel {
        min-height: 540px !important;
      }

      .mq-fixed-stage-page.mq-page-before-reading .example-list {
        grid-template-columns: repeat(3, minmax(0, 1fr)) !important;
      }

      .mq-fixed-stage-page.mq-page-before-reading .write-grid {
        grid-template-columns: repeat(2, minmax(0, 1fr)) !important;
      }

      .mq-fixed-stage-page.mq-page-read-before-book-intro .page {
        width: 1920px !important;
        height: 1080px !important;
        min-height: 1080px !important;
        padding: 32px 64px !important;
      }

      .mq-fixed-stage-page.mq-page-read-before-book-intro .page-inner {
        width: 1660px !important;
        max-width: 1660px !important;
        height: 100% !important;
        grid-template-rows: 60px 126px minmax(0, 1fr) !important;
        gap: 22px !important;
      }

      .mq-fixed-stage-page.mq-page-read-before-book-intro .book-strip {
        grid-template-columns: 92px minmax(0, 1fr) 520px !important;
        min-height: 126px !important;
      }

      .mq-fixed-stage-page.mq-page-read-before-book-intro .step-progress {
        grid-template-columns: 1fr 48px 1fr 48px 1fr 48px 1fr !important;
      }

      .mq-fixed-stage-page.mq-page-read-before-book-intro .intro-card {
        height: 816px !important;
        min-height: 0 !important;
        grid-template-columns: 430px minmax(0, 1fr) !important;
        gap: 70px !important;
        padding: 56px 84px !important;
      }

      .mq-fixed-stage-page.mq-page-read-before-book-intro .cover-slot {
        width: 430px !important;
        height: 620px !important;
      }

      .mq-fixed-stage-page.mq-page-read-before-book-intro .intro-content {
        min-width: 0 !important;
        padding-right: 0 !important;
      }

      .mq-fixed-stage-page.mq-page-read-before-book-intro .intro-title {
        font-size: 72px !important;
        line-height: 1.02 !important;
      }

      .mq-fixed-stage-page.mq-page-read-before-book-intro .intro-subtitle {
        font-size: 28px !important;
      }

      .mq-fixed-stage-page.mq-page-read-before-book-intro .intro-book-box {
        width: 650px !important;
      }

      .mq-fixed-stage-page .step-list {
        grid-template-columns: repeat(4, 1fr) !important;
      }

      .mq-fixed-stage-page.mq-page-practice .step-list {
        width: 100% !important;
        max-width: 100% !important;
        grid-template-columns: repeat(3, minmax(0, 1fr)) !important;
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
