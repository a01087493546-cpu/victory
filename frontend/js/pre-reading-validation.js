/*
  읽기 전(제목/차례/그림/글) 질문·답 1차 형식 검사 공통 함수.
  before-reading.html(연습읽기 읽기 전)과 individual-before-reading.html
  (개별읽기 읽기 전)이 함께 사용한다. AI를 호출하기 전에 빈 입력, 초성만
  입력, 반복 글자/패턴, 너무 짧은 입력, 질문 형식이 아닌 입력, 성의 없는
  답을 걸러낸다. 여기를 통과한 입력만 FeedbackAiService의
  pre_reading_question 프롬프트로 보내 의미 판단을 받는다.

  이름 충돌을 피하기 위해 모든 함수 이름에 preReading 접두사를 붙였다.
*/

/* 공백/구두점을 지운 나머지가 한글 자모(초성·모음)뿐이거나, 한글/영문 글자가 하나도 없으면 의미 없는 입력으로 본다. */
function preReadingIsJamoOrSymbolOnly(text) {
  const compact = text.replace(/[\s.?!~,·…ㆍ]/g, "");

  if (!compact) {
    return true;
  }

  const jamoOnlyPattern = /^[ㄱ-ㅎㅏ-ㅣ]+$/;
  const hasRealLetter = /[가-힣a-zA-Z]/;

  return jamoOnlyPattern.test(compact) || !hasRealLetter.test(compact);
}

/* 같은 글자 3번 이상 반복(가가가가가), 짧은 패턴 반복(abcabcabc, asdfasdf)을 잡아낸다. */
function preReadingHasExcessiveRepetition(text) {
  const compact = text.replace(/[\s.?!~,·…ㆍ]/g, "");

  if (!compact) {
    return false;
  }

  if (/(.)\1{2,}/.test(compact)) {
    return true;
  }

  if (/^(.{2,6})\1+$/.test(compact)) {
    return true;
  }

  return false;
}

function preReadingCountWithoutSpaces(text) {
  return text.replace(/\s/g, "").length;
}

/*
  물음표가 아니라 질문형 표현(왜/무엇/어떻게 등)이나 질문형 어미(~까, ~나요 등)로
  질문인지 판단한다. 이 목록에 하나도 걸리지 않으면 질문으로 보지 않는다.
*/
function preReadingLooksLikeQuestion(text) {
  const questionWordPattern = /왜|무엇|무슨|뭐|누구|누가|어디|언제|어떻게|어떤|어느|몇/;

  if (questionWordPattern.test(text)) {
    return true;
  }

  const stripped = text.replace(/[\s.?!~,·…ㆍ]+$/g, "");
  const questionEndingPattern = /(까요?|나요|가요|는지|은지|인지|일지|인가|한가)$/;

  return questionEndingPattern.test(stripped);
}

/*
  질문·답 1차 형식 검사.
  emptyQuestionMessage: 질문이 비어 있을 때 보여줄, 화면/단계별 안내 문구
  (호출하는 화면에서 단계에 맞는 문구를 만들어 전달한다).
*/
function validatePreReadingQuestionAndAnswer(question, expectedAnswer, emptyQuestionMessage) {
  if (!question) {
    return {
      ok: false,
      failedRule: "INVALID_INPUT",
      message: emptyQuestionMessage
    };
  }

  if (preReadingIsJamoOrSymbolOnly(question)) {
    return {
      ok: false,
      failedRule: "INVALID_INPUT",
      message: "질문의 뜻이 잘 보이지 않아요. 궁금한 점을 문장으로 다시 적어 보세요."
    };
  }

  if (preReadingHasExcessiveRepetition(question)) {
    return {
      ok: false,
      failedRule: "REPEATED_INPUT",
      message: "같은 글자가 너무 많이 반복됐어요. 궁금한 내용을 문장으로 적어 보세요."
    };
  }

  if (preReadingCountWithoutSpaces(question) < 5) {
    return {
      ok: false,
      failedRule: "TOO_SHORT",
      message: "질문이 너무 짧아요. 무엇이 궁금한지 조금 더 자세히 적어 보세요."
    };
  }

  if (!preReadingLooksLikeQuestion(question)) {
    return {
      ok: false,
      failedRule: "NOT_A_QUESTION",
      message: "질문의 뜻이 잘 보이지 않아요. 궁금한 점을 문장으로 다시 적어 보세요."
    };
  }

  if (!expectedAnswer) {
    return {
      ok: false,
      failedRule: "INVALID_INPUT",
      message: "내가 생각한 답도 적어 줘. 아직 읽기 전이니까 상상해서 답해도 괜찮아!"
    };
  }

  if (preReadingIsJamoOrSymbolOnly(expectedAnswer)) {
    return {
      ok: false,
      failedRule: "INVALID_INPUT",
      message: "답의 뜻이 잘 보이지 않아요. 내가 예상한 답을 문장으로 다시 적어 보세요."
    };
  }

  if (preReadingHasExcessiveRepetition(expectedAnswer)) {
    return {
      ok: false,
      failedRule: "REPEATED_INPUT",
      message: "같은 글자가 너무 많이 반복됐어요. 예상한 답을 문장으로 적어 보세요."
    };
  }

  if (preReadingCountWithoutSpaces(expectedAnswer) < 3) {
    return {
      ok: false,
      failedRule: "TOO_SHORT",
      message: "답이 너무 짧아요. 왜 그렇게 생각했는지 조금 더 자세히 적어 보세요."
    };
  }

  const normalizedQuestion = question.replace(/\s/g, "");
  const normalizedAnswer = expectedAnswer.replace(/\s/g, "");

  if (normalizedQuestion === normalizedAnswer) {
    return {
      ok: false,
      failedRule: "LOW_EFFORT_ANSWER",
      message: "질문을 그대로 답으로 적었어요. 내가 예상한 답을 새로 생각해서 적어 보세요."
    };
  }

  const weakAnswers = ["몰라", "모르겠다", "없다", "없음", "그냥", "모름", "아무거나"];

  if (weakAnswers.includes(normalizedAnswer)) {
    return {
      ok: false,
      failedRule: "LOW_EFFORT_ANSWER",
      message: "조금 더 상상해서 답해 보자. 정답이 아니어도 괜찮으니까 ‘~일 것 같다’처럼 써 보면 좋아!"
    };
  }

  return {
    ok: true,
    message: "형식을 확인했어요. 루미에게 검사를 부탁할게요!"
  };
}

/*
  읽기 전 AI 피드백 요청 공통 함수 (before-reading.html / individual-before-reading.html 공용).
  AbortController + 타임아웃으로 "루미가 답변을 읽고 있어요..."에서 무한정
  멈추는 문제를 막고, 중복 클릭도 막는다. 백엔드 FeedbackAiService의
  pre_reading_question 타입(연습읽기와 동일한 프롬프트)을 그대로 재사용한다.

  guard: { isRequesting, setRequesting(bool) } 형태의 상태 객체(화면마다
  전역 변수가 다르므로 게터/세터로 넘겨받는다).
  onResult({ ok, isGood, message }): 결과를 화면에 반영하는 콜백.
*/
function requestPreReadingAiFeedback(options) {
  const stepType = options.stepType;
  const bookTitle = options.bookTitle || "";
  const question = options.question;
  const answer = options.answer;

  /* 연습읽기/개별읽기 성취도 기록용 값. classReadingBookId는 연습읽기,
     readingRecordId는 개별읽기 화면만 채워 보낸다(둘 중 하나만 값이 있음). */
  const activityType = options.activityType;
  const questionType = options.questionType;
  const evaluationKey = options.evaluationKey;
  const classReadingBookId = options.classReadingBookId;
  const readingRecordId = options.readingRecordId;

  const requestButton = options.requestButton;
  const onLoading = options.onLoading;
  const onResult = options.onResult;
  const guard = options.guard;
  const timeoutMs = options.timeoutMs || 15000;
  const failMessage =
    options.failMessage ||
    "피드백을 불러오지 못했어요. 잠시 후 다시 해 보세요.";

  if (guard.isRequesting()) {
    return Promise.resolve();
  }

  guard.setRequesting(true);

  if (requestButton) {
    requestButton.disabled = true;
  }

  if (onLoading) {
    onLoading();
  }

  const abortController = new AbortController();

  const timeoutId = setTimeout(function () {
    abortController.abort();
  }, timeoutMs);

  /*
    evaluationKey가 있으면 인증 API(연습읽기 또는 개별읽기)를 사용한다.
    evaluationKey가 없으면 공개 API를 유지한다(아직 전환하지 않은 화면).
  */
  const isAuthenticatedRequest =
    typeof evaluationKey === "string" &&
    evaluationKey.trim() !== "";

  const token = sessionStorage.getItem("token");

  if (isAuthenticatedRequest && !token) {
    clearTimeout(timeoutId);
    guard.setRequesting(false);

    if (requestButton) {
      requestButton.disabled = false;
    }

    if (onResult) {
      onResult({
        ok: false,
        isGood: false,
        message: "로그인 정보가 없어요. 다시 로그인해 주세요."
      });
    }

    return Promise.resolve();
  }

  /*
    classReadingBookId(연습읽기)도 readingRecordId(개별읽기)도 없이는
    서버가 어떤 책에 대한 검사인지 판단할 수 없다 - 엉뚱한 공용 키로
    시도 이력을 남기지 않도록 여기서 미리 막는다.
  */
  if (isAuthenticatedRequest && !classReadingBookId && !readingRecordId) {
    clearTimeout(timeoutId);
    guard.setRequesting(false);

    if (requestButton) {
      requestButton.disabled = false;
    }

    if (onResult) {
      onResult({
        ok: false,
        isGood: false,
        message: "진행 중인 책 정보를 찾을 수 없어요. 책읽기 화면에서 다시 시도해 주세요."
      });
    }

    return Promise.resolve();
  }

  const apiPath = isAuthenticatedRequest
    ? "/api/students/me/feedback/ai-review"
    : "/api/feedback/ai-review";

  const headers = {
    "Content-Type": "application/json"
  };

  if (isAuthenticatedRequest) {
    headers.Authorization = "Bearer " + token;
  }

  /*
    인증 API와 기존 공개 API가 받는 요청 형식이 다르므로
    화면 종류에 따라 body를 나누어 만든다.
  */
  const requestBody = isAuthenticatedRequest
    ? {
        question: question,
        answer: answer,
        activityType: activityType,
        questionType: questionType,
        evaluationKey: evaluationKey,
        bookTitle: bookTitle,
        classReadingBookId: classReadingBookId,
        readingRecordId: readingRecordId
      }
    : {
        type: "pre_reading_question",
        stepType: stepType,
        bookTitle: bookTitle,
        qaList: [
          {
            question: question,
            answer: answer
          }
        ]
      };

  return fetch(PRE_READING_AI_API_BASE_URL + apiPath, {
    method: "POST",
    headers: headers,
    signal: abortController.signal,
    body: JSON.stringify(requestBody)
  })
    .then(function (response) {
      if (!response.ok) {
        return response.text().then(function (bodyText) {
          console.error(
            "읽기 전 AI 피드백 요청 실패 - status:",
            response.status,
            "body:",
            bodyText
          );

          throw new Error("읽기 전 AI 피드백 요청 실패");
        });
      }

      return response.json();
    })
    .then(function (result) {
      /*
        기존 응답은 result, 일부 응답은 status를 사용할 수 있으므로
        두 필드를 모두 확인한다.
      */
      const feedbackStatus = result.status || result.result;
      const isGood = feedbackStatus === "good";

      if (onResult) {
        onResult({
          ok: true,
          isGood: isGood,
          message:
            result.message ||
            (isGood
              ? "잘했어요! 질문과 예상한 답이 자연스럽게 이어져요."
              : "질문과 답이 서로 잘 이어지지 않아요. 질문에서 물어본 내용에 맞게 다시 예상해 보세요.")
        });
      }
    })
    .catch(function (error) {
      if (error.name === "AbortError") {
        console.error(
          "읽기 전 AI 피드백 요청이 시간 초과되었습니다.",
          error
        );
      } else {
        console.error(
          "읽기 전 AI 피드백 요청 중 오류가 발생했습니다.",
          error
        );
      }

      if (onResult) {
        onResult({
          ok: false,
          isGood: false,
          message: failMessage
        });
      }
    })
    .finally(function () {
      clearTimeout(timeoutId);
      guard.setRequesting(false);

      if (requestButton) {
        requestButton.disabled = false;
      }
    });
}

const PRE_READING_AI_API_BASE_URL = "http://localhost:8080";
