package com.victory.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

import com.victory.dto.AiFeedbackRequest;
import com.victory.dto.AiFeedbackResponse;
import com.victory.dto.PortfolioAiAnalysisResponse;
import com.victory.entity.AiEvaluationAttempt;
import com.victory.entity.ClassReadingBook;
import com.victory.entity.ClassStudent;
import com.victory.entity.ReadingRecord;
import com.victory.entity.SchoolClass;
import com.victory.entity.User;
import com.victory.repository.AiEvaluationAttemptRepository;
import com.victory.repository.ClassReadingBookRepository;
import com.victory.repository.ClassStudentRepository;
import com.victory.repository.ReadingRecordRepository;

/*
 * openaiApiKey는 @Value로 주입되는데 이 테스트는 Spring 컨텍스트 없이 만들어서
 * 항상 null이다. 실제 AI 호출(성공 경로)을 테스트해야 하는 경우는
 * restTemplate(패키지 접근으로 열어 둠)을 모킹해 canned 응답을 돌려주고,
 * "AI 호출 자체가 실패하는 경우"를 재현해야 하는 기존 테스트들은 모킹 없이
 * 그대로 둔다(null 토큰으로 실제 RestTemplate이 호출을 시도하다 실패함).
 */
@ExtendWith(MockitoExtension.class)
class FeedbackAiServiceTest {

    private static final Long CLASS_ID = 5L;
    private static final Long BOOK_ID = 200L;
    private static final Long STUDENT_ID = 42L;
    private static final Long READING_RECORD_ID = 700L;

    @Mock
    private AiEvaluationAttemptRepository aiEvaluationAttemptRepository;

    @Mock
    private ClassStudentRepository classStudentRepository;

    @Mock
    private ClassReadingBookRepository classReadingBookRepository;

    @Mock
    private ReadingRecordRepository readingRecordRepository;

    @Mock
    private DemoAccountService demoAccountService;

    private FeedbackAiService service;

    @BeforeEach
    void setUp() {
        service = new FeedbackAiService(
            demoAccountService,
            aiEvaluationAttemptRepository,
            classStudentRepository,
            classReadingBookRepository,
            readingRecordRepository);

        // 검증 6/7/getFeedback류 테스트는 classReadingBookId 검증을 통과한
        // 뒤 AI 호출 단계에서 실패하는 상황을 재현하므로, 학생-학급-책이
        // 정상적으로 일치하는 기본 스텁을 lenient로 깔아 둔다.
        SchoolClass schoolClass = new SchoolClass();
        schoolClass.setId(CLASS_ID);

        ClassStudent classStudent = new ClassStudent();
        classStudent.setSchoolClass(schoolClass);

        ClassReadingBook classReadingBook = new ClassReadingBook();
        classReadingBook.setId(BOOK_ID);
        classReadingBook.setSchoolClass(schoolClass);

        lenient().when(classStudentRepository.findByStudentId(any()))
            .thenReturn(Optional.of(classStudent));
        lenient().when(classReadingBookRepository.findById(BOOK_ID))
            .thenReturn(Optional.of(classReadingBook));

        User owner = new User();
        owner.setId(STUDENT_ID);
        ReadingRecord readingRecord = new ReadingRecord();
        readingRecord.setId(READING_RECORD_ID);
        readingRecord.setStudent(owner);

        lenient().when(readingRecordRepository.findById(READING_RECORD_ID))
            .thenReturn(Optional.of(readingRecord));
    }

    private AiFeedbackRequest buildRequest(String type, String stepType) {
        AiFeedbackRequest request = new AiFeedbackRequest();
        request.setType(type);
        request.setStepType(stepType);
        request.setQaList(List.of(new AiFeedbackRequest.QAItem("질문?", "답")));
        return request;
    }

    private Map<String, Object> openAiJsonResponse(String jsonContent) {
        Map<String, Object> message = Map.of("content", jsonContent);
        Map<String, Object> choice = Map.of("message", message);
        return Map.of("choices", List.of(choice));
    }

    @SuppressWarnings("unchecked")
    private void stubAiResponse(String jsonContent) {
        RestTemplate mockRestTemplate = mock(RestTemplate.class);
        when(mockRestTemplate.postForObject(anyString(), any(), eq(Map.class)))
            .thenReturn(openAiJsonResponse(jsonContent));
        service.restTemplate = mockRestTemplate;
    }

    private void stubGoodStatusResponse() {
        stubAiResponse("{\"status\":\"good\",\"message\":\"잘했어!\"}");
    }

    private void stubNeedStatusResponse() {
        stubAiResponse("{\"status\":\"need\",\"message\":\"다시 해보자.\"}");
    }

    private void stubGoodResultResponse() {
        // pre_reading_question 전용 응답 계약: status가 아니라 result 필드를 본다.
        stubAiResponse("{\"result\":\"good\",\"message\":\"좋아!\",\"failedRule\":null}");
    }

    @Test
    void inferPrompts_acceptContextBasedEmotionAndReasonQuestions() throws Exception {
        String during = privatePrompt("SYSTEM_PROMPT_DURING_READING_QUESTION");
        String deep = privatePrompt("SYSTEM_PROMPT_DURING_READING_PRACTICE_DEEP");
        String review = privatePrompt("SYSTEM_PROMPT_DURING_READING_PRACTICE_REVIEW");

        for (String prompt : List.of(during, deep, review)) {
            assertThat(prompt)
                // Test A/E: 행동 단서로 마음을 추론하는 질문과 짧은 답
                .contains("유나는 어떤 마음으로 친구 옆에")
                .contains("친구를 위로하고 싶었을 것 같아요")
                // Test B: 앞뒤 문맥으로 이유 추론
                .contains("친구는 왜 미소를 지었을까요?")
                // Test C: 본문에 답이 직접 있는 사실 질문은 direct 권장
                .contains("유나는 어디에 갔나요? / 운동장에 갔습니다")
                // Test D: 글에 단서가 전혀 없는 추측은 need
                .contains("내일 눈이 올까요?")
                // 실제 책 본문이 없는 연습/개별읽기에서도 구조를 우선 판단
                .contains("실제 책 본문을 받지 않는 화면에서는 질문 구조")
                .contains("답이 직접 적혀 있지")
                .contains("반드시 good");
        }
    }

    @Test
    void questionTypePrompts_keepConnectOpinionDirectAndInferMutuallyExclusive() throws Exception {
        String during = privatePrompt("SYSTEM_PROMPT_DURING_READING_QUESTION");
        String deep = privatePrompt("SYSTEM_PROMPT_DURING_READING_PRACTICE_DEEP");
        String review = privatePrompt("SYSTEM_PROMPT_DURING_READING_PRACTICE_REVIEW");

        for (String prompt : List.of(during, deep, review)) {
            String normalizedPrompt = prompt.replaceAll("\\s+", " ");
            assertThat(normalizedPrompt)
                .contains("네 질문 유형 분류의 최종 우선순위")
                .contains("질문의 핵심 의도를 아래 순서로 하나만 분류")
                .contains("나도 내가 잘하는 것이 없다고 생각했다가")
                .contains("나와 연결하기로는 good")
                .contains("짐작하기·책에서 바로 답 찾기·생각이나 느낌 말하기로는 need")
                .contains("내가 주인공이라면 어떻게 했을까요?")
                .contains("강아지똥이 민들레를 도운 장면을 보고 어떤 생각이")
                .contains("생각이나 느낌 말하기로는 good")
                .contains("실제로 더 잘 맞는 한국어 유형과 이유")
                .contains("'나와 연결하기'에 더 잘 맞아요")
                .contains("'생각이나 느낌 말하기'에 더 잘 맞아요")
                .contains("'단서로 짐작하기'에 더");
        }
    }

    @Test
    void storySummaryQuestionPrompts_doNotRequireBeginningMiddleEndWordingOrSlots() throws Exception {
        String practiceQuestion = privatePrompt("SYSTEM_PROMPT_QUESTION").replaceAll("\\s+", " ");
        String individualQuestion = privatePrompt("SYSTEM_PROMPT_INDIVIDUAL_QUESTION").replaceAll("\\s+", " ");
        String finalSummary = privatePrompt("SYSTEM_PROMPT_FINAL_SUMMARY").replaceAll("\\s+", " ");

        for (String prompt : List.of(practiceQuestion, individualQuestion)) {
            assertThat(prompt)
                .contains("그 낱말이 없다는 이유로 need를 주면 안 된다")
                .contains("1번은 처음, 2번은 가운데, 3번은 마지막")
                .contains("주인공에게 어떤 일이 있었나요?")
                .contains("시간이 지나면서 주인공에게 어떤 변화가 생겼나요?")
                .contains("주인공이 새로운 장소로 간 뒤 어떤 일이 생겼나요?")
                .contains("이 일이 일어난 뒤 무엇이 달라졌나요?")
                .contains("주인공이 겪은 문제는 어떻게 해결되었나요?")
                .contains("주인공의 생각은 어떻게 달라졌나요?")
                .contains("주인공의 생각이 바뀌게 된 계기는 무엇인가요?")
                .contains("이야기에서 가장 중요한 사건은 무엇이었나요?")
                .contains("처음에 똥은 왜 슬퍼했나요?")
                .contains("자신이 더럽고 쓸모없는 존재라고 생각했기 때문입니다")
                .contains("이야기 초반의 핵심 문제와 그 원인을 정리하므로 반드시 good")
                .contains("책을 읽은 나는 어떤 기분이었나요?")
                .contains("이 책 표지는 어떤 색인가요?");
        }

        assertThat(practiceQuestion)
            .contains("인물의 마음·생각 변화나 그 이유가 사건의 원인·결과·문제·해결과 이어지면")
            .contains("학생의 개인적 감상·선호만 묻고 이야기 내용이 전혀 드러나지 않을 때만");

        assertThat(finalSummary)
            .contains("질문 1=처음, 질문 2=가운데, 질문 3=마지막 순서일 필요도 없다")
            .contains("세 질문을 전체적으로 봤을 때 이야기의 주요 내용을 정리할 수 있으면 충분")
            .contains("주인공에게 가장 큰 문제는 무엇이었나요?")
            .contains("주인공의 생각은 어떻게 달라졌나요?")
            .contains("반드시 good");
    }

    @Test
    void opinionPrompts_acceptElementaryAmbiguousOwnThoughtQuestions() throws Exception {
        String during = privatePrompt("SYSTEM_PROMPT_DURING_READING_QUESTION");
        String deep = privatePrompt("SYSTEM_PROMPT_DURING_READING_PRACTICE_DEEP");
        String review = privatePrompt("SYSTEM_PROMPT_DURING_READING_PRACTICE_REVIEW");

        assertThat(during)
            .contains("주인공이 다시 고치려고 했을 때")
            .contains("그 모습을 보고 나는")
            .contains("opinion에서 good으로 우선 판정");
        assertThat(deep)
            .contains("이 글을 읽고 어떤 생각이 들었나요?")
            .contains("누구의 생각인지 애매하면")
            .contains("인물의 마음·이유만을 명백히 묻는 질문일 때만 infer");
        assertThat(review)
            .contains("주인공이 실수를 다시 고치려고 했을 때 어떤")
            .contains("그 모습을 보고 나는")
            .contains("주인공은 왜 다시 고치려고 했을까요?");
    }

    @Test
    void connectPrompts_acceptBroadElementaryExperienceConnections() throws Exception {
        String during = privatePrompt("SYSTEM_PROMPT_DURING_READING_QUESTION");
        String deep = privatePrompt("SYSTEM_PROMPT_DURING_READING_PRACTICE_DEEP");
        String review = privatePrompt("SYSTEM_PROMPT_DURING_READING_PRACTICE_REVIEW");

        assertThat(during)
            .contains("소재나 핵심 단어 하나")
            .contains("자기 경험을 먼저 말하고 질문을 잇는 표현")
            .contains("자유 경험 질문도");
        assertThat(deep)
            .contains("여러분도/친구들도 그런 적이")
            .contains("특정 소재 하나만 잡은 경험 질문도 인정");
        assertThat(review)
            .contains("나도 전학가본 경험이 있나요?")
            .contains("친구들도 전학 가면")
            .contains("놀이공원에 가본 적이");
    }

    private String privatePrompt(String fieldName) throws Exception {
        java.lang.reflect.Field field = FeedbackAiService.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        return (String) field.get(null);
    }

    /*
     * 이번 작업의 핵심 검증: "질문을 고쳐야 하는지/답을 고쳐야 하는지/
     * 둘 다인지"를 판단하는 공통 규칙([수정 대상 구분])이 KOREAN_ONLY_OUTPUT_RULE에
     * 있고, 모든 SYSTEM_PROMPT_*가 그 규칙을 이어받는 구조라 한 곳만
     * 고치면 연습읽기/개별읽기/읽기 전·중·후의 AI 피드백 전체 경로에
     * 동일하게 적용된다. 아래 9개는 FeedbackAiService가 실제로 쓰는
     * 프롬프트 전부다(특정 화면 하나만 고치고 끝내지 않았는지 확인).
     */
    @Test
    void allAiFeedbackPrompts_shareTheSameRevisionTargetRule() throws Exception {
        List<String> allPromptFieldNames = List.of(
            "SYSTEM_PROMPT_QUESTION",                      // 읽기 후 "책 질문 3개 만들기"(연습/개별 공용)
            "SYSTEM_PROMPT_SUMMARY",                        // 간추리기(질문 없이 요약문 하나만 평가)
            "SYSTEM_PROMPT_DURING_READING_QUESTION",        // 읽기 중 질문 만들기
            "SYSTEM_PROMPT_DURING_READING_PRACTICE_DEEP",   // 온책읽기 읽기 중 심화 연습(질문만, 답 없음)
            "SYSTEM_PROMPT_DURING_READING_PRACTICE_REVIEW", // 온책읽기 총복습
            "SYSTEM_PROMPT_EXTRA_PRACTICE",                 // 읽기 후 "질문으로 간추리기"(질문 2개 + 답 2개)
            "SYSTEM_PROMPT_FINAL_SUMMARY",                  // 읽기 후 최종 간추리기(질문 없이 요약문 하나만 평가)
            "SYSTEM_PROMPT_INDIVIDUAL_QUESTION",            // 개별읽기 질문
            "SYSTEM_PROMPT_PRE_READING_QUESTION"            // 읽기 전(연습/개별 공용) 제목/차례/그림/글
        );

        for (String fieldName : allPromptFieldNames) {
            String normalizedPrompt = privatePrompt(fieldName).replaceAll("\\s+", " ");
            assertThat(normalizedPrompt)
                .as("프롬프트 %s에 수정 대상 구분 공통 규칙이 있어야 함", fieldName)
                .contains("[수정 대상 구분 - 반드시 지킬 것")
                .contains("\"답을 고쳐 보세요.\"로 시작")
                .contains("\"질문을 고쳐 보세요.\"로 시작")
                .contains("\"질문과 답을 함께 고쳐 보세요.\"로")
                .contains("반드시 \"좋아요!\"로 시작");
        }
    }

    /*
     * 이번 작업의 핵심 검증: "질문↔답 관련성" 판정을 넓히는 [질문-답 관련성
     * 판정] 규칙도 KOREAN_ONLY_OUTPUT_RULE에 있어 9개 프롬프트 전부가
     * 공유한다(특정 화면 하나만 고치지 않았는지 확인). "완전히 무관한
     * 경우만 실패"라는 원칙과, 질문의 전제를 부정하며 답해도 통과라는
     * "밤" 사례, 짧은 답 허용, 사실 오류를 채점하지 말라는 원칙까지 전부
     * 포함돼 있는지 확인한다.
     */
    @Test
    void allAiFeedbackPrompts_shareTheSameAnswerRelevanceRule() throws Exception {
        List<String> allPromptFieldNames = List.of(
            "SYSTEM_PROMPT_QUESTION",
            "SYSTEM_PROMPT_SUMMARY",
            "SYSTEM_PROMPT_DURING_READING_QUESTION",
            "SYSTEM_PROMPT_DURING_READING_PRACTICE_DEEP",
            "SYSTEM_PROMPT_DURING_READING_PRACTICE_REVIEW",
            "SYSTEM_PROMPT_EXTRA_PRACTICE",
            "SYSTEM_PROMPT_FINAL_SUMMARY",
            "SYSTEM_PROMPT_INDIVIDUAL_QUESTION",
            "SYSTEM_PROMPT_PRE_READING_QUESTION"
        );

        for (String fieldName : allPromptFieldNames) {
            String normalizedPrompt = privatePrompt(fieldName).replaceAll("\\s+", " ");
            assertThat(normalizedPrompt)
                .as("프롬프트 %s에 질문-답 관련성 공통 규칙이 있어야 함", fieldName)
                .contains("[질문-답 관련성 판정 - 반드시 넓게 적용")
                .contains("조금이라도 자연스럽게 대응하면 통과 우선")
                // A~F 여섯 가지 중 하나만 만족해도 통과
                .contains("질문에 나온 대상에 대해 답하고 있음")
                .contains("학생의 생각·추측·느낌·경험으로 답함")
                .contains("한 단계 정도 추론하면 질문과 답의 연결을 설명할 수 있음")
                // "밤" 사례(질문의 전제를 부정하며 답해도 통과)
                .contains("밤은 먹는 밤일까?")
                .contains("밤은 먹는 것이 아니라 시간을 의미한다")
                // 짧은 답도 통과
                .contains("짧은 답도 무조건 실패시키지 말 것")
                .contains("용은 무서울까?")
                // 사실 채점기로 동작 금지
                .contains("너는 지식 정답 채점기가 아니다")
                // 완전히 무관한 경우만 실패
                .contains("정말 관련 없음으로 판단할 때만 실패시켜라")
                .contains("나는 김밥을 좋아한다")
                .contains("오늘 날씨는 맑다")
                .contains("내 연필은 파란색이다");
        }
    }

    /*
     * 실제 사용자 신고 사례: 질문의 전제를 "아니다, ~이다"로 바로잡으며
     * 답하는 경우("밤은 먹는 밤일까?" / "밤은 먹는 것이 아니라 시간을
     * 의미한다.")가 답 불일치로 처리됐다. 프롬프트가 이 사례를 명시적
     * good 예시로 담고 있는지, 그리고 실제 서버 로직도 good을 그대로
     * 통과시키는지 확인한다.
     */
    @SuppressWarnings("unchecked")
    @Test
    void getFeedback_preReadingTitleStep_answerThatCorrectsQuestionPremiseIsRelated() {
        RestTemplate mockRestTemplate = mock(RestTemplate.class);
        when(mockRestTemplate.postForObject(anyString(), any(), eq(Map.class)))
            .thenReturn(openAiJsonResponse("{\"result\":\"good\",\"message\":\"좋아요!\",\"failedRule\":null}"));
        service.restTemplate = mockRestTemplate;

        AiFeedbackResponse result = service.getFeedback(buildTitleRequest(
            "긴긴밤",
            "밤은 먹는 밤일까?",
            "밤은 먹는 것이 아니라 시간을 의미한다."));

        assertThat(result.getResult()).isEqualTo("good");
    }

    /* 검증 6: AI 오류 요청은 기록되지 않고 attempt_number도 증가하지 않음 */
    @Test
    void getFeedbackForAuthenticatedStudent_doesNotRecordAttemptWhenAiCallFails() {
        AiFeedbackRequest request = buildRequest("during_reading_question", "direct");

        AiFeedbackResponse result = service.getFeedbackForAuthenticatedStudent(
            STUDENT_ID, request, "eval-key-1", BOOK_ID, null);

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo("need");

        verify(aiEvaluationAttemptRepository, never()).save(any(AiEvaluationAttempt.class));
        verify(aiEvaluationAttemptRepository, never())
            .countByStudentIdAndEvaluationKey(any(), any());
    }

    /* 검증 7: 공개 /api/feedback/ai-review 호출 시 평가 기록이 저장되지 않음 */
    @Test
    void getFeedback_neverRecordsAttemptRegardlessOfOutcome() {
        AiFeedbackRequest request = buildRequest("during_reading_question", "direct");

        AiFeedbackResponse result = service.getFeedback(request);

        assertThat(result).isNotNull();

        verify(aiEvaluationAttemptRepository, never()).save(any(AiEvaluationAttempt.class));
        verify(aiEvaluationAttemptRepository, never())
            .countByStudentIdAndEvaluationKey(any(), any());
    }

    @Test
    void getFeedback_preReadingQuestionThrowsInsteadOfFallback() {
        AiFeedbackRequest request = buildRequest("pre_reading_question", "title");

        org.junit.jupiter.api.Assertions.assertThrows(
            org.springframework.web.server.ResponseStatusException.class,
            () -> service.getFeedback(request)
        );

        verify(aiEvaluationAttemptRepository, never()).save(any(AiEvaluationAttempt.class));
    }

    /*
     * 책별 데이터 분리 검증: 요청한 classReadingBookId가 학생이 속한 학급의
     * 책이 아니면(다른 학급 책 id) 403을 던지고 AI 호출/기록 자체가 일어나지
     * 않아야 한다. (기존 온책읽기 동작 - 이번 작업으로 바뀌지 않았음을 확인)
     */
    @Test
    void getFeedbackForAuthenticatedStudent_throwsForbiddenWhenBookBelongsToAnotherClass() {
        Long otherClassBookId = 999L;

        SchoolClass otherClass = new SchoolClass();
        otherClass.setId(77L);

        ClassReadingBook otherClassBook = new ClassReadingBook();
        otherClassBook.setId(otherClassBookId);
        otherClassBook.setSchoolClass(otherClass);

        when(classReadingBookRepository.findById(otherClassBookId))
            .thenReturn(Optional.of(otherClassBook));

        AiFeedbackRequest request = buildRequest("during_reading_question", "direct");

        assertThatThrownBy(() ->
            service.getFeedbackForAuthenticatedStudent(STUDENT_ID, request, "eval-key-1", otherClassBookId, null)
        )
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("403");

        verify(aiEvaluationAttemptRepository, never()).save(any(AiEvaluationAttempt.class));
    }

    // =========================================================
    // 개별읽기(readingRecordId) 시도 기록 - 이번 작업의 핵심 검증
    // =========================================================

    /* 1) 같은 질문을 처음 검사 → 1회차 기록, readingRecordId가 그대로 저장됨 */
    @Test
    void individualReading_firstAttempt_recordsAttemptNumberOne() {
        stubGoodStatusResponse();
        when(aiEvaluationAttemptRepository.countByStudentIdAndEvaluationKey(STUDENT_ID, "ind-key-1"))
            .thenReturn(0L);

        AiFeedbackRequest request = buildRequest("during_reading_question", "direct");

        service.getFeedbackForAuthenticatedStudent(
            STUDENT_ID, request, "ind-key-1", null, READING_RECORD_ID);

        org.mockito.ArgumentCaptor<AiEvaluationAttempt> captor =
            org.mockito.ArgumentCaptor.forClass(AiEvaluationAttempt.class);
        verify(aiEvaluationAttemptRepository).save(captor.capture());

        AiEvaluationAttempt saved = captor.getValue();
        assertThat(saved.getAttemptNumber()).isEqualTo(1);
        assertThat(saved.getReadingRecordId()).isEqualTo(READING_RECORD_ID);
        assertThat(saved.getClassReadingBookId()).isNull();
        assertThat(saved.getStudentId()).isEqualTo(STUDENT_ID);
        assertThat(saved.getStatus()).isEqualTo("good");
        assertThat(saved.getEvaluationKey()).isEqualTo("ind-key-1");
    }

    /* 2) 같은 질문을 다시 검사 → 같은 평가 대상의 2회차 */
    @Test
    void individualReading_secondAttempt_recordsAttemptNumberTwo() {
        stubNeedStatusResponse();
        when(aiEvaluationAttemptRepository.countByStudentIdAndEvaluationKey(STUDENT_ID, "ind-key-1"))
            .thenReturn(1L);

        AiFeedbackRequest request = buildRequest("during_reading_question", "direct");

        service.getFeedbackForAuthenticatedStudent(
            STUDENT_ID, request, "ind-key-1", null, READING_RECORD_ID);

        org.mockito.ArgumentCaptor<AiEvaluationAttempt> captor =
            org.mockito.ArgumentCaptor.forClass(AiEvaluationAttempt.class);
        verify(aiEvaluationAttemptRepository).save(captor.capture());

        assertThat(captor.getValue().getAttemptNumber()).isEqualTo(2);
        assertThat(captor.getValue().getStatus()).isEqualTo("need");
    }

    /* 3) 세 번째 검사 → 같은 평가 대상의 3회차 */
    @Test
    void individualReading_thirdAttempt_recordsAttemptNumberThree() {
        stubGoodStatusResponse();
        when(aiEvaluationAttemptRepository.countByStudentIdAndEvaluationKey(STUDENT_ID, "ind-key-1"))
            .thenReturn(2L);

        AiFeedbackRequest request = buildRequest("during_reading_question", "direct");

        service.getFeedbackForAuthenticatedStudent(
            STUDENT_ID, request, "ind-key-1", null, READING_RECORD_ID);

        org.mockito.ArgumentCaptor<AiEvaluationAttempt> captor =
            org.mockito.ArgumentCaptor.forClass(AiEvaluationAttempt.class);
        verify(aiEvaluationAttemptRepository).save(captor.capture());

        assertThat(captor.getValue().getAttemptNumber()).isEqualTo(3);
    }

    /* 4) 다른 질문(다른 evaluationKey) 검사 → 별도 평가 대상으로 1회차 */
    @Test
    void individualReading_differentEvaluationKey_startsAtAttemptNumberOne() {
        stubGoodStatusResponse();
        when(aiEvaluationAttemptRepository.countByStudentIdAndEvaluationKey(STUDENT_ID, "ind-key-2"))
            .thenReturn(0L);

        AiFeedbackRequest request = buildRequest("during_reading_question", "infer");

        service.getFeedbackForAuthenticatedStudent(
            STUDENT_ID, request, "ind-key-2", null, READING_RECORD_ID);

        org.mockito.ArgumentCaptor<AiEvaluationAttempt> captor =
            org.mockito.ArgumentCaptor.forClass(AiEvaluationAttempt.class);
        verify(aiEvaluationAttemptRepository).save(captor.capture());

        assertThat(captor.getValue().getAttemptNumber()).isEqualTo(1);
        assertThat(captor.getValue().getEvaluationKey()).isEqualTo("ind-key-2");
    }

    /* 6) 읽기 전(pre_reading_question)도 readingRecordId 경로로 기록됨 */
    @Test
    void individualReading_preReadingQuestion_recordsAttempt() {
        stubGoodResultResponse();
        when(aiEvaluationAttemptRepository.countByStudentIdAndEvaluationKey(STUDENT_ID, "ind-before-title"))
            .thenReturn(0L);

        AiFeedbackRequest request = buildRequest("pre_reading_question", "title");

        AiFeedbackResponse response = service.getFeedbackForAuthenticatedStudent(
            STUDENT_ID, request, "ind-before-title", null, READING_RECORD_ID);

        assertThat(response.getResult()).isEqualTo("good");

        org.mockito.ArgumentCaptor<AiEvaluationAttempt> captor =
            org.mockito.ArgumentCaptor.forClass(AiEvaluationAttempt.class);
        verify(aiEvaluationAttemptRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo("good");
        assertThat(captor.getValue().getReadingRecordId()).isEqualTo(READING_RECORD_ID);
    }

    /* 6) 읽기 후 질문(individual_question)도 readingRecordId 경로로 기록됨 */
    @Test
    void individualReading_individualQuestionType_recordsAttempt() {
        stubGoodStatusResponse();
        when(aiEvaluationAttemptRepository.countByStudentIdAndEvaluationKey(STUDENT_ID, "ind-after-q-1"))
            .thenReturn(0L);

        AiFeedbackRequest request = buildRequest("individual_question", null);

        service.getFeedbackForAuthenticatedStudent(
            STUDENT_ID, request, "ind-after-q-1", null, READING_RECORD_ID);

        org.mockito.ArgumentCaptor<AiEvaluationAttempt> captor =
            org.mockito.ArgumentCaptor.forClass(AiEvaluationAttempt.class);
        verify(aiEvaluationAttemptRepository).save(captor.capture());
        assertThat(captor.getValue().getActivityType()).isEqualTo("individual_question");
    }

    /* 6) 읽기 후 간추리기(individual_summary)도 readingRecordId 경로로 기록됨 */
    @Test
    void individualReading_individualSummaryType_recordsAttempt() {
        stubGoodStatusResponse();
        when(aiEvaluationAttemptRepository.countByStudentIdAndEvaluationKey(STUDENT_ID, "ind-after-summary"))
            .thenReturn(0L);

        AiFeedbackRequest request = buildRequest("individual_summary", null);

        service.getFeedbackForAuthenticatedStudent(
            STUDENT_ID, request, "ind-after-summary", null, READING_RECORD_ID);

        org.mockito.ArgumentCaptor<AiEvaluationAttempt> captor =
            org.mockito.ArgumentCaptor.forClass(AiEvaluationAttempt.class);
        verify(aiEvaluationAttemptRepository).save(captor.capture());
        assertThat(captor.getValue().getActivityType()).isEqualTo("individual_summary");
    }

    /* 7) 다른 학생의 readingRecordId를 보내면 403이고 기록되지 않음(섞이지 않음) */
    @Test
    void individualReading_throwsForbiddenWhenReadingRecordBelongsToAnotherStudent() {
        Long otherStudentId = 999L;

        AiFeedbackRequest request = buildRequest("during_reading_question", "direct");

        assertThatThrownBy(() ->
            service.getFeedbackForAuthenticatedStudent(
                otherStudentId, request, "ind-key-1", null, READING_RECORD_ID)
        )
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("403");

        verify(aiEvaluationAttemptRepository, never()).save(any(AiEvaluationAttempt.class));
    }

    /* 존재하지 않는 readingRecordId → 404, 기록되지 않음 */
    @Test
    void individualReading_throwsNotFoundWhenReadingRecordDoesNotExist() {
        Long missingId = 12345L;
        when(readingRecordRepository.findById(missingId)).thenReturn(Optional.empty());

        AiFeedbackRequest request = buildRequest("during_reading_question", "direct");

        assertThatThrownBy(() ->
            service.getFeedbackForAuthenticatedStudent(
                STUDENT_ID, request, "ind-key-1", null, missingId)
        )
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("404");

        verify(aiEvaluationAttemptRepository, never()).save(any(AiEvaluationAttempt.class));
    }

    /* classReadingBookId와 readingRecordId가 둘 다 없으면 400이고 AI 호출/기록이 일어나지 않음 */
    @Test
    void throwsBadRequestWhenNeitherClassReadingBookIdNorReadingRecordIdProvided() {
        AiFeedbackRequest request = buildRequest("during_reading_question", "direct");

        assertThatThrownBy(() ->
            service.getFeedbackForAuthenticatedStudent(STUDENT_ID, request, "ind-key-1", null, null)
        )
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("400");

        verify(aiEvaluationAttemptRepository, never()).save(any(AiEvaluationAttempt.class));
        verify(readingRecordRepository, never()).findById(any());
    }

    /* 9) 기존 온책읽기(classReadingBookId) 경로는 이번 작업으로 영향받지 않음 */
    @Test
    void practiceReading_classReadingBookIdPath_stillRecordsAttemptAsBefore() {
        stubGoodStatusResponse();
        when(aiEvaluationAttemptRepository.countByStudentIdAndEvaluationKey(STUDENT_ID, "practice-key-1"))
            .thenReturn(0L);

        AiFeedbackRequest request = buildRequest("during_reading_question", "direct");

        service.getFeedbackForAuthenticatedStudent(
            STUDENT_ID, request, "practice-key-1", BOOK_ID, null);

        org.mockito.ArgumentCaptor<AiEvaluationAttempt> captor =
            org.mockito.ArgumentCaptor.forClass(AiEvaluationAttempt.class);
        verify(aiEvaluationAttemptRepository).save(captor.capture());

        assertThat(captor.getValue().getClassReadingBookId()).isEqualTo(BOOK_ID);
        assertThat(captor.getValue().getReadingRecordId()).isNull();
        verify(readingRecordRepository, never()).findById(any());
    }

    /* 5)/부가: good이 아닌 결과는 need로 정확히 저장됨(이미 위 2번 테스트에서도 확인) */
    @Test
    void individualReading_needResult_isStoredAsNeed() {
        stubNeedStatusResponse();
        when(aiEvaluationAttemptRepository.countByStudentIdAndEvaluationKey(STUDENT_ID, "ind-key-need"))
            .thenReturn(0L);

        AiFeedbackRequest request = buildRequest("during_reading_question", "direct");

        service.getFeedbackForAuthenticatedStudent(
            STUDENT_ID, request, "ind-key-need", null, READING_RECORD_ID);

        org.mockito.ArgumentCaptor<AiEvaluationAttempt> captor =
            org.mockito.ArgumentCaptor.forClass(AiEvaluationAttempt.class);
        verify(aiEvaluationAttemptRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo("need");

        verify(aiEvaluationAttemptRepository, times(1)).save(any(AiEvaluationAttempt.class));
    }

    // =========================================================
    // 영어 노출 방지 후처리(sanitizeEnglishLeakage) - 이번 작업의 핵심 검증
    // =========================================================

    /* AI가 영문 질문 유형명을 message에 섞어 반환해도 학생에게는 한국어로 치환되어 나감 */
    @Test
    void getFeedback_englishTypeNamesInMessage_areSanitizedToKorean() {
        stubAiResponse("{\"status\":\"need\",\"message\":\"Opinion 질문이에요. Direct 유형으로 바꿔 보세요.\"}");

        AiFeedbackResponse result = service.getFeedback(buildRequest("during_reading_question", "opinion"));

        assertThat(result.getMessage()).doesNotContainIgnoringCase("Opinion");
        assertThat(result.getMessage()).doesNotContainIgnoringCase("Direct");
        assertThat(result.getMessage()).contains("생각이나 느낌 말하기");
        assertThat(result.getMessage()).contains("책에서 바로 답 찾기");
    }

    /* 흔한 오타 "Opinon"도 공식 한국어 이름으로 치환됨 */
    @Test
    void getFeedback_misspelledOpinonTypeName_isSanitizedToKorean() {
        stubAiResponse("{\"status\":\"need\",\"message\":\"Opinon 질문으로 바꿔보세요.\"}");

        AiFeedbackResponse result = service.getFeedback(buildRequest("during_reading_question", "opinion"));

        assertThat(result.getMessage()).doesNotContainIgnoringCase("Opinon");
        assertThat(result.getMessage()).contains("생각이나 느낌 말하기");
    }

    /* Infer/Inference, Connect/Connection도 모두 치환됨 */
    @Test
    void getFeedback_allFourEnglishTypeNames_areSanitizedToKorean() {
        stubAiResponse(
            "{\"status\":\"need\",\"message\":\"Infer, Inference, Connect, Connection 모두 확인해 보세요.\"}");

        AiFeedbackResponse result = service.getFeedback(buildRequest("during_reading_question", "infer"));

        assertThat(result.getMessage())
            .doesNotContainIgnoringCase("Infer")
            .doesNotContainIgnoringCase("Connect")
            .contains("단서로 짐작하기")
            .contains("나와 연결하기");
    }

    /* AI 응답이 통째로 영어 문장이면 원문을 노출하지 않고 한국어 기본 안내로 대체됨(need) */
    @Test
    void getFeedback_fullyEnglishNeedMessage_isReplacedWithKoreanFallback() {
        stubAiResponse(
            "{\"status\":\"need\",\"message\":\"This question is unclear. Try again with more detail please.\"}");

        AiFeedbackResponse result = service.getFeedback(buildRequest("during_reading_question", "direct"));

        assertThat(result.getMessage()).doesNotContainIgnoringCase("Try again");
        assertThat(result.getMessage())
            .isEqualTo("질문을 잘 살펴보았어요. 책과 관련된 궁금한 점이 드러나도록 조금 더 구체적으로 적어 보세요.");
    }

    /* AI 응답이 통째로 영어 문장이면 한국어 기본 안내로 대체됨(good) */
    @Test
    void getFeedback_fullyEnglishGoodMessage_isReplacedWithKoreanFallback() {
        stubAiResponse("{\"status\":\"good\",\"message\":\"Good question! Great job on this one.\"}");

        AiFeedbackResponse result = service.getFeedback(buildRequest("during_reading_question", "direct"));

        assertThat(result.getMessage()).isEqualTo("정말 잘했어요! 다음으로 넘어가 볼까요?");
    }

    /* 짧은 영어 고유명사(영어 책 제목 등)가 섞인 정상 한국어 문장은 통째로 대체되지 않음 */
    @Test
    void getFeedback_koreanMessageWithShortEnglishTitle_isNotReplacedEntirely() {
        stubAiResponse("{\"status\":\"good\",\"message\":\"Charlotte's Web 이야기를 잘 이해했어요!\"}");

        AiFeedbackResponse result = service.getFeedback(buildRequest("during_reading_question", "direct"));

        assertThat(result.getMessage()).contains("Charlotte's Web");
        assertThat(result.getMessage()).contains("이야기를 잘 이해했어요");
    }

    /* pre_reading_question(result 계약)에서도 영어 유형명 치환이 동일하게 적용됨 */
    @Test
    void getFeedback_preReadingQuestion_englishInMessage_isSanitized() {
        stubAiResponse(
            "{\"result\":\"good\",\"message\":\"잘했어요! Opinion 관점도 좋아요.\",\"failedRule\":null}");

        AiFeedbackResponse result = service.getFeedback(buildRequest("pre_reading_question", "title"));

        assertThat(result.getMessage()).doesNotContainIgnoringCase("Opinion");
        assertThat(result.getMessage()).contains("생각이나 느낌 말하기");
    }

    // =========================================================
    // 읽기 전 제목 질문 - 등록된 책 제목이 실제로 AI 요청에 전달되는지 검증
    // =========================================================

    /* stepType이 title이면 등록된 책 제목이 OpenAI 요청 본문(user 메시지)에 포함됨 */
    @SuppressWarnings("unchecked")
    @Test
    void getFeedback_preReadingTitleStep_includesRegisteredBookTitleInAiRequest() {
        RestTemplate mockRestTemplate = mock(RestTemplate.class);
        when(mockRestTemplate.postForObject(anyString(), any(), eq(Map.class)))
            .thenReturn(openAiJsonResponse("{\"result\":\"good\",\"message\":\"좋아요!\",\"failedRule\":null}"));
        service.restTemplate = mockRestTemplate;

        AiFeedbackRequest request = new AiFeedbackRequest();
        request.setType("pre_reading_question");
        request.setStepType("title");
        request.setBookTitle("백설공주");
        request.setQaList(List.of(new AiFeedbackRequest.QAItem(
            "백설공주의 의미가 뭘까?", "얼굴이 하얗다는 뜻일 것 같다.")));

        service.getFeedback(request);

        org.mockito.ArgumentCaptor<org.springframework.http.HttpEntity<Map<String, Object>>> entityCaptor =
            org.mockito.ArgumentCaptor.forClass(org.springframework.http.HttpEntity.class);
        verify(mockRestTemplate).postForObject(anyString(), entityCaptor.capture(), eq(Map.class));

        List<Map<String, Object>> messages =
            (List<Map<String, Object>>) entityCaptor.getValue().getBody().get("messages");
        String userContent = (String) messages.get(1).get("content");

        assertThat(userContent).contains("책 제목: 백설공주");
        assertThat(userContent).contains("백설공주의 의미가 뭘까?");
    }

    /*
     * 제목의 핵심 낱말에서 자신의 경험·생각을 연결하거나 조금 어색하게
     * 표현한 질문도 초4 수준의 정상 제목 질문으로 인정하도록, 실제 AI에
     * 전달되는 공통 프롬프트에 필수 통과 사례가 모두 포함돼 있는지 검증한다.
     * 연습읽기와 개별읽기는 같은 pre_reading_question 프롬프트를 사용한다.
     */
    @SuppressWarnings("unchecked")
    @Test
    void getFeedback_preReadingTitleStep_promptAllowsExperienceAndImperfectExpressions() {
        RestTemplate mockRestTemplate = mock(RestTemplate.class);
        when(mockRestTemplate.postForObject(anyString(), any(), eq(Map.class)))
            .thenReturn(openAiJsonResponse("{\"result\":\"good\",\"message\":\"좋아요!\",\"failedRule\":null}"));
        service.restTemplate = mockRestTemplate;

        service.getFeedback(buildTitleRequest(
            "나만의 보물 찾기",
            "나도 보물을 찾아본 경험이 있을까?",
            "어릴 때 보물찾기 놀이를 해 본 적이 있다."));

        org.mockito.ArgumentCaptor<org.springframework.http.HttpEntity<Map<String, Object>>> entityCaptor =
            org.mockito.ArgumentCaptor.forClass(org.springframework.http.HttpEntity.class);
        verify(mockRestTemplate).postForObject(anyString(), entityCaptor.capture(), eq(Map.class));

        List<Map<String, Object>> messages =
            (List<Map<String, Object>>) entityCaptor.getValue().getBody().get("messages");
        String systemContent = (String) messages.get(0).get("content");

        assertThat(systemContent)
            .contains("보물 찾기는 어떤 내용일까")
            .contains("어떤 보물을 찾게 될까")
            .contains("주인공은 보물을 찾을 수 있을까")
            .contains("왜 제목이 나만의 보물 찾기일까")
            .contains("나도 보물을 찾아본 경험이 있을까")
            .contains("내가 보물이라고 생각하는 것은 무엇일까")
            .contains("나라면 어떤 보물을 찾고 싶을까")
            .contains("주인공 보물은 뭘까")
            .contains("주인공은 몇 살일까? → NOT_RELATED_TO_BOOK")
            .contains("오늘 날씨는 어떨까? → NOT_RELATED_TO_BOOK")
            .contains("재미있을 것 같다. → NOT_A_QUESTION")
            .contains("왜? → NOT_A_QUESTION")
            .contains("ㅋㅋㅋㅋㅋㅋ → NOT_A_QUESTION")
            .contains("나만의 보물 찾기? → SHALLOW_STAGE_QUESTION")
            .contains("NOT_RELATED_TO_BOOK을 주면 안 돼")
            .contains("1~2문장");
    }

    /*
     * "나만의 보물 찾기" 한 권만의 예외가 아니라, 어느 제목에나 적용되는
     * 원칙인지 확인한다. 특히 줄거리 예상이 아닌 "제목 속 대상이 실제로
     * 있는지/어떤 성질인지" 궁금해하는 일반적인 궁금증(존재 여부, 습성,
     * 다른 지식과의 연결)도 정상 질문으로 인정하도록 프롬프트에 들어
     * 있는지, 서로 다른 책 제목("용이 산다", "강아지똥", "긴긴밤") 예시로
     * 검증한다. 연습읽기와 개별읽기는 같은 pre_reading_question 프롬프트를
     * 쓰므로 이 한 곳만 확인하면 양쪽 모두에 적용된다.
     */
    @SuppressWarnings("unchecked")
    @Test
    void getFeedback_preReadingTitleStep_promptAllowsGeneralCuriosityAndKnowledgeConnection() {
        RestTemplate mockRestTemplate = mock(RestTemplate.class);
        when(mockRestTemplate.postForObject(anyString(), any(), eq(Map.class)))
            .thenReturn(openAiJsonResponse("{\"result\":\"good\",\"message\":\"좋아요!\",\"failedRule\":null}"));
        service.restTemplate = mockRestTemplate;

        service.getFeedback(buildTitleRequest(
            "용이 산다",
            "용은 실제로 존재할까?",
            "진짜 있을 수도 있을 것 같다."));

        org.mockito.ArgumentCaptor<org.springframework.http.HttpEntity<Map<String, Object>>> entityCaptor =
            org.mockito.ArgumentCaptor.forClass(org.springframework.http.HttpEntity.class);
        verify(mockRestTemplate).postForObject(anyString(), entityCaptor.capture(), eq(Map.class));

        List<Map<String, Object>> messages =
            (List<Map<String, Object>>) entityCaptor.getValue().getBody().get("messages");
        String systemContent = (String) messages.get(0).get("content");

        assertThat(systemContent)
            // 줄거리 예상이 아니어도 되는 일반 궁금증(존재 여부·습성·이유)
            .contains("용은 실제로 존재할까")
            .contains("용은 어디에 살까")
            .contains("용은 왜 불을 뿜을까")
            .contains("나는 용을 만나면 무서울까")
            // 다른 지식과의 연결
            .contains("용은 공룡과 비슷할까")
            // 제목 이유 / 앞으로 일어날 일 예상
            .contains("왜 제목이 용이 산다일까")
            .contains("앞으로 용에게 어떤 일이 생길까")
            // 같은 원칙이 다른 제목에도 동일하게 적용됨을 보여주는 예시
            .contains("강아지똥도 쓸모가 있을까")
            .contains("강아지똥은 왜 생길까")
            .contains("왜 밤이 긴 걸까")
            .contains("긴 밤에는 무엇을 할까")
            // 줄거리 예상 활동으로 한정하지 않는다는 명시적 원칙
            .contains("줄거리를 예상해야만 하는 활동이 아니므로")
            // 여전히 불통해야 하는 사례들
            .contains("오늘 급식은 무엇일까? → NOT_RELATED_TO_BOOK")
            .contains("용이 산다? → SHALLOW_STAGE_QUESTION");
    }

    /*
     * 실제 사용자 신고 사례: "용은 전설속의 동물인가?"가 여전히 불통
     * 처리됐다. "용은 실제로 존재할까?"라는 예시 하나만 넣어서는 표현이
     * 조금만 달라도 AI가 다른 취급을 할 수 있다는 문제라, 이번에는
     * "책 내용을 직접 예상하는가"가 아니라 "제목을 보고 자연스럽게 떠올릴
     * 수 있는 궁금증인가"를 최우선 원칙으로 앞세우고, 1~4번 예시는 닫힌
     * 목록이 아니라 참고 사례일 뿐이라는 점과 분류·상상형 질문("용은
     * 전설 속의 동물인가?", "용도 가족이 있을까?" 등)도 명시적으로 포함해
     * 프롬프트를 더 넓혔다. 새로 넓힌 원칙과 신고된 문장이 실제로 프롬프트
     * 안에 있는지, 그리고 신고된 사례를 그대로 보내면 서버가 good을
     * 그대로 통과시키는지 모두 확인한다.
     */
    @SuppressWarnings("unchecked")
    @Test
    void getFeedback_preReadingTitleStep_promptTreatsQuestionSpiritBroadlyNotJustExactExamples() {
        RestTemplate mockRestTemplate = mock(RestTemplate.class);
        when(mockRestTemplate.postForObject(anyString(), any(), eq(Map.class)))
            .thenReturn(openAiJsonResponse("{\"result\":\"good\",\"message\":\"좋아요!\",\"failedRule\":null}"));
        service.restTemplate = mockRestTemplate;

        service.getFeedback(buildTitleRequest(
            "용이 산다",
            "용은 전설속의 동물인가?",
            "옛날이야기에 나오는 상상의 동물인 것 같다."));

        org.mockito.ArgumentCaptor<org.springframework.http.HttpEntity<Map<String, Object>>> entityCaptor =
            org.mockito.ArgumentCaptor.forClass(org.springframework.http.HttpEntity.class);
        verify(mockRestTemplate).postForObject(anyString(), entityCaptor.capture(), eq(Map.class));

        List<Map<String, Object>> messages =
            (List<Map<String, Object>>) entityCaptor.getValue().getBody().get("messages");
        String systemContent = ((String) messages.get(0).get("content")).replaceAll("\\s+", " ");

        assertThat(systemContent)
            // 새로 앞세운 핵심 원칙(책 내용 예상이 아니라 "떠올릴 수 있는 궁금증"인가)
            .contains("책 내용을 직접 예상하는가")
            .contains("이 책 제목이나 책에서 학생이 연상했을 가능성이 조금이라도")
            .contains("닫힌 목록이 아니다")
            .contains("애매하면 항상 통과시켜라")
            // 신고된 실제 문장과 같은 종류(분류·상상형 질문)도 명시적으로 포함
            .contains("용은 전설 속의 동물인가?")
            .contains("용도 가족이 있을까?")
            .contains("용은 학교에 갈 수 있을까?")
            // 5번(NOT_RELATED_TO_BOOK)을 좁게 적용하라는 지시
            .contains("이 5번은 좁게 적용해")
            // 다양한 제목으로 같은 원칙이 반복 적용됨을 보여주는 예시
            .contains("인어는 정말 있을까?")
            .contains("만복이는 누구일까?");

        // 실제 서버 로직도 이 입력을 good으로 그대로 통과시키는지 확인
        AiFeedbackResponse result = service.getFeedback(buildTitleRequest(
            "용이 산다",
            "용은 전설속의 동물인가?",
            "옛날이야기에 나오는 상상의 동물인 것 같다."));
        assertThat(result.getResult()).isEqualTo("good");
    }

    /*
     * 세 번째 완화 라운드의 핵심: 제목의 낱말을 질문에 전혀 쓰지 않아도,
     * 제목이 다루는 가치·주제로 "한 단계" 확장한 질문이면 통과해야 한다.
     * 실제 신고 사례: 책 제목이 "나만의 보물 찾기"인데 학생 질문
     * "내가 좋아하는 보물은 무엇인가?"가 여전히 불통 처리됐다. 이 질문은
     * "보물"이라는 낱말이 있어 사실 3번 기준(핵심 단어 포함)만으로도
     * 통과해야 하는데도 막혔던 사례라, 프롬프트가 실제로 이 문장과 그보다
     * 더 간접적인 문장(낱말이 아예 없는 문장)까지 명시적으로 통과 예시로
     * 담고 있는지 확인한다. "책 내용과 조금 멀다" 류의 표현이 실패
     * 피드백에서 완전히 빠졌는지도 함께 확인한다.
     */
    @SuppressWarnings("unchecked")
    @Test
    void getFeedback_preReadingTitleStep_promptAllowsIndirectAssociationsWithNoTitleWordOverlap() throws Exception {
        RestTemplate mockRestTemplate = mock(RestTemplate.class);
        when(mockRestTemplate.postForObject(anyString(), any(), eq(Map.class)))
            .thenReturn(openAiJsonResponse("{\"result\":\"good\",\"message\":\"좋아요!\",\"failedRule\":null}"));
        service.restTemplate = mockRestTemplate;

        service.getFeedback(buildTitleRequest(
            "나만의 보물 찾기",
            "내가 좋아하는 보물은 무엇인가?",
            "우리 엄마다."));

        org.mockito.ArgumentCaptor<org.springframework.http.HttpEntity<Map<String, Object>>> entityCaptor =
            org.mockito.ArgumentCaptor.forClass(org.springframework.http.HttpEntity.class);
        verify(mockRestTemplate).postForObject(anyString(), entityCaptor.capture(), eq(Map.class));

        List<Map<String, Object>> messages =
            (List<Map<String, Object>>) entityCaptor.getValue().getBody().get("messages");
        String normalizedPrompt = ((String) messages.get(0).get("content")).replaceAll("\\s+", " ");

        assertThat(normalizedPrompt)
            // 신고된 실제 문장
            .contains("내가 좋아하는 보물은 무엇인가?")
            // "보물"이라는 낱말이 전혀 없는, 한 단계 더 간접적인 예시들
            .contains("가족도 보물이 될 수 있을까?")
            .contains("나에게 가장 중요한 사람은 누구일까?")
            .contains("사람마다 소중한 것은 다를까?")
            .contains("친구도 나에게 보물이 될 수 있을까?")
            // 다른 제목에도 같은 간접 확장 원칙이 적용됨(강아지똥/긴긴밤)
            .contains("쓸모없어 보이는 것도 중요한 역할을 할 수 있을까?")
            .contains("식물은 무엇을 먹고 자랄까?")
            .contains("밤에 혼자 있으면 어떤 기분일까?")
            .contains("시간이 느리게 가는 때는 언제일까?")
            // 간접 연상을 명시적으로 허용한다는 원칙 문구
            .contains("한 단계 정도 떨어진 간접")
            .contains("제목의 낱말이 질문에 전혀 없어도")
            // "강아지는 귀여울까?"가 이제는 거부가 아니라 통과 예시로 바뀌었는지
            .contains("강아지는 귀여울까?")
            .contains("반드시 통과시켜");

        // 금지된 문구("조금 멀어요", "인물/물건/행동 중 하나를 골라")가
        // 더 이상 어디에도 남아 있지 않은지 프롬프트 전체에서 확인
        assertThat(normalizedPrompt)
            .doesNotContain("조금 멀어요")
            .doesNotContain("사람, 물건, 행동 중 하나를 골라");

        // 실제 서버 로직도 이 입력을 good으로 그대로 통과시키는지 확인
        AiFeedbackResponse result = service.getFeedback(buildTitleRequest(
            "나만의 보물 찾기",
            "내가 좋아하는 보물은 무엇인가?",
            "우리 엄마다."));
        assertThat(result.getResult()).isEqualTo("good");
    }

    /*
     * 실패 피드백 문구 자체("조금 멀어요", "인물/물건/행동 중 하나를 골라")가
     * 서버 고정 템플릿(applyFixedTitleMismatchFeedback)에서도 완전히
     * 사라졌는지 확인한다. 정말 무관한 질문일 때도 이제는 더 부드러운
     * 안내("이 책 제목을 보고 떠오른 궁금한 점을 다시 질문해 보세요")를 쓴다.
     */
    @Test
    void getFeedback_titleMismatchFixedTemplates_noLongerUseBannedPhrases() {
        stubAiResponse(
            "{\"result\":\"retry\",\"message\":\"아무 문구\",\"failedRule\":\"NOT_RELATED_TO_BOOK\"}");

        AiFeedbackResponse withTitle = service.getFeedback(
            buildTitleRequest("용이 산다", "오늘 급식은 무엇일까?", "김치찌개일 것 같다."));
        assertThat(withTitle.getMessage())
            .doesNotContain("조금 멀어요")
            .doesNotContain("사람, 물건, 행동 중 하나를 골라")
            .startsWith("질문을 고쳐 보세요.")
            .contains("용이 산다");

        stubAiResponse("{\"result\":\"retry\",\"message\":\"아무 문구\",\"failedRule\":\"NOT_RELATED_TO_BOOK\"}");
        AiFeedbackRequest noTitleRequest = new AiFeedbackRequest();
        noTitleRequest.setType("pre_reading_question");
        noTitleRequest.setStepType("title");
        noTitleRequest.setQaList(List.of(new AiFeedbackRequest.QAItem("오늘 급식은 무엇일까?", "김치찌개일 것 같다.")));
        AiFeedbackResponse withoutTitle = service.getFeedback(noTitleRequest);
        assertThat(withoutTitle.getMessage())
            .doesNotContain("조금 멀어요")
            .doesNotContain("사람, 물건, 행동 중 하나를 골라")
            .startsWith("질문을 고쳐 보세요.");
    }

    // =========================================================
    // NOT_RELATED_TO_BOOK 고정 피드백(applyFixedTitleMismatchFeedback) 검증
    // =========================================================

    private AiFeedbackRequest buildTitleRequest(String bookTitle, String question, String answer) {
        AiFeedbackRequest request = new AiFeedbackRequest();
        request.setType("pre_reading_question");
        request.setStepType("title");
        request.setBookTitle(bookTitle);
        request.setQaList(List.of(new AiFeedbackRequest.QAItem(question, answer)));
        return request;
    }

    /* 테스트 1: AI가 무엇을 반환하든 NOT_RELATED_TO_BOOK이면 서버가 고정 문구로 덮어씀 */
    @Test
    void getFeedback_titleStepNotRelatedToBook_overridesMessageWithFixedBookTitleTemplate() {
        stubAiResponse(
            "{\"result\":\"retry\",\"message\":\"제목이 무엇인지 묻기보다, 제목을 보고 궁금한 인물이나 사건을 적어 보세요.\",\"failedRule\":\"NOT_RELATED_TO_BOOK\"}");

        AiFeedbackResponse result = service.getFeedback(
            buildTitleRequest("우리 낙원에서", "안녕하세요는 무슨 뜻일까?", "천국을 의미하는 것 같다."));

        assertThat(result.getResult()).isEqualTo("retry");
        assertThat(result.getFailedRule()).isEqualTo("NOT_RELATED_TO_BOOK");
        assertThat(result.getMessage()).isEqualTo(
            "질문을 고쳐 보세요. 이 책 제목 '우리 낙원에서'를 보고 떠오른 궁금한 점을 다시 질문해 보세요.");
        assertThat(result.getMessage()).startsWith("질문을 고쳐 보세요.");
        assertThat(result.getMessage()).doesNotContain("제목이 무엇인지 묻기보다");
        assertThat(result.getMessage()).doesNotContain("인물이나 사건을 적어 보세요");
    }

    /* 테스트 5: 다른 책 제목("백설공주")에도 동일하게 적용되고 그 제목이 그대로 들어감 */
    @Test
    void getFeedback_titleStepNotRelatedToBook_includesActualBookTitleSnowWhite() {
        stubAiResponse("{\"result\":\"retry\",\"message\":\"아무 문구\",\"failedRule\":\"NOT_RELATED_TO_BOOK\"}");

        AiFeedbackResponse result = service.getFeedback(
            buildTitleRequest("백설공주", "안녕하세요는 무슨 뜻일까?", "인사하는 말이다."));

        assertThat(result.getMessage()).contains("백설공주");
        assertThat(result.getMessage()).isEqualTo(
            "질문을 고쳐 보세요. 이 책 제목 '백설공주'를 보고 떠오른 궁금한 점을 다시 질문해 보세요.");
    }

    /* 받침 있는 제목("마당을 나온 암탉")은 "과"/"이라는" 조사가 올바르게 붙음 */
    @Test
    void getFeedback_titleStepNotRelatedToBook_usesCorrectParticleForTitleWithFinalConsonant() {
        stubAiResponse("{\"result\":\"retry\",\"message\":\"아무 문구\",\"failedRule\":\"NOT_RELATED_TO_BOOK\"}");

        AiFeedbackResponse result = service.getFeedback(
            buildTitleRequest("마당을 나온 암탉", "축구를 잘하는 사람은 누구일까?", "손흥민일 것 같다."));

        assertThat(result.getMessage()).isEqualTo(
            "질문을 고쳐 보세요. 이 책 제목 '마당을 나온 암탉'을 보고 떠오른 궁금한 점을 다시 질문해 보세요.");
    }

    /* 테스트 6: bookTitle이 없으면(null) 안전한 일반 문구를 쓰고 "null" 문자열이 노출되지 않음 */
    @Test
    void getFeedback_titleStepNotRelatedToBook_nullBookTitle_usesSafeFallbackMessage() {
        stubAiResponse("{\"result\":\"retry\",\"message\":\"아무 문구\",\"failedRule\":\"NOT_RELATED_TO_BOOK\"}");

        AiFeedbackRequest request = new AiFeedbackRequest();
        request.setType("pre_reading_question");
        request.setStepType("title");
        request.setQaList(List.of(new AiFeedbackRequest.QAItem("안녕하세요는 무슨 뜻일까?", "천국을 의미하는 것 같다.")));

        AiFeedbackResponse result = service.getFeedback(request);

        assertThat(result.getMessage()).doesNotContainIgnoringCase("null");
        assertThat(result.getMessage()).doesNotContainIgnoringCase("undefined");
        assertThat(result.getMessage()).isEqualTo(
            "질문을 고쳐 보세요. 이 책 제목을 보고 떠오른 궁금한 점을 다시 질문해 보세요.");
    }

    /* bookTitle이 빈 문자열("")이어도 위와 동일하게 안전한 일반 문구를 씀 */
    @Test
    void getFeedback_titleStepNotRelatedToBook_blankBookTitle_usesSafeFallbackMessage() {
        stubAiResponse("{\"result\":\"retry\",\"message\":\"아무 문구\",\"failedRule\":\"NOT_RELATED_TO_BOOK\"}");

        AiFeedbackResponse result = service.getFeedback(buildTitleRequest("", "안녕하세요는 무슨 뜻일까?", "잘 모르겠다."));

        assertThat(result.getMessage()).isEqualTo(
            "질문을 고쳐 보세요. 이 책 제목을 보고 떠오른 궁금한 점을 다시 질문해 보세요.");
    }

    /* 회귀: NOT_RELATED_TO_BOOK이 아닌 다른 failedRule(예: SHALLOW_STAGE_QUESTION)은 건드리지 않음 */
    @Test
    void getFeedback_titleStepOtherFailedRule_doesNotApplyFixedTemplate() {
        stubAiResponse(
            "{\"result\":\"retry\",\"message\":\"제목이 무엇인지 묻기보다, 제목을 보고 궁금한 인물이나 사건을 적어 보세요.\",\"failedRule\":\"SHALLOW_STAGE_QUESTION\"}");

        AiFeedbackResponse result = service.getFeedback(buildTitleRequest("우리 낙원에서", "제목이 뭘까", "모르겠다"));

        assertThat(result.getMessage()).isEqualTo(
            "제목이 무엇인지 묻기보다, 제목을 보고 궁금한 인물이나 사건을 적어 보세요.");
    }

    /* 회귀: title 단계가 아닌 다른 stepType(contents)에서는 이 고정 문구를 적용하지 않음 */
    @Test
    void getFeedback_nonTitleStepNotRelatedRule_doesNotApplyFixedTemplate() {
        stubAiResponse(
            "{\"result\":\"retry\",\"message\":\"지금 단계에서 살펴볼 내용과 관련된 궁금한 점을 적어 보세요.\",\"failedRule\":\"NOT_RELATED_TO_STAGE\"}");

        AiFeedbackRequest request = new AiFeedbackRequest();
        request.setType("pre_reading_question");
        request.setStepType("contents");
        request.setBookTitle("우리 낙원에서");
        request.setQaList(List.of(new AiFeedbackRequest.QAItem("수학 문제는 몇 개일까?", "열 개일 것 같다.")));

        AiFeedbackResponse result = service.getFeedback(request);

        assertThat(result.getMessage()).isEqualTo("지금 단계에서 살펴볼 내용과 관련된 궁금한 점을 적어 보세요.");
    }

    /* 회귀: good 판정에는 이 후처리가 전혀 영향을 주지 않음 */
    @Test
    void getFeedback_titleStepGoodResult_unaffectedByFixedTemplateLogic() {
        stubGoodResultResponse();

        AiFeedbackResponse result = service.getFeedback(
            buildTitleRequest("우리 낙원에서", "낙원은 어떤 곳일까?", "모두가 행복하게 사는 곳일 것 같다."));

        assertThat(result.getResult()).isEqualTo("good");
        assertThat(result.getMessage()).isEqualTo("좋아!");
    }

    /*
     * 검증 4: 질문도 활동 목적과 안 맞고 답도 질문과 안 맞는(둘 다 진짜
     * 문제인) 경우. 이 케이스는 서버가 결정적으로 보정하는 대상이 아니라
     * AI가 직접 판단해 message를 만드므로(위 [수정 대상 구분] 프롬프트
     * 규칙이 담당), AI가 이미 "질문과 답을 함께 고쳐 보세요."로 응답하면
     * 서버가 그 문구를 그대로 통과시키는지만 확인한다(NOT_RELATED_TO_BOOK이
     * 아니므로 서버 쪽 고정 문구 로직이 끼어들지 않는다).
     */
    @Test
    void getFeedback_bothQuestionAndAnswerInvalid_passesThroughBothRevisionMessage() {
        stubAiResponse(
            "{\"result\":\"retry\",\"message\":\"질문과 답을 함께 고쳐 보세요. 먼저 제목에서 궁금한 점을 질문으로 만들고, 그 질문에 맞는 생각을 써 보세요.\",\"failedRule\":\"NOT_A_QUESTION\"}");

        AiFeedbackResponse result = service.getFeedback(
            buildTitleRequest("용이 산다", "오늘 급식은 무엇일까 재밌겠다", "ㅋㅋㅋㅋ"));

        assertThat(result.getResult()).isEqualTo("retry");
        assertThat(result.getMessage()).startsWith("질문과 답을 함께 고쳐 보세요.");
    }

    // =========================================================
    // 질문 관련성 vs 답 관련성 오분류 보정("밤" 사례) - 이번 작업의 핵심 검증
    // =========================================================

    /*
     * AI가 "답이 질문과 안 맞는" 경우를 잘못 NOT_RELATED_TO_BOOK으로
     * 반환해도, 학생 질문에 등록된 책 제목("밤")이 문자 그대로 들어 있으면
     * 서버가 이를 명백한 오분류로 판단해 ANSWER_NOT_RELATED로 바로잡고
     * "질문은 괜찮다, 답을 다시 써라"는 문구를 내보내야 한다.
     */
    @Test
    void getFeedback_titleContainedInQuestion_reclassifiesNotRelatedToBookAsAnswerNotRelated() {
        stubAiResponse(
            "{\"result\":\"retry\",\"message\":\"지금 질문은 책 제목 '밤'과 관련이 없어요. '밤'이라는 제목을 보고 궁금한 점을 질문으로 적어 보세요.\",\"failedRule\":\"NOT_RELATED_TO_BOOK\"}");

        AiFeedbackResponse result = service.getFeedback(
            buildTitleRequest("밤", "밤의 의미는 무엇일까?", "내가 만들었다."));

        assertThat(result.getResult()).isEqualTo("retry");
        assertThat(result.getFailedRule()).isEqualTo("ANSWER_NOT_RELATED");
        assertThat(result.getMessage()).isEqualTo(
            "답을 고쳐 보세요. 질문은 책 제목 '밤'과 잘 연결되어 있어요. 질문에서 묻는 내용에 맞게 답을 다시 적어 보세요.");
        assertThat(result.getMessage()).startsWith("답을 고쳐 보세요.");
        assertThat(result.getMessage()).doesNotContain("제목을 보고 궁금한 점을 질문으로 적어 보세요");
    }

    /* 받침 있는 제목("우리 낙원에서"는 받침 없음, "강아지똥"은 받침 있음)에도 조사가 올바르게 붙음 */
    @Test
    void getFeedback_titleContainedInQuestion_usesCorrectParticleForTitleWithFinalConsonant() {
        stubAiResponse(
            "{\"result\":\"retry\",\"message\":\"아무 문구\",\"failedRule\":\"NOT_RELATED_TO_BOOK\"}");

        AiFeedbackResponse result = service.getFeedback(
            buildTitleRequest("강아지똥", "강아지똥의 뜻은 무엇일까?", "내가 만들었다."));

        assertThat(result.getFailedRule()).isEqualTo("ANSWER_NOT_RELATED");
        assertThat(result.getMessage()).isEqualTo(
            "답을 고쳐 보세요. 질문은 책 제목 '강아지똥'과 잘 연결되어 있어요. 질문에서 묻는 내용에 맞게 답을 다시 적어 보세요.");
    }

    /* 질문에 제목이 없으면(진짜 무관) 여전히 기존 고정 문구를 그대로 씀 - 오탐 방지 회귀 */
    @Test
    void getFeedback_titleNotContainedInQuestion_stillUsesTitleMismatchTemplate() {
        stubAiResponse(
            "{\"result\":\"retry\",\"message\":\"아무 문구\",\"failedRule\":\"NOT_RELATED_TO_BOOK\"}");

        AiFeedbackResponse result = service.getFeedback(
            buildTitleRequest("밤", "오늘 급식은 무엇일까?", "김치찌개다."));

        assertThat(result.getFailedRule()).isEqualTo("NOT_RELATED_TO_BOOK");
        assertThat(result.getMessage()).isEqualTo(
            "질문을 고쳐 보세요. 이 책 제목 '밤'을 보고 떠오른 궁금한 점을 다시 질문해 보세요.");
    }

    /* 제목의 핵심 단어 일부만 겹치는 경우(제목 전체 포함이 아님)는 이 결정적 보정을 적용하지 않음 */
    @Test
    void getFeedback_onlyPartialTitleWordOverlap_doesNotTriggerAnswerReclassification() {
        stubAiResponse(
            "{\"result\":\"retry\",\"message\":\"아무 문구\",\"failedRule\":\"NOT_RELATED_TO_BOOK\"}");

        AiFeedbackResponse result = service.getFeedback(
            buildTitleRequest("강아지똥", "강아지는 귀여울까?", "귀여울 것 같다."));

        assertThat(result.getFailedRule()).isEqualTo("NOT_RELATED_TO_BOOK");
        assertThat(result.getMessage()).contains("강아지똥");
    }

    /*
     * 실제 API 검증 중 AI가 result:"good"이면서 failedRule:"NOT_RELATED_TO_BOOK"을
     * 함께 반환하는 자기모순 응답을 실제로 확인했다 - 이런 경우에도
     * 서버가 result를 강제로 "retry"로 바로잡아야 한다(안 그러면 학생이
     * 검사 실패인데도 통과한 것처럼 다음 단계로 넘어가 버림).
     */
    @Test
    void getFeedback_aiReturnsGoodResultWithNonNullFailedRule_forcesResultToRetry() {
        stubAiResponse(
            "{\"result\":\"good\",\"message\":\"아무 문구\",\"failedRule\":\"NOT_RELATED_TO_BOOK\"}");

        AiFeedbackResponse result = service.getFeedback(
            buildTitleRequest("밤", "밤의 의미는 무엇일까?", "내가 만들었다."));

        assertThat(result.getResult()).isEqualTo("retry");
        assertThat(result.getFailedRule()).isEqualTo("ANSWER_NOT_RELATED");
    }

    /*
     * AI가 처음부터 직접 ANSWER_NOT_RELATED로 올바르게 분류했지만(제목
     * 불일치 재분류 경로를 거치지 않음) result만 실수로 "good"으로 남긴
     * 경우에도 일반 정합성 보정(enforceResultFailedRuleConsistency)이
     * retry로 바로잡아야 한다.
     */
    @Test
    void getFeedback_aiDirectlyReturnsAnswerNotRelatedWithGoodResult_forcesResultToRetry() {
        stubAiResponse(
            "{\"result\":\"good\",\"message\":\"질문은 책 제목 '밤'과 관련이 있어요. 하지만 답이 질문과 잘 맞지 않아요.\",\"failedRule\":\"ANSWER_NOT_RELATED\"}");

        AiFeedbackResponse result = service.getFeedback(
            buildTitleRequest("밤", "밤의 의미는 무엇일까?", "내가 만들었다."));

        assertThat(result.getResult()).isEqualTo("retry");
        assertThat(result.getFailedRule()).isEqualTo("ANSWER_NOT_RELATED");
    }

    /* 다른 failedRule(SHALLOW_STAGE_QUESTION 등)에는 이 보정이 전혀 적용되지 않음(회귀) */
    @Test
    void getFeedback_answerMismatchReclassification_onlyAppliesToNotRelatedToBookRule() {
        stubAiResponse(
            "{\"result\":\"retry\",\"message\":\"원본 메시지\",\"failedRule\":\"ANSWER_NOT_RELATED\"}");

        AiFeedbackResponse result = service.getFeedback(
            buildTitleRequest("밤", "밤의 의미는 무엇일까?", "내가 만들었다."));

        // 이미 ANSWER_NOT_RELATED였다면 서버가 메시지를 건드리지 않고 AI 메시지를 그대로 씀
        assertThat(result.getFailedRule()).isEqualTo("ANSWER_NOT_RELATED");
        assertThat(result.getMessage()).isEqualTo("원본 메시지");
    }

    /*
     * contents(차례)/picture(그림)/skim(내용 훑어보기) 단계는 AI가 실제
     * 자료를 볼 수 없으므로, "자료와 정확히 일치하는가"가 아니라 질문·답의
     * 형식과 서로의 연결만으로 판단하도록 프롬프트가 넓게 허용하는지
     * 확인한다. title 단계 전용 기준([title 단계 핵심 원칙] 이하)은 이번
     * 완화 대상이 아니므로 그대로 남아 있는지도 함께 확인한다.
     */
    @Test
    void preReadingQuestionPrompt_contentsPictureSkimStagesAcceptBroadAssociativeQuestions() throws Exception {
        String prompt = privatePrompt("SYSTEM_PROMPT_PRE_READING_QUESTION");

        assertThat(prompt)
            // 5가지 핵심 판단 기준(자료 일치 여부 대신 사용)
            .contains("질문이 질문 문장으로 성립하는가")
            .contains("답이 질문에 대체로 대응하는가")
            .contains("질문과 답이 서로 완전히 딴소리는 아닌가")
            // contents(차례) 통과 예시
            .contains("비밀은 무엇을 말할까?")
            .contains("주인공이 밤에 일어나는 일을 말할 것이다")
            .contains("마지막에는 어떤 일이 생길까?")
            .contains("왜 여행을 떠났을까?")
            .contains("새로운 친구는 누구일까?")
            // picture(그림) 통과 예시
            .contains("이 사람은 왜 놀란 표정을 짓고 있을까?")
            .contains("저 물건은 무엇에 쓰는 걸까?")
            // skim(내용 훑어보기) 통과 예시
            .contains("주문은 왜 자주 나올까?")
            .contains("주인공은 왜 걱정하고 있을까?")
            // title 단계 전용 기준은 이번 작업에서 그대로 유지되어야 함
            .contains("[title 단계 핵심 원칙 - 가장 먼저, 가장 넓게 적용]")
            .contains("이 책 제목이나 책에서 학생이 연상했을 가능성이 조금이라도");

        // 금지된 판정 문구들은 "이렇게 쓰지 말라"는 금지 지시 안에서만 등장해야 함
        String normalizedPrompt = prompt.replaceAll("\\s+", " ");
        assertThat(normalizedPrompt)
            .contains("차례와 관련 없는 질문이에요.")
            .contains("그림에 나온 내용과 관련이 없어요.")
            .contains("글에서 확인할 수 없는 질문이에요.")
            .contains("책의 내용과 연결되지 않았어요.")
            .contains("절대 쓰지 마");
        assertThat(prompt.split("차례와 관련 없는 질문이에요").length - 1).isEqualTo(1);
    }

    /*
     * 실제로 보고된 실패 사례: contents 단계에서 "비밀은 무엇을 말할까?" /
     * "주인공이 밤에 일어나는 일을 말할 것이다."가 AI 응답을 통해 good으로
     * 그대로 전달되는지 end-to-end로 확인한다.
     */
    @Test
    void getFeedback_contentsStepKeywordMeaningQuestion_passesThrough() {
        stubGoodResultResponse();

        AiFeedbackRequest request = new AiFeedbackRequest();
        request.setType("pre_reading_question");
        request.setStepType("contents");
        request.setQaList(List.of(new AiFeedbackRequest.QAItem(
            "비밀은 무엇을 말할까?", "주인공이 밤에 일어나는 일을 말할 것이다.")));

        AiFeedbackResponse result = service.getFeedback(request);

        assertThat(result.getResult()).isEqualTo("good");
        assertThat(result.getMessage()).isEqualTo("좋아!");
    }

    @Test
    void generatePortfolioAnalysis_parsesStructuredStrengthAndImprovement() {
        stubAiResponse("{\"strengthText\":\"활동에 꾸준히 참여하고 있어요.\","
            + "\"improvementText\":\"생각을 한 번 더 구체적으로 적어 보면 좋아요.\"}");

        PortfolioAiAnalysisResponse result = service.generatePortfolioAnalysis(
            "practice", Map.of("participationRate", 80, "activityCount", 5));

        assertThat(result.strengthText()).isEqualTo("활동에 꾸준히 참여하고 있어요.");
        assertThat(result.improvementText()).isEqualTo("생각을 한 번 더 구체적으로 적어 보면 좋아요.");
    }

    @Test
    void generatePortfolioAnalysis_allowsSparseDataWithoutInventingFallbackText() throws Exception {
        stubAiResponse("{\"strengthText\":\"현재 기록에서는 참여를 시작한 점이 보여요.\","
            + "\"improvementText\":\"활동 기록을 조금씩 이어가면 좋아요.\"}");
        Map<String, Object> body = service.buildPortfolioAnalysisRequestBody("individual", Map.of("completedBookCount", 0));
        String messages = body.get("messages").toString();
        assertThat(messages).contains("현재 기록에서는").contains("추측하지 않는다");
        assertThat(service.generatePortfolioAnalysis("individual", Map.of("completedBookCount", 0)).strengthText())
            .startsWith("현재 기록에서는");
    }

    @Test
    void generatePortfolioAnalysis_returnsRetryableStatusWhenOpenAiCallFails() {
        RestTemplate mockRestTemplate = mock(RestTemplate.class);
        when(mockRestTemplate.postForObject(anyString(), any(), eq(Map.class)))
            .thenThrow(new RestClientException("timeout"));
        service.restTemplate = mockRestTemplate;

        assertThatThrownBy(() -> service.generatePortfolioAnalysis("practice", Map.of()))
            .isInstanceOfSatisfying(ResponseStatusException.class,
                error -> assertThat(error.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE));
    }

    @Test
    void generatePortfolioAnalysis_rejectsMalformedOrIncompleteJson() {
        stubAiResponse("{not-json}");
        assertThatThrownBy(() -> service.generatePortfolioAnalysis("practice", Map.of()))
            .isInstanceOfSatisfying(ResponseStatusException.class,
                error -> assertThat(error.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY));

        stubAiResponse("{\"strengthText\":\"좋아요\"}");
        assertThatThrownBy(() -> service.generatePortfolioAnalysis("practice", Map.of()))
            .isInstanceOfSatisfying(ResponseStatusException.class,
                error -> assertThat(error.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY));
    }
}
