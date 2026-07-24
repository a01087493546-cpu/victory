const API_BASE_URL = "http://localhost:8080";

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

  const requestBody = {
    question: options.question || "",
    answer: options.answer || "",
    activityType: options.activityType,
    questionType: options.questionType,
    evaluationKey: options.evaluationKey,
    classReadingBookId: options.classReadingBookId,
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
