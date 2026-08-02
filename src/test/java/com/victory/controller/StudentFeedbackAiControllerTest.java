package com.victory.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.server.ResponseStatusException;

import com.victory.dto.AiFeedbackRequest;
import com.victory.dto.AiFeedbackResponse;
import com.victory.dto.StudentAiFeedbackRequest;
import com.victory.service.FeedbackAiService;

class StudentFeedbackAiControllerTest {

    private final FeedbackAiService feedbackAiService = mock(FeedbackAiService.class);
    private final StudentFeedbackAiController controller =
        new StudentFeedbackAiController(feedbackAiService);

    private StudentAiFeedbackRequest buildRequest() {
        StudentAiFeedbackRequest request = new StudentAiFeedbackRequest();
        setField(request, "question", "질문?");
        setField(request, "answer", "답");
        setField(request, "activityType", "during_reading_question");
        setField(request, "questionType", "direct");
        setField(request, "evaluationKey", "eval-key-1");
        setField(request, "classReadingBookId", 200L);
        return request;
    }

    private StudentAiFeedbackRequest buildRequest(String activityType, String answer) {
        StudentAiFeedbackRequest request = buildRequest();
        setField(request, "activityType", activityType);
        setField(request, "answer", answer);
        return request;
    }

    private void setField(Object target, String fieldName, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    /* 검증 8: 인증 학생 API는 JWT 없으면 401 */
    @Test
    void getAiReviewForAuthenticatedStudent_throws401WhenNoAuthentication() {
        assertThatThrownBy(() ->
            controller.getAiReviewForAuthenticatedStudent(buildRequest(), null)
        )
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("401");
    }

    /* 검증 10: student JWT로 호출 시 JWT의 사용자 ID로 저장 */
    @Test
    void getAiReviewForAuthenticatedStudent_usesStudentIdFromAuthentication() {
        Long studentIdFromJwt = 77L;
        Authentication authentication = new UsernamePasswordAuthenticationToken(
            studentIdFromJwt, null, List.of(new SimpleGrantedAuthority("student")));

        AiFeedbackResponse expected = new AiFeedbackResponse("good", "잘했어", null, null);
        when(feedbackAiService.getFeedbackForAuthenticatedStudent(
                eq(studentIdFromJwt), any(AiFeedbackRequest.class), eq("eval-key-1"), eq(200L), isNull()))
            .thenReturn(expected);

        AiFeedbackResponse result = controller.getAiReviewForAuthenticatedStudent(
            buildRequest(), authentication);

        assertThat(result).isEqualTo(expected);
        verify(feedbackAiService).getFeedbackForAuthenticatedStudent(
            eq(studentIdFromJwt), any(AiFeedbackRequest.class), eq("eval-key-1"), eq(200L), isNull());
    }

    /* 검증: 심화 연습(during_reading_practice_deep)은 answer가 빈 문자열이어도 400 없이 통과 */
    @Test
    void getAiReviewForAuthenticatedStudent_allowsBlankAnswerForDeepPractice() {
        Long studentIdFromJwt = 77L;
        Authentication authentication = new UsernamePasswordAuthenticationToken(
            studentIdFromJwt, null, List.of(new SimpleGrantedAuthority("student")));

        StudentAiFeedbackRequest request = buildRequest("during_reading_practice_deep", "");

        AiFeedbackResponse expected = new AiFeedbackResponse("good", "잘했어", null, null);
        when(feedbackAiService.getFeedbackForAuthenticatedStudent(
                eq(studentIdFromJwt), any(AiFeedbackRequest.class), eq("eval-key-1"), eq(200L), isNull()))
            .thenReturn(expected);

        AiFeedbackResponse result = controller.getAiReviewForAuthenticatedStudent(request, authentication);

        assertThat(result).isEqualTo(expected);
    }

    /* 검증: 심화 연습이 아닌 활동은 answer가 비어 있으면 400 */
    @Test
    void getAiReviewForAuthenticatedStudent_throws400WhenAnswerBlankForNonDeepPractice() {
        Long studentIdFromJwt = 77L;
        Authentication authentication = new UsernamePasswordAuthenticationToken(
            studentIdFromJwt, null, List.of(new SimpleGrantedAuthority("student")));

        StudentAiFeedbackRequest request = buildRequest("during_reading_question", "");

        assertThatThrownBy(() ->
            controller.getAiReviewForAuthenticatedStudent(request, authentication)
        )
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("400");
    }

    /* 검증: 읽기 후 book_question 정상 요청은 200 */
    @Test
    void getAiReviewForAuthenticatedStudent_bookQuestionSucceeds() {
        Long studentIdFromJwt = 77L;
        Authentication authentication = new UsernamePasswordAuthenticationToken(
            studentIdFromJwt, null, List.of(new SimpleGrantedAuthority("student")));

        StudentAiFeedbackRequest request = buildRequest("book_question", "답");

        AiFeedbackResponse expected = new AiFeedbackResponse("good", "잘했어", null, null);
        when(feedbackAiService.getFeedbackForAuthenticatedStudent(
                eq(studentIdFromJwt), any(AiFeedbackRequest.class), eq("eval-key-1"), eq(200L), isNull()))
            .thenReturn(expected);

        AiFeedbackResponse result = controller.getAiReviewForAuthenticatedStudent(request, authentication);

        assertThat(result).isEqualTo(expected);
    }

    /* 검증: book_question은 question이 비어 있으면 400 */
    @Test
    void getAiReviewForAuthenticatedStudent_throws400WhenBookQuestionQuestionBlank() {
        Authentication authentication = new UsernamePasswordAuthenticationToken(
            77L, null, List.of(new SimpleGrantedAuthority("student")));

        StudentAiFeedbackRequest request = buildRequest("book_question", "답");
        setField(request, "question", "");

        assertThatThrownBy(() ->
            controller.getAiReviewForAuthenticatedStudent(request, authentication)
        )
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("400");
    }

    /*
     * 검증: final_summary는 question/answer 없이(빈 문자열) summaryText만
     * 있어도 200 - 내부적으로 qaList/summaryText가 그대로 매핑되는지도 확인.
     */
    @Test
    void getAiReviewForAuthenticatedStudent_finalSummarySucceedsWithoutQuestionAnswer() {
        Long studentIdFromJwt = 77L;
        Authentication authentication = new UsernamePasswordAuthenticationToken(
            studentIdFromJwt, null, List.of(new SimpleGrantedAuthority("student")));

        StudentAiFeedbackRequest request = buildRequest("final_summary", "");
        setField(request, "question", "");
        setField(request, "summaryText", "최종 간추리기 내용입니다.");
        setField(request, "qaList", List.of(
            new AiFeedbackRequest.QAItem("질문1?", "답1"),
            new AiFeedbackRequest.QAItem("질문2?", "답2")
        ));

        AiFeedbackResponse expected = new AiFeedbackResponse("good", "잘했어", null, null);
        when(feedbackAiService.getFeedbackForAuthenticatedStudent(
                eq(studentIdFromJwt), any(AiFeedbackRequest.class), eq("eval-key-1"), eq(200L), isNull()))
            .thenReturn(expected);

        AiFeedbackResponse result = controller.getAiReviewForAuthenticatedStudent(request, authentication);

        assertThat(result).isEqualTo(expected);

        ArgumentCaptor<AiFeedbackRequest> captor = ArgumentCaptor.forClass(AiFeedbackRequest.class);
        verify(feedbackAiService).getFeedbackForAuthenticatedStudent(
            eq(studentIdFromJwt), captor.capture(), eq("eval-key-1"), eq(200L), isNull());

        AiFeedbackRequest internalRequest = captor.getValue();
        assertThat(internalRequest.getSummaryText()).isEqualTo("최종 간추리기 내용입니다.");
        assertThat(internalRequest.getQaList()).hasSize(2);
        assertThat(internalRequest.getQaList().get(0).getQuestion()).isEqualTo("질문1?");
    }

    /* 검증: final_summary는 summaryText가 비어 있으면 400 */
    @Test
    void getAiReviewForAuthenticatedStudent_throws400WhenFinalSummaryTextBlank() {
        Authentication authentication = new UsernamePasswordAuthenticationToken(
            77L, null, List.of(new SimpleGrantedAuthority("student")));

        StudentAiFeedbackRequest request = buildRequest("final_summary", "");
        setField(request, "question", "");
        setField(request, "summaryText", "");

        assertThatThrownBy(() ->
            controller.getAiReviewForAuthenticatedStudent(request, authentication)
        )
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("400");
    }

    /*
     * 개별읽기: readingRecordId가 채워진 요청은 그대로 서비스에 전달된다
     * (classReadingBookId는 null).
     */
    @Test
    void getAiReviewForAuthenticatedStudent_passesReadingRecordIdForIndividualReading() {
        Long studentIdFromJwt = 77L;
        Authentication authentication = new UsernamePasswordAuthenticationToken(
            studentIdFromJwt, null, List.of(new SimpleGrantedAuthority("student")));

        StudentAiFeedbackRequest request = buildRequest();
        setField(request, "classReadingBookId", null);
        setField(request, "readingRecordId", 900L);

        AiFeedbackResponse expected = new AiFeedbackResponse("good", "잘했어", null, null);
        when(feedbackAiService.getFeedbackForAuthenticatedStudent(
                eq(studentIdFromJwt), any(AiFeedbackRequest.class), eq("eval-key-1"), isNull(), eq(900L)))
            .thenReturn(expected);

        AiFeedbackResponse result = controller.getAiReviewForAuthenticatedStudent(request, authentication);

        assertThat(result).isEqualTo(expected);
        verify(feedbackAiService).getFeedbackForAuthenticatedStudent(
            eq(studentIdFromJwt), any(AiFeedbackRequest.class), eq("eval-key-1"), isNull(), eq(900L));
    }

    /*
     * 개별읽기 간추리기(individual_summary)도 final_summary와 같이
     * summaryText만 필수이고 qaList/summaryText가 그대로 매핑된다.
     */
    @Test
    void getAiReviewForAuthenticatedStudent_individualSummarySucceedsWithoutQuestionAnswer() {
        Long studentIdFromJwt = 77L;
        Authentication authentication = new UsernamePasswordAuthenticationToken(
            studentIdFromJwt, null, List.of(new SimpleGrantedAuthority("student")));

        StudentAiFeedbackRequest request = buildRequest("individual_summary", "");
        setField(request, "question", "");
        setField(request, "classReadingBookId", null);
        setField(request, "readingRecordId", 900L);
        setField(request, "summaryText", "개별읽기 최종 간추리기 내용입니다.");
        setField(request, "qaList", List.of(new AiFeedbackRequest.QAItem("질문1?", "답1")));

        AiFeedbackResponse expected = new AiFeedbackResponse("good", "잘했어", null, null);
        when(feedbackAiService.getFeedbackForAuthenticatedStudent(
                eq(studentIdFromJwt), any(AiFeedbackRequest.class), eq("eval-key-1"), isNull(), eq(900L)))
            .thenReturn(expected);

        AiFeedbackResponse result = controller.getAiReviewForAuthenticatedStudent(request, authentication);

        assertThat(result).isEqualTo(expected);

        ArgumentCaptor<AiFeedbackRequest> captor = ArgumentCaptor.forClass(AiFeedbackRequest.class);
        verify(feedbackAiService).getFeedbackForAuthenticatedStudent(
            eq(studentIdFromJwt), captor.capture(), eq("eval-key-1"), isNull(), eq(900L));

        assertThat(captor.getValue().getSummaryText()).isEqualTo("개별읽기 최종 간추리기 내용입니다.");
        assertThat(captor.getValue().getQaList()).hasSize(1);
    }

    /* individual_summary도 summaryText가 비어 있으면 400 */
    @Test
    void getAiReviewForAuthenticatedStudent_throws400WhenIndividualSummaryTextBlank() {
        Authentication authentication = new UsernamePasswordAuthenticationToken(
            77L, null, List.of(new SimpleGrantedAuthority("student")));

        StudentAiFeedbackRequest request = buildRequest("individual_summary", "");
        setField(request, "question", "");
        setField(request, "classReadingBookId", null);
        setField(request, "readingRecordId", 900L);
        setField(request, "summaryText", "");

        assertThatThrownBy(() ->
            controller.getAiReviewForAuthenticatedStudent(request, authentication)
        )
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("400");
    }

    /*
     * 검증 11: body에 다른 studentId를 넣더라도 무시되거나 요청 자체에서
     * 받을 수 없음 - StudentAiFeedbackRequest에는 애초에 studentId 필드가
     * 없으므로(컴파일 타임에 보장됨) 리플렉션으로 확인한다.
     */
    @Test
    void studentAiFeedbackRequest_hasNoStudentIdField() {
        boolean hasStudentIdField = java.util.Arrays.stream(
                StudentAiFeedbackRequest.class.getDeclaredFields())
            .anyMatch(field -> "studentId".equalsIgnoreCase(field.getName()));

        assertThat(hasStudentIdField).isFalse();
    }
}
