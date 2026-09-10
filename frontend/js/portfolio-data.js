/*
  파일명: portfolio-data.js
  역할: 온책읽기/개별읽기 "학생 성장 포트폴리오" 편집 화면(teacher/
  practice-growth-portfolio.html, teacher/individual-growth-portfolio.html)이
  쓰는 데이터 loader(adapter) 모음.

  집계 전용 백엔드 API(.../practice-portfolio, .../individual-portfolio)는
  다른 세션이 동시에 개발 중이라 아직 없다. fetch 지점을 이 파일 한 곳에
  모아 뒀으므로, 그 API가 완성되면 PORTFOLIO_AGGREGATE_PATHS와
  normalizePracticePortfolioAggregate/normalizeIndividualPortfolioAggregate의
  필드 매핑만 실제 응답 모양에 맞춰 고치면 된다.

  집계 API가 아직 없거나 실패하면(network error/404/501 등) 이미 존재하는
  교사용 화면(book-manage.html, individual-reading-manage.html)이 쓰는
  것과 동일한 API를 조합해 값을 채운다. 이 API들은 심사(데모) 교사
  계정(tt11)에 대해 서버(PracticeAchievementService.buildDemoClassAchievement/
  buildDemoHistory 등)가 이미 데모 전용 값을 내려주므로, 화면은 그 실제
  응답을 그대로 재사용할 뿐 새 난수/placeholder 숫자를 만들지 않는다.

  fallback 조합으로도 구할 수 없는 값(온책읽기 "활동 횟수", 개별읽기
  "활동 현황 4종 세부/월별 완독 기록/독서 역량 4종" - 학생 본인 토큰
  전용 API만 있어 교사 화면에서 조회 불가, 개별읽기 데이터 조사 보고서
  참고)은 0이나 임의 숫자 대신 unavailableFields로 표시한다. 화면은 이
  필드를 "집계 준비 중"으로 보여주고 교사가 알고 있는 값을 직접
  입력하도록 편집 가능한 입력칸으로 남긴다.
*/

function getPortfolioApiBaseUrl() {
  if (window.location.hostname === "127.0.0.1") {
    return "http://127.0.0.1:8080";
  }
  if (window.location.hostname === "localhost") {
    return "http://localhost:8080";
  }
  return "https://victory-production-f94d.up.railway.app";
}

const PORTFOLIO_API_BASE_URL = getPortfolioApiBaseUrl();

/*
 * 집계 전용 API 제안 경로. 백엔드가 완성되면 아래 경로 문자열만 실제
 * 경로에 맞춰 수정하면 된다(다른 세션과 최종 합의 필요).
 */
const PORTFOLIO_AGGREGATE_PATHS = {
  practice: function (teacherId, classId, studentId) {
    return "/api/teachers/" + teacherId + "/classes/" + classId +
      "/students/" + studentId + "/practice-portfolio";
  },
  individual: function (teacherId, classId, studentId) {
    return "/api/teachers/" + teacherId + "/classes/" + classId +
      "/students/" + studentId + "/individual-portfolio";
  }
};

/* 이미 여러 교사 화면이 쓰는 것과 같은 규칙(token/id/role) - 반복 정의를
   피하려고 여기 한 곳에만 둔다. */
function getPortfolioTeacherAuthContext() {
  const token = sessionStorage.getItem("token");
  const teacherId = sessionStorage.getItem("id") || sessionStorage.getItem("studentId");
  const role = sessionStorage.getItem("role");

  if (!token || !teacherId || role !== "teacher") {
    return null;
  }

  return { token: token, teacherId: teacherId };
}

async function portfolioFetchJson(path, token) {
  try {
    const response = await fetch(PORTFOLIO_API_BASE_URL + path, {
      headers: token ? { Authorization: "Bearer " + token } : {}
    });

    if (!response.ok) {
      return { ok: false, status: response.status, body: null };
    }

    const body = await response.json().catch(function () {
      return null;
    });

    return { ok: true, status: response.status, body: body };
  } catch (error) {
    return { ok: false, status: 0, body: null, networkError: error };
  }
}

function portfolioToDateInputValue(date) {
  return date.toISOString().slice(0, 10);
}

/*
 * 기본 평가 기간: 최근 6개월. 교사가 화면에서 자유롭게 바꿀 수 있다.
 * "학생 성장 포트폴리오"는 한 학기 단위로 보는 경우가 많고, 개별읽기
 * 데모 시드(DemoExperienceDataInitializer)도 완독 시점을 최대 4개월
 * 전까지 분산해 두므로, 기본값이 1개월처럼 너무 좁으면 실제 aggregate
 * API가 정상 200을 반환하고도 "최근 데이터가 없다"는 이유로 0으로 보일
 * 수 있다(집계 실패가 아니라 기간 밖이라 0인 것 - 혼동하기 쉬워 기본값을
 * 넉넉하게 잡는다).
 */
function defaultPortfolioPeriod() {
  const to = new Date();
  const from = new Date();
  from.setMonth(from.getMonth() - 6);
  return {
    from: portfolioToDateInputValue(from),
    to: portfolioToDateInputValue(to)
  };
}

function portfolioAverage(values) {
  const nums = values.filter(function (value) {
    return typeof value === "number" && !Number.isNaN(value);
  });
  if (nums.length === 0) return null;
  return nums.reduce(function (a, b) { return a + b; }, 0) / nums.length;
}

/* =========================================================
   온책읽기(연습읽기) 포트폴리오
   ========================================================= */

async function loadPracticePortfolioData(auth, classId, studentId, from, to) {
  const aggregatePath =
    PORTFOLIO_AGGREGATE_PATHS.practice(auth.teacherId, classId, studentId) +
    "?from=" + encodeURIComponent(from) + "&to=" + encodeURIComponent(to);

  const aggregate = await portfolioFetchJson(aggregatePath, auth.token);
  if (aggregate.ok && aggregate.body) {
    return {
      source: "aggregate-api",
      unavailableFields: Array.isArray(aggregate.body.unavailableFields)
        ? aggregate.body.unavailableFields
        : [],
      data: normalizePracticePortfolioAggregate(aggregate.body)
    };
  }

  return loadPracticePortfolioFallback(auth, classId, studentId, from, to);
}

/*
 * PracticePortfolioResponse(record)의 canonical 필드를 우선 읽고,
 * (구버전 응답 등에서) canonical이 없을 때만 @JsonProperty alias
 * (beforeStatus/duringStatus/afterStatus/bookTitle)로 대체한다.
 * canonical: beforeParticipation/duringParticipation/afterParticipation
 * (PortfolioActivityCount{count, participated}), currentBookTitle.
 */
function portfolioParticipationStatus(participation, aliasStatus) {
  if (participation && typeof participation.participated === "boolean") {
    return participation.participated ? "완료" : "미완료";
  }
  return aliasStatus || "";
}

function normalizePracticePortfolioAggregate(body) {
  const stageAnalysis = body.stageAnalysis || {};
  const stageText = function (keys) {
    for (const key of keys) {
      const value = stageAnalysis[key];
      if (typeof value === "string" && value.trim()) return value;
      if (value && typeof value === "object") {
        const text = value.growthPoint || value.strength || value.strengthText || value.message || value.summary;
        if (typeof text === "string" && text.trim()) return text;
      }
    }
    return "";
  };
  return {
    studentName: body.studentName || "",
    grade: body.grade != null ? body.grade : null,
    classNumber: body.classNumber != null ? body.classNumber : null,
    participationRate: body.participationRate != null ? body.participationRate : null,
    comprehensionRate: body.comprehensionRate != null ? body.comprehensionRate : null,
    beforeStatus: portfolioParticipationStatus(body.beforeParticipation, body.beforeStatus),
    duringStatus: portfolioParticipationStatus(body.duringParticipation, body.duringStatus),
    afterStatus: portfolioParticipationStatus(body.afterParticipation, body.afterStatus),
    stageAnalysis: {
      before: stageText(["before", "beforeReading"]) || body.beforeStage?.growthNote || body.beforeAnalysis || body.beforeGrowthPoint || "",
      during: stageText(["during", "duringReading"]) || body.duringStage?.growthNote || body.duringAnalysis || body.duringGrowthPoint || "",
      after: stageText(["after", "afterReading"]) || body.afterStage?.growthNote || body.afterAnalysis || body.afterGrowthPoint || ""
    },
    bookTitle: body.currentBookTitle || body.bookTitle || "",
    activityCount: body.activityCount != null ? body.activityCount : null,
    aiStrengths: body.aiStrengths || "",
    aiImprovements: body.aiImprovements || ""
  };
}

async function loadPracticePortfolioFallback(auth, classId, studentId, from, to) {
  const unavailable = [];
  const data = {
    studentName: "",
    grade: null,
    classNumber: null,
    participationRate: null,
    comprehensionRate: null,
    beforeStatus: "",
    duringStatus: "",
    afterStatus: "",
    stageAnalysis: { before: "", during: "", after: "" },
    bookTitle: "",
    activityCount: null,
    aiStrengths: "",
    aiImprovements: ""
  };

  const classInfo = await portfolioFetchJson("/api/teachers/" + auth.teacherId + "/class", auth.token);
  if (classInfo.ok && classInfo.body) {
    data.grade = classInfo.body.grade;
    data.classNumber = classInfo.body.classNumber;
  }

  const studentsInfo = await portfolioFetchJson("/api/classes/" + classId + "/students", auth.token);
  if (studentsInfo.ok && Array.isArray(studentsInfo.body)) {
    const found = studentsInfo.body.find(function (student) {
      return String(student.studentId) === String(studentId);
    });
    if (found) data.studentName = found.name;
  }

  /*
   * 기간 평균: 성취도 스냅샷 이력(practice-achievement/history)이 있으면
   * 우선 쓰고, 이력이 비어 있으면(스냅샷이 아직 안 쌓인 경우) 현재값으로
   * 대체한다. 이력 API도 심사 교사 계정을 데모값으로 응답하므로(서버
   * buildDemoHistory) 동일하게 재사용된다.
   */
  const history = await portfolioFetchJson(
    "/api/teachers/" + auth.teacherId + "/classes/" + classId +
      "/practice-achievement/history?studentId=" + studentId +
      "&from=" + encodeURIComponent(from) + "&to=" + encodeURIComponent(to),
    auth.token
  );
  if (history.ok && history.body && Array.isArray(history.body.history) && history.body.history.length > 0) {
    data.participationRate = portfolioAverage(
      history.body.history.map(function (item) { return item.participationRate; })
    );
    data.comprehensionRate = portfolioAverage(
      history.body.history.map(function (item) { return item.comprehensionRate; })
    );
  }

  const achievement = await portfolioFetchJson(
    "/api/teachers/" + auth.teacherId + "/classes/" + classId + "/practice-achievement",
    auth.token
  );
  if (achievement.ok && achievement.body && Array.isArray(achievement.body.students)) {
    const item = achievement.body.students.find(function (student) {
      return String(student.studentId) === String(studentId);
    });
    if (item) {
      if (data.participationRate === null) data.participationRate = item.participationRate;
      if (data.comprehensionRate === null) data.comprehensionRate = item.comprehensionRate;
      if (!data.studentName) data.studentName = item.studentName;
    }
  }

  const progress = await portfolioFetchJson("/api/students/" + studentId + "/practice-progress", auth.token);
  if (progress.ok && progress.body) {
    data.beforeStatus = progress.body.beforeDone ? "완료" : "미완료";
    data.duringStatus = progress.body.classReadDone ? "완료" : "미완료";
    data.afterStatus = progress.body.afterDone ? "완료" : "미완료";
  } else {
    unavailable.push("beforeStatus", "duringStatus", "afterStatus");
  }

  const readingRange = await portfolioFetchJson("/api/classes/" + classId + "/reading-range", auth.token);
  if (readingRange.ok && readingRange.body && readingRange.body.bookTitle) {
    data.bookTitle = readingRange.body.bookTitle;
  } else {
    unavailable.push("bookTitle");
  }

  /*
   * 활동 횟수: 온책읽기 데이터 조사 결과 실제 집계 소스가 없다
   * (student_daily_metrics.activity_count_today는 테이블조차 생성되지
   * 않는 미사용 컬럼). 집계 API가 붙기 전까지는 0을 임의로 채우지 않고
   * 교사가 직접 입력하게 비워 둔다.
   */
  unavailable.push("activityCount");

  if (data.participationRate === null) unavailable.push("participationRate");
  if (data.comprehensionRate === null) unavailable.push("comprehensionRate");

  return { source: "legacy-fallback", unavailableFields: unavailable, data: data };
}

/* =========================================================
   개별읽기 포트폴리오
   ========================================================= */

async function loadIndividualPortfolioData(auth, classId, studentId, from, to) {
  const aggregatePath =
    PORTFOLIO_AGGREGATE_PATHS.individual(auth.teacherId, classId, studentId) +
    "?from=" + encodeURIComponent(from) + "&to=" + encodeURIComponent(to);

  const aggregate = await portfolioFetchJson(aggregatePath, auth.token);
  if (aggregate.ok && aggregate.body) {
    return {
      source: "aggregate-api",
      unavailableFields: Array.isArray(aggregate.body.unavailableFields)
        ? aggregate.body.unavailableFields
        : [],
      data: normalizeIndividualPortfolioAggregate(aggregate.body)
    };
  }

  return loadIndividualPortfolioFallback(auth, classId, studentId, from, to);
}

/*
 * IndividualPortfolioResponse(record)의 canonical(중첩) 필드를 우선
 * 읽고, 없을 때만 @JsonProperty alias(평평한 필드)로 대체한다.
 * canonical: completedBookCount, averageReadingPracticeScore,
 * averageRecordCompletionScore, activitySummary.{questions,
 * thoughtWriting, summaries, bookChatSharing},
 * monthlyCompletionStats.monthlyCounts,
 * readingCompetencies.{questionGeneration, readingPersistence,
 * thoughtRefinement, thoughtSharing}.{score, level}.
 */
function portfolioPickNumber() {
  for (let i = 0; i < arguments.length; i++) {
    const value = arguments[i];
    if (value !== null && value !== undefined) return value;
  }
  return null;
}

function normalizeIndividualPortfolioAggregate(body) {
  const activitySummary = body.activitySummary || {};
  const monthlyStats = body.monthlyCompletionStats || {};
  const readingCompetencies = body.readingCompetencies || {};
  const competency = function (name) {
    const value = readingCompetencies[name];
    return value && typeof value === "object" ? value : {};
  };
  const questionGeneration = competency("questionGeneration");
  const readingPersistence = competency("readingPersistence");
  const thoughtRefinement = competency("thoughtRefinement");
  const thoughtSharing = competency("thoughtSharing");

  return {
    studentName: body.studentName || "",
    grade: body.grade != null ? body.grade : null,
    classNumber: body.classNumber != null ? body.classNumber : null,
    booksReadCount: portfolioPickNumber(body.completedBookCount, body.booksReadCount),
    readingPracticeAvg: portfolioPickNumber(body.averageReadingPracticeScore, body.readingPracticeAvg),
    readingPracticeLevel: body.averageReadingPracticeLevel || body.readingPracticeLevel || "",
    recordCompletionAvg: portfolioPickNumber(body.averageRecordCompletionScore, body.recordCompletionAvg),
    recordCompletionLevel: body.averageRecordCompletionLevel || body.recordCompletionLevel || "",
    activityQuestionCount: portfolioPickNumber(activitySummary.questions, body.activityQuestionCount),
    activityThoughtCount: portfolioPickNumber(activitySummary.thoughtWriting, body.activityThoughtCount),
    activitySummaryCount: portfolioPickNumber(activitySummary.summaries, body.activitySummaryCount),
    activityBookChatCount: portfolioPickNumber(activitySummary.bookChatSharing, body.activityBookChatCount),
    monthlyCounts: Array.isArray(monthlyStats.monthlyCounts)
      ? monthlyStats.monthlyCounts
      : (Array.isArray(body.monthlyCounts) ? body.monthlyCounts : null),
    competencyQuestion: portfolioPickNumber(questionGeneration.score, body.questionGenerationScore),
    competencyQuestionLevel: questionGeneration.level || body.questionGenerationLevel || "",
    competencyPersistence: portfolioPickNumber(readingPersistence.score, body.readingPersistenceScore),
    competencyPersistenceLevel: readingPersistence.level || body.readingPersistenceLevel || "",
    competencyRefine: portfolioPickNumber(thoughtRefinement.score, body.thoughtRefinementScore),
    competencyRefineLevel: thoughtRefinement.level || body.thoughtRefinementLevel || "",
    competencyShare: portfolioPickNumber(thoughtSharing.score, body.thoughtSharingScore),
    competencyShareLevel: thoughtSharing.level || body.thoughtSharingLevel || "",
    aiStrengths: body.aiStrengths || "",
    aiImprovements: body.aiImprovements || ""
  };
}

async function loadIndividualPortfolioFallback(auth, classId, studentId, from, to) {
  const unavailable = [];
  const data = {
    studentName: "",
    grade: null,
    classNumber: null,
    booksReadCount: null,
    readingPracticeAvg: null,
    recordCompletionAvg: null,
    activityQuestionCount: null,
    activityThoughtCount: null,
    activitySummaryCount: null,
    activityBookChatCount: null,
    monthlyCounts: null,
    competencyQuestion: null,
    competencyQuestionLevel: "",
    competencyPersistence: null,
    competencyPersistenceLevel: "",
    competencyRefine: null,
    competencyRefineLevel: "",
    competencyShare: null,
    competencyShareLevel: "",
    aiStrengths: "",
    aiImprovements: ""
  };

  const classInfo = await portfolioFetchJson("/api/teachers/" + auth.teacherId + "/class", auth.token);
  if (classInfo.ok && classInfo.body) {
    data.grade = classInfo.body.grade;
    data.classNumber = classInfo.body.classNumber;
  }

  const dashboard = await portfolioFetchJson(
    "/api/teachers/" + auth.teacherId + "/classes/" + classId + "/individual-reading/dashboard",
    auth.token
  );
  if (dashboard.ok && dashboard.body && Array.isArray(dashboard.body.students)) {
    const item = dashboard.body.students.find(function (student) {
      return String(student.studentId) === String(studentId);
    });
    if (item) {
      data.studentName = item.studentName;
      data.booksReadCount = item.totalCompletedBookCount;
      data.readingPracticeAvg = item.readingPracticeScore;
      data.recordCompletionAvg = item.recordCompletionScore;
    }
  }

  if (data.booksReadCount === null) unavailable.push("booksReadCount");
  if (data.readingPracticeAvg === null) unavailable.push("readingPracticeAvg");
  if (data.recordCompletionAvg === null) unavailable.push("recordCompletionAvg");

  /*
   * 활동 현황 4종 세부 카운트, 월별 완독 기록, 독서 역량 4종 누적값은
   * 교사 토큰으로 조회 가능한 기존 API가 없다 - monthly-completion-stats는
   * "/api/students/me/..." 경로로 학생 본인 토큰만 허용하고,
   * StudentStatsController도 requireSelf로 본인 조회만 허용한다(개별읽기
   * 데이터 조사 보고서 참고). 집계 API가 붙기 전까지는 0/임의값 대신
   * "집계 준비 중"으로 두고 교사가 직접 입력할 수 있게 한다. 게임 현재
   * 능력치(마법력/체력/지혜/용기, 던전 리셋 대상)는 애초에 후보로 쓰지
   * 않는다.
   */
  unavailable.push(
    "activityQuestionCount", "activityThoughtCount", "activitySummaryCount", "activityBookChatCount",
    "monthlyCounts",
    "competencyQuestion", "competencyPersistence", "competencyRefine", "competencyShare"
  );

  return { source: "legacy-fallback", unavailableFields: unavailable, data: data };
}

/* =========================================================
   AI 분석 재생성
   ========================================================= */

/*
 * AI 분석 endpoint 제안 경로. 다른 세션이 백엔드를 개발 중이라 아직
 * 없을 수 있다(404) - 실제 경로가 다르게 확정되면 이 두 함수만
 * 고치면 된다.
 */
const PORTFOLIO_AI_ANALYSIS_PATHS = {
  practice: function (teacherId, classId, studentId) {
    return "/api/teachers/" + teacherId + "/classes/" + classId +
      "/students/" + studentId + "/practice-portfolio/ai-analysis";
  },
  individual: function (teacherId, classId, studentId) {
    return "/api/teachers/" + teacherId + "/classes/" + classId +
      "/students/" + studentId + "/individual-portfolio/ai-analysis";
  }
};

async function portfolioPostJson(path, token, payload) {
  try {
    const response = await fetch(PORTFOLIO_API_BASE_URL + path, {
      method: "POST",
      headers: Object.assign(
        { "Content-Type": "application/json" },
        token ? { Authorization: "Bearer " + token } : {}
      ),
      body: JSON.stringify(payload)
    });

    if (!response.ok) {
      return { ok: false, status: response.status, body: null };
    }

    const body = await response.json().catch(function () {
      return null;
    });

    return { ok: true, status: response.status, body: body };
  } catch (error) {
    return { ok: false, status: 0, body: null, networkError: error };
  }
}

/*
 * 백엔드 응답 필드 이름이 예상({"strength","improvement"})과 다를 때도
 * 흡수할 수 있도록 몇 가지 흔한 별칭을 함께 지원한다. 실제 확정된
 * field 이름이 이 목록에 없으면 이 함수만 고치면 된다.
 */
function normalizePortfolioAnalysisResponse(body) {
  if (!body || typeof body !== "object") return null;

  const strength = body.strengthText ?? body.strength ?? body.aiStrengths ?? body.strengths ?? body.good ?? null;
  const improvement = body.improvementText ?? body.improvement ?? body.aiImprovements ?? body.improvements ?? body.toImprove ?? null;

  if (strength == null && improvement == null) return null;

  const stages = body.stageAnalysis && typeof body.stageAnalysis === "object" ? body.stageAnalysis : null;
  const stageText = function (stage) {
    if (!stage || typeof stage !== "object") return "";
    const strengthText = typeof stage.strengthText === "string" ? stage.strengthText.trim() : "";
    const growthText = typeof stage.growthText === "string" ? stage.growthText.trim() : "";
    if (strengthText || growthText) return {
      title: typeof stage.title === "string" ? stage.title.trim() : "",
      completed: typeof stage.completed === "boolean" ? stage.completed : null,
      strengthText: strengthText,
      growthText: growthText
    };
    const text = stage.growthPoint || stage.message;
    return typeof text === "string" ? text.trim() : "";
  };

  return {
    strength: typeof strength === "string" ? strength : "",
    improvement: typeof improvement === "string" ? improvement : "",
    stageAnalysis: stages ? {
      before: stageText(stages.before),
      during: stageText(stages.during),
      after: stageText(stages.after)
    } : null
  };
}

/*
 * AI 분석(잘하는 점/노력할 점) 재생성. 교사가 draft에서 수정한 값은
 * 절대 보내지 않는다 - AI 분석은 teacherId/classId/studentId/from/to
 * 좌표만으로 백엔드가 원본 aggregate 데이터를 다시 조회해 근거로
 * 삼아야, 교사가 출력용으로 고친 숫자가 AI 분석 근거를 오염시키지
 * 않는다. kind는 "practice" 또는 "individual".
 */
async function regeneratePortfolioAnalysis(kind, auth, classId, studentId, from, to) {
  const pathBuilder = PORTFOLIO_AI_ANALYSIS_PATHS[kind];
  if (!pathBuilder || !auth) {
    return { ok: false, reason: "invalid-arguments" };
  }

  const path = pathBuilder(auth.teacherId, classId, studentId);
  const result = await portfolioPostJson(path, auth.token, { from: from, to: to });

  if (!result.ok) {
    return { ok: false, status: result.status, reason: result.networkError ? "network" : "http" };
  }

  const analysis = normalizePortfolioAnalysisResponse(result.body);
  if (!analysis) {
    return { ok: false, status: result.status, reason: "empty-response" };
  }

  return { ok: true, strength: analysis.strength, improvement: analysis.improvement,
    stageAnalysis: analysis.stageAnalysis };
}

/* =========================================================
   draft(임시 저장) - localStorage, 학생/유형별로 분리
   ========================================================= */

function portfolioDraftKey(kind, studentId) {
  return "portfolioDraft_" + kind + "_" + studentId;
}

function savePortfolioDraft(kind, studentId, draft) {
  try {
    localStorage.setItem(
      portfolioDraftKey(kind, studentId),
      JSON.stringify({ draft: draft, savedAt: new Date().toISOString() })
    );
    return true;
  } catch (error) {
    console.error("포트폴리오 임시 저장에 실패했습니다.", error);
    return false;
  }
}

function loadPortfolioDraft(kind, studentId) {
  try {
    const raw = localStorage.getItem(portfolioDraftKey(kind, studentId));
    if (!raw) return null;
    return JSON.parse(raw);
  } catch (error) {
    console.error("포트폴리오 임시 저장 데이터를 불러오지 못했습니다.", error);
    return null;
  }
}

function clearPortfolioDraft(kind, studentId) {
  try {
    localStorage.removeItem(portfolioDraftKey(kind, studentId));
  } catch (error) {
    console.error("포트폴리오 임시 저장 데이터를 지우지 못했습니다.", error);
  }
}
