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
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import com.victory.dto.AiFeedbackRequest;
import com.victory.dto.AiFeedbackResponse;
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

        assertThat(during)
            .contains("정답이 글에 그대로 써 있지 않은 것이 정상")
            .contains("어떤 마음이었을까요?")
            .contains("애매하면 good");
        assertThat(deep)
            .contains("정답이 글에 직접 써 있어야 하는 유형이 아니야")
            .contains("어떤 마음으로 ~했을까요?");
        assertThat(review)
            .contains("어떤 마음으로 친구 옆에 앉았을까요?")
            .contains("친구는 왜 미소를")
            .contains("무슨 색을 좋아할까요?");
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

    private String privatePrompt(String fieldName) throws Exception {
        java.lang.reflect.Field field = FeedbackAiService.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        return (String) field.get(null);
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
            "지금 질문은 책 제목 '우리 낙원에서'와 관련이 없어요. '우리 낙원에서'라는 제목을 보고 궁금한 점을 질문으로 적어 보세요.");
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
            "지금 질문은 책 제목 '백설공주'와 관련이 없어요. '백설공주'라는 제목을 보고 궁금한 점을 질문으로 적어 보세요.");
    }

    /* 받침 있는 제목("마당을 나온 암탉")은 "과"/"이라는" 조사가 올바르게 붙음 */
    @Test
    void getFeedback_titleStepNotRelatedToBook_usesCorrectParticleForTitleWithFinalConsonant() {
        stubAiResponse("{\"result\":\"retry\",\"message\":\"아무 문구\",\"failedRule\":\"NOT_RELATED_TO_BOOK\"}");

        AiFeedbackResponse result = service.getFeedback(
            buildTitleRequest("마당을 나온 암탉", "축구를 잘하는 사람은 누구일까?", "손흥민일 것 같다."));

        assertThat(result.getMessage()).isEqualTo(
            "지금 질문은 책 제목 '마당을 나온 암탉'과 관련이 없어요. '마당을 나온 암탉'이라는 제목을 보고 궁금한 점을 질문으로 적어 보세요.");
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
            "지금 질문은 책 제목과 관련이 없어요. 책 제목을 다시 보고 궁금한 점을 질문으로 적어 보세요.");
    }

    /* bookTitle이 빈 문자열("")이어도 위와 동일하게 안전한 일반 문구를 씀 */
    @Test
    void getFeedback_titleStepNotRelatedToBook_blankBookTitle_usesSafeFallbackMessage() {
        stubAiResponse("{\"result\":\"retry\",\"message\":\"아무 문구\",\"failedRule\":\"NOT_RELATED_TO_BOOK\"}");

        AiFeedbackResponse result = service.getFeedback(buildTitleRequest("", "안녕하세요는 무슨 뜻일까?", "잘 모르겠다."));

        assertThat(result.getMessage()).isEqualTo(
            "지금 질문은 책 제목과 관련이 없어요. 책 제목을 다시 보고 궁금한 점을 질문으로 적어 보세요.");
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
            "질문은 책 제목 '밤'과 관련이 있어요. 하지만 답이 질문과 잘 맞지 않아요. 질문에 알맞은 답을 다시 적어 보세요.");
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
            "질문은 책 제목 '강아지똥'과 관련이 있어요. 하지만 답이 질문과 잘 맞지 않아요. 질문에 알맞은 답을 다시 적어 보세요.");
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
            "지금 질문은 책 제목 '밤'과 관련이 없어요. '밤'이라는 제목을 보고 궁금한 점을 질문으로 적어 보세요.");
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
}
