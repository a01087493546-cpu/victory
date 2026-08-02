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

    private FeedbackAiService service;

    @BeforeEach
    void setUp() {
        service = new FeedbackAiService(
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
}
