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
