/*
  파일명: demo-storage.js
  역할: 심사 체험 계정(ss01) 전용 임시 저장소.

  심사위원 여러 명이 같은 ss01 로그인을 서로 다른 브라우저/기기에서 동시에
  쓰더라도 각자 작성한 질문·답·루미 AI 피드백이 섞이거나 서로 덮어쓰지
  않도록, 공용 DB 대신 "현재 브라우저"의 localStorage에만 저장한다.
  일반 학생 계정은 이 파일을 전혀 거치지 않고 기존 DB 저장/복원 그대로
  동작한다(호출하는 화면에서 isDemoAccount()로 분기).

  키는 전부 MQ_DEMO_STORAGE_PREFIX로 시작한다. localStorage는 tab/브라우저를
  닫아도 지워지지 않으므로, 로그아웃해도 심사 체험 기록이 유지된다(로그아웃은
  auth.js에서 인증 키만 지우고 이 prefix는 건드리지 않는다). 대신 심사위원이
  직접 "심사 체험 기록 초기화" 버튼을 눌렀을 때만 clearAllDemoState()로
  이 prefix가 붙은 키를 전부 지운다.

  예전 버전은 sessionStorage를 사용했다. 브라우저에 남아 있는 예전
  sessionStorage 데이터를 잃지 않도록, 이 파일이 처음 로드될 때 localStorage에
  같은 키가 아직 없으면 sessionStorage → localStorage로 한 번만 옮겨 둔다
  (원본 sessionStorage 값은 지우지 않는다 — 탭을 닫으면 자연히 사라진다).
*/

const MQ_DEMO_STORAGE_PREFIX = "mq_demo_";
const MQ_DEMO_BROWSER_ID_KEY = MQ_DEMO_STORAGE_PREFIX + "browserId";
const DEMO_APPROVAL_STORAGE_KEYS = {
  practice: "practiceBookChatApprovals",
  individual: "individualBookChatApprovals"
};

function getDemoBrowserId() {
  try {
    let browserId = localStorage.getItem(MQ_DEMO_BROWSER_ID_KEY);
    if (browserId) return browserId;

    browserId = globalThis.crypto && typeof globalThis.crypto.randomUUID === "function"
      ? globalThis.crypto.randomUUID()
      : "demo-browser-" + Date.now() + "-" + Math.random().toString(36).slice(2);
    localStorage.setItem(MQ_DEMO_BROWSER_ID_KEY, browserId);
    return browserId;
  } catch (error) {
    console.error("심사 브라우저 식별값을 준비하지 못했습니다.", error);
    return "demo-browser-unavailable";
  }
}

function getDemoApprovalStorageKey(scope) {
  return DEMO_APPROVAL_STORAGE_KEYS[scope] || null;
}

function loadDemoApprovalRequests(scope) {
  const storageKey = getDemoApprovalStorageKey(scope);
  if (!storageKey) return [];
  const browserId = getDemoBrowserId();
  const requests = loadDemoState(storageKey, []);
  return Array.isArray(requests)
    ? requests.filter(function (request) {
        return request && request.demoBrowserId === browserId;
      })
    : [];
}

function saveDemoApprovalRequest(scope, request) {
  const storageKey = getDemoApprovalStorageKey(scope);
  if (!storageKey || !request) return null;

  const browserId = getDemoBrowserId();
  const requests = loadDemoApprovalRequests(scope);
  const savedRequest = Object.assign({}, request, {
    requestId: String(request.requestId || (scope + "-" + Date.now())),
    demoBrowserId: browserId,
    activityScope: request.activityScope || scope,
    status: request.status || "pending",
    rejectedReason: request.rejectedReason || ""
  });
  const existingIndex = requests.findIndex(function (item) {
    return String(item.requestId) === savedRequest.requestId;
  });

  if (existingIndex >= 0) requests[existingIndex] = savedRequest;
  else requests.push(savedRequest);
  saveDemoState(storageKey, requests);
  return savedRequest;
}

function updateDemoApprovalRequest(scope, requestId, status, rejectedReason) {
  const request = loadDemoApprovalRequests(scope).find(function (item) {
    return String(item.requestId) === String(requestId);
  });
  if (!request) return null;

  return saveDemoApprovalRequest(scope, Object.assign({}, request, {
    status: status,
    rejectedReason: status === "rejected" ? String(rejectedReason || "") : "",
    reviewedAt: new Date().toISOString()
  }));
}

/*
  심사 교사(tt11)가 "이미 공용 DB에 승인 상태로 심어진 책수다방 글"
  (DemoExperienceDataInitializer/DemoClassActivityInitializer가 만든 진짜
  Response 행, 학생 화면에 이미 공개돼 있음) 또는 화면에서만 존재하는
  "가상 승인대기 예시"의 상태를 이 브라우저에서만 뒤집어 보여줄 때 쓴다.
  loadDemoApprovalRequests/updateDemoApprovalRequest(ss01이 이 브라우저에서
  직접 쓴 글 전용)와는 완전히 다른 대상이라 별도 key를 쓴다 - 공용 DB
  행 자체는 절대 건드리지 않고, "이 postId는 이 브라우저에서 이 상태로
  보여준다"는 override만 로컬에 남긴다.
*/
const DEMO_BOOK_CHAT_OVERRIDE_STORAGE_KEYS = {
  practice: "practiceBookChatSeedOverrides",
  individual: "individualBookChatSeedOverrides"
};

/*
  간추리기(요약) 승인 상태 전용 override - after-read.html/individual-after-reading.html의
  editRejectedPracticeSummary/editRejectedIndividualSummary/getDemoLocalSharedSummary가
  이미 "summaryReviewOverrides" 키 + "practice:"/"individual:" 접두어 규칙으로 직접
  읽고 쓰고 있으므로, 교사용 간추리기 승인관리 화면도 이름을 새로 만들지 않고
  같은 규칙을 그대로 재사용한다(둘이 서로 다른 저장소를 보면 학생 화면과
  교사 승인 결과가 어긋난다).
*/
const DEMO_SUMMARY_REVIEW_OVERRIDE_KEY = "summaryReviewOverrides";

const DEMO_SUMMARY_REVIEW_SEEDS = {
  practice: {
    pending: [
      { id: "practice-summary-seed-pending-01", writerLoginId: "ss01", studentName: "김초롱", bookType: "story", summary: "주인공은 보물을 찾기 위해 모험을 떠났습니다. 여러 단서를 살펴보며 어려운 문제를 만났지만 친구의 도움을 받아 해결했습니다. 마지막에는 보물도 중요하지만 친구와 함께 힘을 모아 문제를 해결하는 것이 더 중요하다는 것을 깨달았습니다.", questions: [
        { index: 1, question: "처음에 주인공은 무엇을 하기 위해 모험을 시작했나요?", answer: "주인공은 보물을 찾기 위해 모험을 시작했습니다." },
        { index: 2, question: "보물을 찾는 동안 주인공에게 어떤 일이 있었나요?", answer: "여러 단서를 찾고 어려운 문제를 만났지만 친구와 함께 해결했습니다." },
        { index: 3, question: "마지막에 주인공이 깨달은 것은 무엇인가요?", answer: "혼자보다 친구와 함께 문제를 해결하는 것이 더 중요하다는 것을 깨달았습니다." }
      ], submittedAt: "2026-08-05T10:20:00" },
      { id: "practice-summary-seed-pending-02", writerLoginId: "demo_student_02", studentName: "송민정", bookType: "info", summary: "이 책은 우리 몸이 어떻게 병균과 싸우는지 알려줍니다. 처음에는 피부가 병균을 막아주고, 가운데에는 백혈구가 병균과 싸운다고 했습니다. 끝에는 손을 잘 씻는 것이 중요하다고 했습니다.", submittedAt: "2026-08-06T14:10:00" },
      { id: "practice-summary-seed-pending-03", writerLoginId: "demo_student_03", studentName: "박하민", bookType: "opinion", summary: "이 글쓴이는 학교에 도서관이 더 필요하다고 주장합니다. 처음에는 책 읽을 곳이 부족하다고 했고, 가운데에는 도서관이 생기면 좋은 점을 이야기했습니다. 끝에는 도서관을 늘려야 한다고 다시 강조했습니다.", submittedAt: "2026-08-07T09:40:00" }
    ],
    rejected: [
      { id: "practice-summary-seed-rejected-01", writerLoginId: "demo_student_04", studentName: "이진우", bookType: "story", summary: "주인공이 여행을 떠났습니다. 그리고 집으로 돌아왔습니다.", submittedAt: "2026-08-04T11:05:00", rejectionReason: "중요한 내용이 조금 빠졌어요. 가운데 부분에서 어떤 일이 있었는지 다시 읽고 고쳐보세요." }
    ]
  },
  individual: {
    approved: [
      { id: "individual-summary-seed-approved-01", writerLoginId: "ss01", studentName: "김초롱", bookType: "story", summary: "주인공은 낯선 마을에서 잃어버린 물건을 찾기 시작했습니다. 여러 단서를 살피고 친구의 도움을 받아 문제를 해결했습니다. 마지막에는 혼자보다 함께 해결하는 것이 더 중요하다는 것을 알게 되었습니다.", questions: [
        { index: 1, question: "주인공은 낯선 마을에서 무엇을 찾았나요?", answer: "잃어버린 소중한 물건을 찾았습니다." },
        { index: 2, question: "주인공은 문제를 어떻게 해결했나요?", answer: "단서를 살피고 친구의 도움을 받아 해결했습니다." },
        { index: 3, question: "주인공이 마지막에 깨달은 것은 무엇인가요?", answer: "혼자보다 함께 해결하는 것이 중요하다는 것을 알았습니다." }
      ], submittedAt: "2026-08-08T10:15:00" },
      { id: "individual-summary-seed-approved-02", writerLoginId: "demo_student_02", studentName: "송민정", bookType: "info", summary: "이 책은 바다거북이 어떻게 살아가는지 설명합니다. 바다거북은 먼 바다에서 먹이를 찾고 알을 낳기 위해 다시 해변으로 돌아옵니다. 바다 쓰레기가 바다거북에게 위험할 수 있다는 내용도 나옵니다.", submittedAt: "2026-08-09T11:25:00" },
      { id: "individual-summary-seed-approved-03", writerLoginId: "demo_student_03", studentName: "박하민", bookType: "opinion", summary: "글쓴이는 학교에서 일회용품 사용을 줄여야 한다고 주장합니다. 쓰레기를 줄이고 환경을 보호할 수 있기 때문입니다. 그래서 물병과 다회용품을 사용하는 것이 좋다고 설명합니다.", submittedAt: "2026-08-10T09:35:00" },
      { id: "individual-summary-seed-approved-04", writerLoginId: "demo_student_04", studentName: "이진우", bookType: "other", summary: "이 책에는 세계 여러 나라의 특별한 집이 소개됩니다. 날씨와 주변 환경에 따라 집의 재료와 모양이 달라집니다. 사람들은 사는 곳에 알맞게 집을 지으며 생활해 왔다는 것을 알게 되었습니다.", submittedAt: "2026-08-11T14:05:00" }
    ],
    pending: [
      { id: "individual-summary-seed-pending-01", writerLoginId: "demo_student_05", studentName: "김민지", bookType: "info", summary: "이 책은 우리 몸속 여러 기관에 대해 알려줍니다. 처음에는 심장과 뇌, 폐처럼 서로 다른 일을 하는 기관들을 소개합니다. 가운데에는 이 기관들이 따로 움직이지 않고 서로 도우며 몸을 움직인다고 했습니다. 끝에는 우리 몸의 구조와 기능을 알기 쉽게 정리해 주었습니다.", submittedAt: "2026-08-05T10:30:00" },
      { id: "individual-summary-seed-pending-02", writerLoginId: "demo_student_06", studentName: "서희원", bookType: "story", summary: "처음에는 강아지똥이 자기 모습 때문에 슬퍼하며 아무 쓸모가 없다고 생각합니다. 가운데에는 민들레가 꽃을 피우기 위해 강아지똥의 도움이 필요하다고 말합니다. 끝에는 강아지똥이 자신도 소중한 존재가 될 수 있다는 것을 알게 됩니다.", submittedAt: "2026-08-06T13:50:00" },
      { id: "individual-summary-seed-pending-03", writerLoginId: "demo_student_07", studentName: "김수진", bookType: "story", summary: "처음에는 여우가 책을 너무 좋아해서 읽은 뒤에 소금과 후추를 뿌려 먹어 버립니다. 가운데에는 책을 구하지 못하게 되어 어려움을 겪습니다. 끝에는 여우가 직접 이야기를 쓰기 시작하며 새로운 즐거움을 찾습니다.", submittedAt: "2026-08-07T09:20:00" }
    ],
    rejected: [
      { id: "individual-summary-seed-rejected-01", writerLoginId: "demo_student_08", studentName: "이혜원", bookType: "story", summary: "주인공이 여행을 떠났습니다. 그리고 집으로 돌아왔습니다.", submittedAt: "2026-08-04T11:15:00", rejectionReason: "중요한 내용이 조금 빠졌어요. 가운데 부분에서 어떤 일이 있었는지 다시 읽고 고쳐보세요." }
    ]
  }
};

function getDemoSummaryReviewSeeds(scope, initialStatus) {
  const group = DEMO_SUMMARY_REVIEW_SEEDS[scope] || {};
  const seeds = group[initialStatus] || [];
  return seeds.map(function(seed) { return Object.assign({}, seed); });
}

function getDemoFinalSummaryReviewSeeds(scope) {
  return ["approved", "pending", "rejected"].flatMap(function(initialStatus) {
    return getDemoSummaryReviewSeeds(scope, initialStatus).map(function(seed) {
      const override = getDemoSummaryReviewOverride(scope, seed.id);
      const status = override ? override.status : initialStatus;
      return Object.assign({}, seed, {
        summary: override && typeof override.summary === "string" ? override.summary : seed.summary,
        bookType: override && override.bookType ? override.bookType : seed.bookType,
        questions: override && Array.isArray(override.questions) ? override.questions : (seed.questions || []),
        status: status,
        rejectionReason: override && status === "rejected"
          ? String(override.reason || "")
          : (status === "rejected" ? String(seed.rejectionReason || "") : ""),
        reviewedAt: override ? override.updatedAt : "",
        deleted: Boolean(override && override.deleted)
      });
    });
  }).filter(function(seed) { return seed.deleted !== true; });
}

function getDemoSummaryReviewOverride(scope, summaryId) {
  const overrides = loadDemoState(DEMO_SUMMARY_REVIEW_OVERRIDE_KEY, {});
  return overrides[scope + ":" + summaryId] || null;
}

function saveDemoSummaryReviewOverride(scope, summaryId, status, reason) {
  const overrides = loadDemoState(DEMO_SUMMARY_REVIEW_OVERRIDE_KEY, {});
  const key = scope + ":" + summaryId;
  const saved = Object.assign({}, overrides[key] || {}, {
    status: status,
    reason: status === "rejected" ? String(reason || "") : "",
    updatedAt: new Date().toISOString()
  });
  overrides[key] = saved;
  saveDemoState(DEMO_SUMMARY_REVIEW_OVERRIDE_KEY, overrides);
  return saved;
}

function saveDemoSummaryRevisionOverride(scope, summaryId, summary, bookType, questions) {
  const overrides = loadDemoState(DEMO_SUMMARY_REVIEW_OVERRIDE_KEY, {});
  const key = scope + ":" + summaryId;
  overrides[key] = Object.assign({}, overrides[key] || {}, {
    status: "pending",
    reason: "",
    summary: String(summary || "").trim(),
    bookType: bookType || "",
    questions: Array.isArray(questions) ? questions : (overrides[key] || {}).questions,
    deleted: false,
    updatedAt: new Date().toISOString()
  });
  saveDemoState(DEMO_SUMMARY_REVIEW_OVERRIDE_KEY, overrides);
  return overrides[key];
}

function deleteDemoSummaryReviewSeed(scope, summaryId) {
  const overrides = loadDemoState(DEMO_SUMMARY_REVIEW_OVERRIDE_KEY, {});
  const key = scope + ":" + summaryId;
  /* tombstone만 남겨 수정 본문·거절 사유·이전 상태는 함께 폐기한다. */
  overrides[key] = {
    deleted: true,
    updatedAt: new Date().toISOString()
  };
  saveDemoState(DEMO_SUMMARY_REVIEW_OVERRIDE_KEY, overrides);
}

function clearDemoSummaryReviewOverride(scope, summaryId) {
  const overrides = loadDemoState(DEMO_SUMMARY_REVIEW_OVERRIDE_KEY, {});
  delete overrides[scope + ":" + summaryId];
  saveDemoState(DEMO_SUMMARY_REVIEW_OVERRIDE_KEY, overrides);
}

/* 연습읽기 학생·교사 화면이 같은 direct 간추리기와 같은 summaryId를
   사용하도록 mq_demo_afterReading을 한 곳에서 effective record로 만든다. */
function getDemoPracticeDirectSummary(classReadingBookId) {
  const data = loadDemoState("afterReading", {});
  const summary = String(data.summary || data.summaryText || "").trim();
  if (!summary || summary.indexOf("________________") >= 0) return null;

  const browserId = getDemoBrowserId();
  if (data.demoBrowserId && String(data.demoBrowserId) !== String(browserId)) return null;

  const id = String(data.localId || [
    "practice-summary",
    browserId,
    classReadingBookId || data.classReadingBookId || "book"
  ].join("-"));
  const override = getDemoSummaryReviewOverride("practice", id);
  if (override && override.deleted === true) return null;

  const status = String(override ? override.status : (data.status || "pending")).toLowerCase();
  return Object.assign({}, data, {
    id: id,
    localId: id,
    demoBrowserId: browserId,
    summary: override && typeof override.summary === "string" ? override.summary : summary,
    summaryText: override && typeof override.summary === "string" ? override.summary : summary,
    bookType: override && override.bookType ? override.bookType : (data.bookType || "story"),
    status: status,
    rejectionReason: status === "rejected"
      ? String(override ? override.reason || "" : data.rejectionReason || "")
      : ""
  });
}

function getDemoIndividualDirectSummaries() {
  const list = loadDemoState("individualWrittenSummaries", []);
  if (!Array.isArray(list)) return [];

  return list.map(function(item) {
    if (!item) return null;
    const summaryId = item.summaryId != null ? item.summaryId : item.id;
    const summaryText = String(item.summaryText || item.summary || "").trim();
    if (summaryId == null || !summaryText) return null;

    const override = getDemoSummaryReviewOverride("individual", String(summaryId));
    if (override && override.deleted === true) return null;
    const status = String(override ? override.status : (item.status || "pending")).toLowerCase();

    return Object.assign({}, item, {
      summaryId: String(summaryId),
      summaryText: override && typeof override.summary === "string"
        ? override.summary
        : summaryText,
      bookType: override && override.bookType ? override.bookType : (item.bookType || "other"),
      questionAnswers: override && Array.isArray(override.questions)
        ? override.questions
        : (item.questionAnswers || item.questions || []),
      status: status,
      rejectionReason: status === "rejected"
        ? String(override ? override.reason || "" : item.rejectionReason || "")
        : ""
    });
  }).filter(Boolean);
}

function getDemoBookChatOverrideStorageKey(scope) {
  return DEMO_BOOK_CHAT_OVERRIDE_STORAGE_KEYS[scope] || null;
}

function loadDemoBookChatStatusOverrides(scope) {
  const storageKey = getDemoBookChatOverrideStorageKey(scope);
  if (!storageKey) return {};
  const overrides = loadDemoState(storageKey, {});
  return overrides && typeof overrides === "object" && !Array.isArray(overrides) ? overrides : {};
}

function getDemoBookChatStatusOverride(scope, postId) {
  const overrides = loadDemoBookChatStatusOverrides(scope);
  return overrides[String(postId)] || null;
}

function saveDemoBookChatStatusOverride(scope, postId, status, reason) {
  const storageKey = getDemoBookChatOverrideStorageKey(scope);
  if (!storageKey || !postId) return null;

  const overrides = loadDemoBookChatStatusOverrides(scope);
  const saved = Object.assign({}, overrides[String(postId)] || {}, {
    status: status,
    reason: status === "rejected" ? String(reason || "") : "",
    updatedAt: new Date().toISOString()
  });
  overrides[String(postId)] = saved;
  saveDemoState(storageKey, overrides);
  return saved;
}

function saveDemoBookChatRevisionOverride(scope, postId, revision) {
  const storageKey = getDemoBookChatOverrideStorageKey(scope);
  if (!storageKey || !postId) return null;
  const overrides = loadDemoBookChatStatusOverrides(scope);
  const saved = Object.assign({}, overrides[String(postId)] || {}, revision || {}, {
    status: "pending", reason: "", updatedAt: new Date().toISOString()
  });
  overrides[String(postId)] = saved;
  saveDemoState(storageKey, overrides);
  return saved;
}

function deleteDemoBookChatPost(scope, postId) {
  const storageKey = getDemoBookChatOverrideStorageKey(scope);
  if (!storageKey || !postId) return null;
  const overrides = loadDemoBookChatStatusOverrides(scope);
  const saved = Object.assign({}, overrides[String(postId)] || {}, {
    deleted: true,
    reason: "",
    updatedAt: new Date().toISOString()
  });
  overrides[String(postId)] = saved;
  saveDemoState(storageKey, overrides);
  return saved;
}

/*
 * 교사 관리에서 승인할 수 있는 화면 전용 책수다 seed의 단일 원본.
 * 승인되면 학생 공개 목록에도 같은 id/작성자/본문이 나타나야 하므로
 * 학생·교사 HTML에 배열을 각각 복사하지 않고 이 함수만 사용한다.
 */
const DEMO_BOOK_CHAT_PENDING_SEEDS = {
  practice: [
    {
      id: "practice-seed-pending-01", studentName: "박하민", questionType: "direct",
      question: "주인공이 길을 잃었을 때 도와준 것은 누구였나요?",
      answer: "A. 우연히 만난 친구 / B. 혼자 힘으로 찾았다 (정답 A)",
      submittedAt: "2026-08-05T10:20:00"
    },
    {
      id: "practice-seed-pending-02", studentName: "서희원", questionType: "opinion",
      question: "내가 주인공이라면 끝까지 보물을 찾으러 갔을까요?",
      answer: "나는 무서워도 끝까지 가 봤을 것 같아. 궁금한 걸 그냥 두면 계속 생각날 것 같거든.",
      submittedAt: "2026-08-06T14:10:00"
    },
    {
      id: "practice-seed-pending-03", studentName: "김수진", questionType: "connect",
      question: "나도 친구와 함께 어려운 일을 해결한 적이 있나요?",
      answer: "체육 시간에 줄넘기를 못했는데 친구가 같이 연습해 줘서 성공한 적이 있어.",
      submittedAt: "2026-08-07T09:40:00"
    }
  ],
  individual: [
    {
      id: "individual-seed-pending-01", studentName: "송민정", bookTitle: "긴긴밤", title: "노든의 선택",
      scene: "노든은 위험한 길을 지나야 어린 펭귄을 지킬 수 있습니다.\n하지만 그 길은 아주 위험합니다.",
      optionA: "안전한 길로 돌아간다.", optionB: "위험해도 가장 빠른 길로 간다.", authorChoice: "B",
      submittedAt: "2026-08-05T10:20:00"
    },
    {
      id: "individual-seed-pending-02", studentName: "김민지", bookTitle: "우리 몸의 신비", title: "건강 습관 하나만 고르기",
      scene: "몸을 건강하게 만들기 위해 매일 한 가지 습관만 지킬 수 있다면 어떤 것을 고를까요?",
      optionA: "매일 운동하기", optionB: "매일 충분히 잠자기", authorChoice: "B",
      submittedAt: "2026-08-06T14:10:00"
    },
    {
      id: "individual-seed-pending-03", studentName: "이혜원", bookTitle: "초정리 편지", title: "글을 배울 기회",
      scene: "글을 배우는 것이 쉽지 않지만, 글을 알게 되면 내 생각을 다른 사람에게 전할 수 있습니다.",
      optionA: "어렵더라도 끝까지 글을 배운다.", optionB: "너무 어려우면 다른 사람이 대신 읽고 써 주게 한다.", authorChoice: "A",
      submittedAt: "2026-08-07T09:40:00"
    }
  ]
};

/* 학생·교사 개별읽기 책수다방이 함께 쓰는 공개 예시. 날짜별 direct 글과
   달리 심사 seed는 선택 날짜가 바뀌어도 항상 보인다. */
const DEMO_INDIVIDUAL_BOOK_CHAT_APPROVED_SEEDS = [
  {
    id: "individual-seed-approved-01", status: "approved", writerLoginId: "ss01",
    studentName: "김초롱", bookTitle: "긴긴밤", title: "노든이 지키고 싶었던 것",
    scene: "노든은 위험한 길에서도 어린 펭귄을 끝까지 지키려고 합니다.",
    optionA: "안전한 곳으로 먼저 피한다.", optionB: "위험해도 펭귄과 함께 간다.", authorChoice: "B",
    submittedAt: "2026-08-08T10:10:00"
  },
  {
    id: "individual-seed-approved-02", status: "approved", writerLoginId: "demo_student_02",
    studentName: "송민정", bookTitle: "바다거북의 여행", title: "바다거북을 돕는 방법",
    scene: "바다 쓰레기 때문에 바다거북이 먹이를 찾고 헤엄치는 데 어려움을 겪습니다.",
    optionA: "일회용품을 줄인다.", optionB: "쓰레기는 바다에서 저절로 없어진다.", authorChoice: "A",
    submittedAt: "2026-08-09T11:20:00"
  },
  {
    id: "individual-seed-approved-03", status: "approved", writerLoginId: "demo_student_03",
    studentName: "박하민", bookTitle: "초정리 편지", title: "어려워도 글을 배울까?",
    scene: "글을 배우는 일은 어렵지만 내 생각을 직접 전할 수 있게 해 줍니다.",
    optionA: "어려워도 끝까지 배운다.", optionB: "다른 사람이 대신 써 주게 한다.", authorChoice: "A",
    submittedAt: "2026-08-10T09:30:00"
  },
  {
    id: "individual-seed-approved-04", status: "approved", writerLoginId: "demo_student_04",
    studentName: "이진우", bookTitle: "우리 몸의 신비", title: "건강 습관 하나 고르기",
    scene: "몸을 건강하게 지키기 위해 매일 한 가지 습관을 실천하려고 합니다.",
    optionA: "충분히 자고 규칙적으로 운동한다.", optionB: "늦게까지 깨어 있는다.", authorChoice: "A",
    submittedAt: "2026-08-11T14:00:00"
  }
];

function getDemoBookChatPendingSeeds(scope) {
  const seeds = DEMO_BOOK_CHAT_PENDING_SEEDS[scope] || [];
  return seeds.map(function (seed) { return Object.assign({}, seed); });
}

function getDemoFinalIndividualBookChatSeeds() {
  return DEMO_INDIVIDUAL_BOOK_CHAT_APPROVED_SEEDS
    .concat(getDemoBookChatPendingSeeds("individual"))
    .map(function(seed) {
      const override = getDemoBookChatStatusOverride("individual", seed.id);
      return Object.assign({}, seed, override || {}, {
        status: override ? override.status : (seed.status || "pending"),
        reason: override && override.status === "rejected" ? String(override.reason || "") : ""
      });
    })
    .filter(function(seed) { return seed.deleted !== true; });
}

function getDemoTodayDateKey() {
  const now = new Date();
  const year = now.getFullYear();
  const month = String(now.getMonth() + 1).padStart(2, "0");
  const day = String(now.getDate()).padStart(2, "0");
  return year + "-" + month + "-" + day;
}

function getDemoBookChatItemDateKey(item, reviewDate) {
  if (!item) return "";
  const value = reviewDate
    || item.activityDate
    || item.createdDate
    || item.submittedAt
    || item.createdAt;
  return value ? String(value).slice(0, 10) : "";
}

/* 학생 책수다방의 기본 선택 날짜(오늘)와 교사 승인 탭의 공개 범위를 맞춘다. */
function isDemoBookChatItemOnDate(item, dateKey, reviewDate) {
  const itemDateKey = getDemoBookChatItemDateKey(item, reviewDate);
  return Boolean(itemDateKey) && itemDateKey === String(dateKey || getDemoTodayDateKey());
}

function saveDemoState(key, value) {
  try {
    localStorage.setItem(MQ_DEMO_STORAGE_PREFIX + key, JSON.stringify(value));
  } catch (error) {
    console.error("심사 체험 데이터를 저장하지 못했습니다.", error);
  }
}

function loadDemoState(key, fallback) {
  try {
    const raw = localStorage.getItem(MQ_DEMO_STORAGE_PREFIX + key);
    if (!raw) {
      return fallback;
    }
    return JSON.parse(raw);
  } catch (error) {
    console.error("심사 체험 데이터를 불러오지 못했습니다.", error);
    return fallback;
  }
}

/*
  "심사 체험 기록 초기화" 버튼에서 호출한다. mq_demo_ prefix가 붙은
  localStorage 키만 지우므로, 로그인/인증용 sessionStorage 키나 DB seed
  데이터에는 영향을 주지 않는다. (예전 이름을 그대로 유지 — 로그아웃 시
  자동 호출하던 것을 이번 변경으로 그만두고, 사용자가 직접 누를 때만 쓴다.)
*/
function clearAllDemoState() {
  Object.keys(localStorage)
    .filter(function (key) {
      return key.indexOf(MQ_DEMO_STORAGE_PREFIX) === 0;
    })
    .forEach(function (key) {
      localStorage.removeItem(key);
    });
}

/*
  예전 sessionStorage 심사 체험 데이터를 localStorage로 1회 이전한다.
  localStorage에 이미 같은 키가 있으면 건드리지 않는다(중복/덮어쓰기 방지).
*/
(function migrateSessionDemoStateToLocalStorage() {
  try {
    Object.keys(sessionStorage)
      .filter(function (key) {
        return key.indexOf(MQ_DEMO_STORAGE_PREFIX) === 0;
      })
      .forEach(function (key) {
        if (localStorage.getItem(key) === null) {
          localStorage.setItem(key, sessionStorage.getItem(key));
        }
      });
  } catch (error) {
    console.error("심사 체험 데이터를 이전하지 못했습니다.", error);
  }
})();

/*
  예전 심사 seed를 교사가 검토하거나 학생이 수정한 적이 있으면 원본 seed가
  아니라 summaryReviewOverrides에 본문/질문이 복사돼 남아 있을 수 있다.
  사용자가 직접 쓴 글은 보호하고, 과거 기본 문구와 정확히 같은 값만
  「나만의 보물 찾기」 최신 seed로 바꾼다.
*/
(function migrateOldPracticeSummarySeed() {
  const oldSummary = "처음에는 노든이 혼자 사막을 걷다가 아기 펭귄을 만났습니다. 가운데에는 노든이 아기 펭귄에게 헤엄치는 법을 알려주려고 애썼습니다. 끝에는 둘이 함께 바다를 향해 걸어갔습니다.";
  const newSummary = "주인공은 보물을 찾기 위해 모험을 떠났습니다. 여러 단서를 살펴보며 어려운 문제를 만났지만 친구의 도움을 받아 해결했습니다. 마지막에는 보물도 중요하지만 친구와 함께 힘을 모아 문제를 해결하는 것이 더 중요하다는 것을 깨달았습니다.";
  const oldQuestions = [
    { index: 1, question: "노든은 사막에서 누구를 만났나요?", answer: "혼자 남은 아기 펭귄을 만났습니다." },
    { index: 2, question: "노든은 아기 펭귄을 위해 무엇을 했나요?", answer: "헤엄치는 방법을 알려주며 함께 바다로 갔습니다." },
    { index: 3, question: "두 주인공은 마지막에 어디로 향했나요?", answer: "서로 의지하며 바다를 향해 걸어갔습니다." }
  ];
  const newQuestions = [
    { index: 1, question: "처음에 주인공은 무엇을 하기 위해 모험을 시작했나요?", answer: "주인공은 보물을 찾기 위해 모험을 시작했습니다." },
    { index: 2, question: "보물을 찾는 동안 주인공에게 어떤 일이 있었나요?", answer: "여러 단서를 찾고 어려운 문제를 만났지만 친구와 함께 해결했습니다." },
    { index: 3, question: "마지막에 주인공이 깨달은 것은 무엇인가요?", answer: "혼자보다 친구와 함께 문제를 해결하는 것이 더 중요하다는 것을 깨달았습니다." }
  ];

  try {
    const overrides = loadDemoState(DEMO_SUMMARY_REVIEW_OVERRIDE_KEY, {});
    const key = "practice:practice-summary-seed-pending-01";
    const override = overrides && overrides[key];
    if (!override) return;

    let changed = false;
    if (override.summary === oldSummary) {
      override.summary = newSummary;
      changed = true;
    }
    if (JSON.stringify(override.questions) === JSON.stringify(oldQuestions)) {
      override.questions = newQuestions;
      changed = true;
    }
    if (changed) saveDemoState(DEMO_SUMMARY_REVIEW_OVERRIDE_KEY, overrides);
  } catch (error) {
    console.error("예전 연습읽기 간추리기 seed를 이전하지 못했습니다.", error);
  }
})();

/*
  ============================================================
  교사 메인 대시보드 "승인대기 n개" 배지
  ------------------------------------------------------------
  book-chat-manage.html / summary-manage.html /
  individual-book-chat-manage.html / individual-summary-manage.html은
  각자 화면 안에서 "실제 DB 항목 + 화면 전용 seed + ss01이 이 브라우저에서
  직접 쓴 글" 세 가지를 합쳐 승인대기 개수를 보여준다. 교사 메인 화면
  (book-manage.html / individual-reading-manage.html) 메뉴 버튼의 배지도
  같은 숫자를 보여줘야 하므로, 그 계산 로직만 그대로 옮겨 "개수"만
  돌려준다(화면 렌더링은 하지 않음).

  seed id는 각 관리 화면의 것과 완전히 같아야 override 조회가
  어긋나지 않는다(승인/거절 override는 id를 키로 저장되기 때문) - 텍스트
  내용까지는 필요 없고 개수 판정에는 id만 있으면 되므로 id만 복사했다.
  관리 화면 쪽 seed id가 바뀌면 여기도 같이 바꿔야 한다.
  ============================================================
*/
const TEACHER_BADGE_SEED_IDS = {
  practiceBookChat: getDemoBookChatPendingSeeds("practice").map(function (seed) { return seed.id; }),
  practiceSummaryPending: ["practice-summary-seed-pending-01", "practice-summary-seed-pending-02", "practice-summary-seed-pending-03"],
  practiceSummaryRejected: ["practice-summary-seed-rejected-01"],
  individualBookChat: getDemoBookChatPendingSeeds("individual").map(function (seed) { return seed.id; }),
  individualSummaryPending: ["individual-summary-seed-pending-01", "individual-summary-seed-pending-02", "individual-summary-seed-pending-03"],
  individualSummaryRejected: ["individual-summary-seed-rejected-01"]
};

function getTeacherBadgeAuthContext() {
  const token = sessionStorage.getItem("token");
  const teacherId = sessionStorage.getItem("id") || sessionStorage.getItem("studentId");
  const role = sessionStorage.getItem("role");
  if (!token || !teacherId || role !== "teacher") return null;
  return { token: token, teacherId: teacherId };
}

async function fetchTeacherBadgeList(url, auth) {
  const response = await fetch(url, { headers: { Authorization: "Bearer " + auth.token } });
  if (!response.ok) throw new Error("승인대기 개수를 불러오지 못했습니다: " + url);
  return response.json();
}

/*
  연습읽기 책수다방 - book-chat-manage.html의 loadBackendReviewPosts()와
  같은 판정: 실제 DB 행은 override가 없으면 항상 "approved"로 본다(심사
  학급 DB 예시는 이미 승인 상태로 심어져 있고, "승인대기" 체험은 seed와
  ss01 직접 작성 글에서만 나오기 때문) - 이 부분은 book-chat-manage.html의
  기존 동작을 그대로 따른 것이지 이 배지에서 새로 정한 규칙이 아니다.
*/
async function getPracticeBookChatPendingCount(apiBaseUrl) {
  const auth = getTeacherBadgeAuthContext();
  if (!auth) return 0;

  const statusUrl = function (status) {
    return apiBaseUrl + "/api/teachers/" + auth.teacherId + "/book-thought-reviews?status=" + encodeURIComponent(status);
  };

  if (!isDemoAccount()) {
    try {
      const items = await fetchTeacherBadgeList(statusUrl("PENDING"), auth);
      return Array.isArray(items) ? items.length : 0;
    } catch (error) {
      console.warn("연습읽기 책수다방 승인대기 개수를 불러오지 못했습니다.", error);
      return 0;
    }
  }

  let count = 0;

  try {
    const results = await Promise.all([
      fetchTeacherBadgeList(statusUrl("PENDING"), auth),
      fetchTeacherBadgeList(statusUrl("APPROVED"), auth),
      fetchTeacherBadgeList(statusUrl("REJECTED"), auth)
    ]);
    results[0].concat(results[1]).concat(results[2]).forEach(function (item) {
      const override = getDemoBookChatStatusOverride("practice", String(item.id));
      if (override && override.deleted === true) return;
      const status = override ? override.status : "approved";
      if (status === "pending") count++;
    });
  } catch (error) {
    console.warn("심사 학급의 실제 책수다방 글을 불러오지 못했습니다.", error);
  }

  TEACHER_BADGE_SEED_IDS.practiceBookChat.forEach(function (id) {
    const override = getDemoBookChatStatusOverride("practice", id);
    if (override && override.deleted === true) return;
    const status = override ? override.status : "pending";
    if (status === "pending") count++;
  });

  loadDemoApprovalRequests("practice").forEach(function (request) {
    const status = ["pending", "approved", "rejected"].indexOf(request.status) >= 0 ? request.status : "pending";
    if (status === "pending") count++;
  });

  return count;
}

/*
  연습읽기 간추리기 - summary-manage.html의 loadReviewSummaries()와 같은
  판정. 실제 DB 행은 (책수다방과 달리) override 없이 자기 status를 그대로
  믿는다 - summary-manage.html도 그렇게 동작한다.
*/
async function getPracticeSummaryPendingCount(apiBaseUrl, classId) {
  const auth = getTeacherBadgeAuthContext();
  if (!auth || !classId) return 0;

  const statusUrl = function (status) {
    return apiBaseUrl + "/api/teachers/" + auth.teacherId + "/classes/" + classId + "/after-reading/summaries/review?status=" + encodeURIComponent(status);
  };

  if (!isDemoAccount()) {
    try {
      const items = await fetchTeacherBadgeList(statusUrl("pending"), auth);
      return Array.isArray(items) ? items.length : 0;
    } catch (error) {
      console.warn("연습읽기 간추리기 승인대기 개수를 불러오지 못했습니다.", error);
      return 0;
    }
  }

  let count = 0;

  try {
    const results = await Promise.all([
      fetchTeacherBadgeList(statusUrl("pending"), auth),
      fetchTeacherBadgeList(statusUrl("approved"), auth),
      fetchTeacherBadgeList(statusUrl("rejected"), auth)
    ]);
    results[0].concat(results[1]).concat(results[2]).forEach(function (item) {
      const override = getDemoSummaryReviewOverride("practice", String(item.summaryId || item.id));
      if (override && override.deleted === true) return;
      const status = String(override ? override.status : (item.status || "pending")).toLowerCase();
      if (status === "pending") count++;
    });
  } catch (error) {
    console.warn("심사 학급의 실제 간추리기를 불러오지 못했습니다.", error);
  }

  TEACHER_BADGE_SEED_IDS.practiceSummaryPending.forEach(function (id) {
    const override = getDemoSummaryReviewOverride("practice", id);
    if (override && override.deleted === true) return;
    const status = override ? override.status : "pending";
    if (status === "pending") count++;
  });

  TEACHER_BADGE_SEED_IDS.practiceSummaryRejected.forEach(function (id) {
    const override = getDemoSummaryReviewOverride("practice", id);
    if (override && override.deleted === true) return;
    const status = override ? override.status : "rejected";
    if (status === "pending") count++;
  });

  /*
   * ss01이 이 브라우저에서 직접 쓴 간추리기(있으면) - after-read.html의
   * getDemoLocalSharedSummary()와 완전히 같은 방식으로 localId를
   * 계산해야 같은 override를 가리킨다.
   */
  let classReadingBookId = null;
  try {
    const readingBookResponse = await fetch(apiBaseUrl + "/api/classes/" + classId + "/reading-range", {
      headers: { Authorization: "Bearer " + auth.token }
    });
    if (readingBookResponse.ok) {
      const readingBook = await readingBookResponse.json();
      classReadingBookId = readingBook.id;
    }
  } catch (error) {
    console.warn("현재 온책읽기 도서 정보를 불러오지 못했습니다.", error);
  }

  const directSummary = getDemoPracticeDirectSummary(classReadingBookId);
  if (directSummary && directSummary.status === "pending") count++;

  return count;
}

/*
  개별읽기 책수다방 - individual-book-chat-manage.html의
  loadBackendReviewPosts()와 같은 판정(실제 DB 행은 override 없으면
  "approved").
*/
async function getIndividualBookChatPendingCount(apiBaseUrl) {
  const auth = getTeacherBadgeAuthContext();
  if (!auth) return 0;

  const statusUrl = function (status) {
    return apiBaseUrl + "/api/teachers/" + auth.teacherId + "/individual-reading/book-chat/posts?status=" + encodeURIComponent(status);
  };

  if (!isDemoAccount()) {
    try {
      const items = await fetchTeacherBadgeList(statusUrl("PENDING"), auth);
      return Array.isArray(items) ? items.length : 0;
    } catch (error) {
      console.warn("개별읽기 책수다방 승인대기 개수를 불러오지 못했습니다.", error);
      return 0;
    }
  }

  let count = 0;

  try {
    const results = await Promise.all([
      fetchTeacherBadgeList(statusUrl("PENDING"), auth),
      fetchTeacherBadgeList(statusUrl("APPROVED"), auth),
      fetchTeacherBadgeList(statusUrl("REJECTED"), auth)
    ]);
    results[0].concat(results[1]).concat(results[2]).forEach(function (item) {
      const override = getDemoBookChatStatusOverride("individual", String(item.id));
      if (override && override.deleted === true) return;
      const status = override ? override.status : "approved";
      if (status === "pending") count++;
    });
  } catch (error) {
    console.warn("심사 학급의 실제 개별읽기 책수다방 글을 불러오지 못했습니다.", error);
  }

  TEACHER_BADGE_SEED_IDS.individualBookChat.forEach(function (id) {
    const override = getDemoBookChatStatusOverride("individual", id);
    if (override && override.deleted === true) return;
    const status = override ? override.status : "pending";
    if (status === "pending") count++;
  });

  loadDemoApprovalRequests("individual").forEach(function (request) {
    const status = ["pending", "approved", "rejected"].indexOf(request.status) >= 0 ? request.status : "pending";
    if (status === "pending") count++;
  });

  return count;
}

/*
  개별읽기 간추리기 - individual-summary-manage.html의
  loadReviewSummaries()와 같은 판정. ss01이 직접 쓴 글은
  mq_demo_individualWrittenSummaries 배열 자체가 source of truth다
  (별도 override 계층 없음 - 그 화면과 동일).
*/
async function getIndividualSummaryPendingCount(apiBaseUrl) {
  const auth = getTeacherBadgeAuthContext();
  if (!auth) return 0;

  const statusUrl = function (status) {
    return apiBaseUrl + "/api/teachers/" + auth.teacherId + "/individual-reading/summaries/review?status=" + encodeURIComponent(status);
  };

  if (!isDemoAccount()) {
    try {
      const items = await fetchTeacherBadgeList(statusUrl("pending"), auth);
      return Array.isArray(items) ? items.length : 0;
    } catch (error) {
      console.warn("개별읽기 간추리기 승인대기 개수를 불러오지 못했습니다.", error);
      return 0;
    }
  }

  let count = 0;

  try {
    const results = await Promise.all([
      fetchTeacherBadgeList(statusUrl("pending"), auth),
      fetchTeacherBadgeList(statusUrl("approved"), auth),
      fetchTeacherBadgeList(statusUrl("rejected"), auth)
    ]);
    results[0].concat(results[1]).concat(results[2]).forEach(function (item) {
      const override = getDemoSummaryReviewOverride("individual", String(item.summaryId || item.id));
      if (override && override.deleted === true) return;
      const status = String(override ? override.status : (item.status || "pending")).toLowerCase();
      if (status === "pending") count++;
    });
  } catch (error) {
    console.warn("심사 학급의 실제 개별읽기 간추리기를 불러오지 못했습니다.", error);
  }

  TEACHER_BADGE_SEED_IDS.individualSummaryPending.forEach(function (id) {
    const override = getDemoSummaryReviewOverride("individual", id);
    if (override && override.deleted === true) return;
    const status = override ? override.status : "pending";
    if (status === "pending") count++;
  });

  TEACHER_BADGE_SEED_IDS.individualSummaryRejected.forEach(function (id) {
    const override = getDemoSummaryReviewOverride("individual", id);
    if (override && override.deleted === true) return;
    const status = override ? override.status : "rejected";
    if (status === "pending") count++;
  });

  getDemoIndividualDirectSummaries().forEach(function (item) {
    if (!item || !item.summaryText) return;
    const status = String(item.status || "pending").toLowerCase();
    if (status === "pending") count++;
  });

  return count;
}
