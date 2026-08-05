package com.victory.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import com.victory.dto.PreReadingResponseItem;
import com.victory.dto.PreReadingResponseRequest;
import com.victory.entity.Response;
import com.victory.entity.User;
import com.victory.repository.ClassReadingBookRepository;
import com.victory.repository.ClassStudentRepository;
import com.victory.repository.ResponseRepository;
import com.victory.repository.SchoolClassRepository;
import com.victory.repository.UserRepository;

/*
 * 연습읽기 읽기 전(제목/차례/그림/글) 질문·답 저장/조회, 특히 "차례 없음"
 * 스킵 처리(질문·답을 실제로 작성한 것이 아니므로 빈 값 + skipped=true로
 * 저장하고, passed=true로 정상 완료 취급) 회귀 방지 테스트.
 */
@ExtendWith(MockitoExtension.class)
class ResponseServiceTest {

    private static final Long STUDENT_ID = 1L;
    private static final Long BOOK_ID = 10L;

    @Mock private ResponseRepository responseRepository;
    @Mock private UserRepository userRepository;
    @Mock private ClassStudentRepository classStudentRepository;
    @Mock private SchoolClassRepository schoolClassRepository;
    @Mock private ClassReadingBookRepository classReadingBookRepository;

    private ResponseService service;

    @BeforeEach
    void setUp() {
        service = new ResponseService(
            responseRepository, userRepository, classStudentRepository,
            schoolClassRepository, classReadingBookRepository);

        User student = new User();
        student.setId(STUDENT_ID);
        student.setRole("student");
        org.mockito.Mockito.lenient().when(userRepository.findById(STUDENT_ID)).thenReturn(Optional.of(student));
    }

    private PreReadingResponseRequest normalRequest(String stepType, String question, String answer) {
        PreReadingResponseRequest request = new PreReadingResponseRequest();
        setField(request, "stepType", stepType);
        setField(request, "question", question);
        setField(request, "answer", answer);
        setField(request, "classReadingBookId", BOOK_ID);
        setField(request, "skipped", false);
        return request;
    }

    private PreReadingResponseRequest skipRequest(String stepType) {
        PreReadingResponseRequest request = new PreReadingResponseRequest();
        setField(request, "stepType", stepType);
        setField(request, "question", "");
        setField(request, "answer", "");
        setField(request, "classReadingBookId", BOOK_ID);
        setField(request, "skipped", true);
        return request;
    }

    private void setField(Object target, String fieldName, Object value) {
        try {
            java.lang.reflect.Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    /* 일반 저장(회귀): 기존과 동일하게 질문·답이 그대로 저장되고 skipped=false */
    @Test
    void savePreReadingResponse_normalSave_storesRealQuestionAndAnswer() {
        when(responseRepository.findByStudent_IdAndModeAndContentTypeAndStageAndDeletedAtIsNullOrderByIdAsc(
            STUDENT_ID, "class", "answer", "before")).thenReturn(new ArrayList<>());
        when(responseRepository.save(any(Response.class))).thenAnswer(inv -> inv.getArgument(0));

        PreReadingResponseItem result = service.savePreReadingResponse(
            STUDENT_ID, normalRequest("title", "왜 이런 제목일까?", "궁금해서 그런 것 같다."));

        assertThat(result.getQuestion()).isEqualTo("왜 이런 제목일까?");
        assertThat(result.getAnswer()).isEqualTo("궁금해서 그런 것 같다.");
        assertThat(result.getPassed()).isTrue();
        assertThat(result.isSkipped()).isFalse();
    }

    /* 일반 저장(회귀): 질문 또는 답이 비어 있으면 여전히 400 */
    @Test
    void savePreReadingResponse_normalSaveWithBlankQuestion_throwsBadRequest() {
        assertThatThrownBy(() ->
            service.savePreReadingResponse(STUDENT_ID, normalRequest("title", "", "답만 있음")))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("400");
    }

    @Test
    void savePreReadingResponse_normalSaveWithBlankAnswer_throwsBadRequest() {
        assertThatThrownBy(() ->
            service.savePreReadingResponse(STUDENT_ID, normalRequest("title", "질문만 있음", "")))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("400");
    }

    /* 차례 없음: 질문/답 없이도 저장되고, 실제 텍스트를 가짜로 채우지 않음 */
    @Test
    void savePreReadingResponse_skippedContents_storesEmptyQuestionAndAnswer() {
        when(responseRepository.findByStudent_IdAndModeAndContentTypeAndStageAndDeletedAtIsNullOrderByIdAsc(
            STUDENT_ID, "class", "answer", "before")).thenReturn(new ArrayList<>());
        when(responseRepository.save(any(Response.class))).thenAnswer(inv -> inv.getArgument(0));

        PreReadingResponseItem result = service.savePreReadingResponse(
            STUDENT_ID, skipRequest("contents"));

        assertThat(result.getQuestion()).isEmpty();
        assertThat(result.getAnswer()).isEmpty();
        assertThat(result.isSkipped()).isTrue();
        // 차례 없음도 "이 단계를 정상적으로 완료했다"는 의미로 passed=true를 그대로 쓴다
        assertThat(result.getPassed()).isTrue();
    }

    /* 차례 없음은 "차례 없음"/"건너뜀" 같은 문자열을 학생의 질문·답으로 저장하지 않음 */
    @Test
    void savePreReadingResponse_skippedContents_neverStoresPlaceholderTextAsRealAnswer() {
        when(responseRepository.findByStudent_IdAndModeAndContentTypeAndStageAndDeletedAtIsNullOrderByIdAsc(
            STUDENT_ID, "class", "answer", "before")).thenReturn(new ArrayList<>());

        org.mockito.ArgumentCaptor<Response> captor = org.mockito.ArgumentCaptor.forClass(Response.class);
        when(responseRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

        service.savePreReadingResponse(STUDENT_ID, skipRequest("contents"));

        Response saved = captor.getValue();
        assertThat(saved.getContent()).isEmpty();
        assertThat(saved.getExtraData().get("question")).isEqualTo("");
        assertThat(saved.getExtraData().get("skipped")).isEqualTo(true);
    }

    /* 재접속 복원: getPreReadingResponses가 skipped 플래그를 그대로 돌려줌 */
    @Test
    void getPreReadingResponses_returnsSkippedFlagForRestore() {
        Response skippedResponse = new Response();
        skippedResponse.setId(5L);
        skippedResponse.setContent("");
        skippedResponse.setPassed(true);
        skippedResponse.setExtraData(java.util.Map.of(
            "stepType", "contents", "question", "", "skipped", true));

        when(responseRepository.findByStudent_IdAndModeAndContentTypeAndStageAndDeletedAtIsNullOrderByIdAsc(
            STUDENT_ID, "class", "answer", "before")).thenReturn(List.of(skippedResponse));

        List<PreReadingResponseItem> items = service.getPreReadingResponses(STUDENT_ID);

        assertThat(items).hasSize(1);
        assertThat(items.get(0).getStepType()).isEqualTo("contents");
        assertThat(items.get(0).isSkipped()).isTrue();
        assertThat(items.get(0).getPassed()).isTrue();
    }

    /* 회귀: 일반(정상 작성) 응답에는 skipped가 false로 나옴 */
    @Test
    void getPreReadingResponses_normalResponse_skippedIsFalse() {
        Response normalResponse = new Response();
        normalResponse.setId(6L);
        normalResponse.setContent("궁금해서 그런 것 같다.");
        normalResponse.setPassed(true);
        normalResponse.setExtraData(java.util.Map.of(
            "stepType", "title", "question", "왜 이런 제목일까?"));

        when(responseRepository.findByStudent_IdAndModeAndContentTypeAndStageAndDeletedAtIsNullOrderByIdAsc(
            STUDENT_ID, "class", "answer", "before")).thenReturn(List.of(normalResponse));

        List<PreReadingResponseItem> items = service.getPreReadingResponses(STUDENT_ID);

        assertThat(items.get(0).isSkipped()).isFalse();
    }

    /* 차례 없음 취소 후 실제 질문·답으로 다시 저장하면 skipped=false로 완전히 덮어써짐(동일 stepType 행 재사용) */
    @Test
    void savePreReadingResponse_realAnswerAfterSkip_overwritesSkippedState() {
        Response existingSkipped = new Response();
        existingSkipped.setId(7L);
        existingSkipped.setContent("");
        existingSkipped.setPassed(true);
        existingSkipped.setExtraData(new java.util.HashMap<>(java.util.Map.of(
            "stepType", "contents", "question", "", "skipped", true)));

        when(responseRepository.findByStudent_IdAndModeAndContentTypeAndStageAndDeletedAtIsNullOrderByIdAsc(
            STUDENT_ID, "class", "answer", "before")).thenReturn(new ArrayList<>(List.of(existingSkipped)));
        when(responseRepository.save(any(Response.class))).thenAnswer(inv -> inv.getArgument(0));

        PreReadingResponseItem result = service.savePreReadingResponse(
            STUDENT_ID, normalRequest("contents", "차례를 보니 어떤 일이 생길까?", "모험을 떠날 것 같다."));

        assertThat(result.isSkipped()).isFalse();
        assertThat(result.getQuestion()).isEqualTo("차례를 보니 어떤 일이 생길까?");
        assertThat(result.getAnswer()).isEqualTo("모험을 떠날 것 같다.");
    }

    /* 다른 학생 id로 저장 시도하면 존재하지 않는 학생이라 404 */
    @Test
    void savePreReadingResponse_unknownStudent_throwsNotFound() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.savePreReadingResponse(999L, skipRequest("contents")))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("404");
    }
}
