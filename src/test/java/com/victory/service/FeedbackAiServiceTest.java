package com.victory.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.victory.dto.AiFeedbackRequest;
import com.victory.dto.AiFeedbackResponse;
import com.victory.entity.AiEvaluationAttempt;
import com.victory.entity.ClassReadingBook;
import com.victory.entity.ClassStudent;
import com.victory.entity.SchoolClass;
import com.victory.repository.AiEvaluationAttemptRepository;
import com.victory.repository.ClassReadingBookRepository;
import com.victory.repository.ClassStudentRepository;

/*
 * openaiApiKey는 @Value로 주입되는데 이 테스트는 Spring 컨텍스트 없이 만들어서
 * 항상 null이다. RestTemplate가 null 토큰으로 헤더를 만들다 예외를 던지므로
 * (HttpHeaders.setBearerAuth), 실제 OpenAI 호출/네트워크 없이도
 * "AI 호출이 실패하는 경우"를 그대로 재현할 수 있다 - 이 테스트들이 검증하려는
 * "정상 평가가 안 됐을 때 기록하지 않는다"에 정확히 맞는 상황이다.
 */
@ExtendWith(MockitoExtension.class)
class FeedbackAiServiceTest {

    private static final Long CLASS_ID = 5L;
    private static final Long BOOK_ID = 200L;

    @Mock
    private AiEvaluationAttemptRepository aiEvaluationAttemptRepository;

    @Mock
    private ClassStudentRepository classStudentRepository;

    @Mock
    private ClassReadingBookRepository classReadingBookRepository;

    private FeedbackAiService service;

    @BeforeEach
    void setUp() {
        service = new FeedbackAiService(
            aiEvaluationAttemptRepository, classStudentRepository, classReadingBookRepository);

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
    }

    private AiFeedbackRequest buildRequest(String type, String stepType) {
        AiFeedbackRequest request = new AiFeedbackRequest();
        request.setType(type);
        request.setStepType(stepType);
        request.setQaList(java.util.List.of(new AiFeedbackRequest.QAItem("질문?", "답")));
        return request;
    }

    /* 검증 6: AI 오류 요청은 기록되지 않고 attempt_number도 증가하지 않음 */
    @Test
    void getFeedbackForAuthenticatedStudent_doesNotRecordAttemptWhenAiCallFails() {
        AiFeedbackRequest request = buildRequest("during_reading_question", "direct");

        AiFeedbackResponse result = service.getFeedbackForAuthenticatedStudent(
            42L, request, "eval-key-1", BOOK_ID);

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
     * 않아야 한다.
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

        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
            service.getFeedbackForAuthenticatedStudent(42L, request, "eval-key-1", otherClassBookId)
        )
            .isInstanceOf(org.springframework.web.server.ResponseStatusException.class)
            .hasMessageContaining("403");

        verify(aiEvaluationAttemptRepository, never()).save(any(AiEvaluationAttempt.class));
    }
}
