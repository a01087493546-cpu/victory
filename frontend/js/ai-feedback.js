function getApiBaseUrl() {
  if (window.location.hostname === "127.0.0.1") {
    return "http://127.0.0.1:8080";
  }
  if (window.location.hostname === "localhost") {
    return "http://localhost:8080";
  }
  return "https://victory-production-f94d.up.railway.app";
}

const API_BASE_URL = getApiBaseUrl();

function requestAiFeedback(type, bookType, qaList, questionNumber, summaryText) {
  const requestBody = { type: type, bookType: bookType, qaList: qaList || [] };

  if (typeof questionNumber === "number") {
    requestBody.question_number = questionNumber;
  }

  if (typeof summaryText === "string") {
    requestBody.summaryText = summaryText;
  }

  return fetch(API_BASE_URL + "/api/feedback/ai-review", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(requestBody)
  }).then(function(response) {
    if (!response.ok) {
      throw new Error("AI 피드백 요청 실패: " + response.status);
    }
    return response.json();
  });
}

/*
  연습읽기 읽기 후(after-read.html) 전용 인증 AI 피드백 요청.
  위 requestAiFeedback(공개 API)은 individual-after-reading.html 등 다른
  화면이 계속 쓰므로 그대로 두고, 이 함수는 evaluationKey/classReadingBookId를
  받아 인증 API(/api/students/me/feedback/ai-review)로만 보낸다 - 호출하는
  화면이 명시적으로 이 함수를 부를 때만 인증 기록이 남는다.

  options: {
    activityType, questionType, evaluationKey, classReadingBookId,
    bookType, bookTitle, passage,
    question, answer,           // book_question 등 단일 질문·답 유형
    qaList, summaryText         // final_summary 전용
  }
*/
function requestAuthenticatedAiFeedback(options) {
  const token = sessionStorage.getItem("token");

  if (!token) {
    return Promise.reject(new Error("로그인 정보가 없습니다."));
  }

  /*
    classReadingBookId(연습읽기)도 readingRecordId(개별읽기)도 없이는
    서버가 어떤 책에 대한 검사인지 판단할 수 없다 - 엉뚱한 공용 키로
    시도 이력을 남기지 않도록 여기서 미리 막는다.
  */
  if (!options.classReadingBookId && !options.readingRecordId) {
    return Promise.reject(new Error("classReadingBookId 또는 readingRecordId가 필요합니다."));
  }

  /*
    개별읽기 심사계정은 여러 심사위원이 같은 ss01을 동시에 쓰는 충돌을
    피하려고 공용 DB에 책을 등록하지 않고 "demo-" + timestamp 같은
    브라우저 로컬 가짜 id를 쓴다(individual-before-reading.html 참고).
    서버 DTO의 readingRecordId는 Long이라 이런 문자열을 그대로 보내면
    JSON 역직렬화 단계에서 400으로 튕겨 나가 AI 판정 자체가 실패한다
    (실패 메시지: "루미가 지금 답을 읽지 못했어"). 숫자가 아닌 값이면
    보내지 않는다 - 서버는 심사계정이면 classReadingBookId/readingRecordId
    없이도 판정을 내려주도록 이미 되어 있다(FeedbackAiService.validateEvaluationScope).
  */
  const numericReadingRecordId =
    options.readingRecordId != null && /^\d+$/.test(String(options.readingRecordId))
      ? Number(options.readingRecordId)
      : undefined;

  const requestBody = {
    question: options.question || "",
    answer: options.answer || "",
    activityType: options.activityType,
    questionType: options.questionType,
    evaluationKey: options.evaluationKey,
    classReadingBookId: options.classReadingBookId,
    readingRecordId: numericReadingRecordId,
    bookType: options.bookType,
    bookTitle: options.bookTitle,
    passage: options.passage,
    qaList: options.qaList,
    summaryText: options.summaryText
  };

  return fetch(API_BASE_URL + "/api/students/me/feedback/ai-review", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      Authorization: "Bearer " + token
    },
    body: JSON.stringify(requestBody)
  }).then(function(response) {
    if (!response.ok) {
      throw new Error("AI 피드백 요청 실패: " + response.status);
    }
    return response.json();
  });
}
