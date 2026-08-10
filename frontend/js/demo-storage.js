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
