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
    @Mock private DemoAccountService demoAccountService;

    private ResponseService service;

    @BeforeEach
    void setUp() {
        service = new ResponseService(
            responseRepository, userRepository, classStudentRepository,
            schoolClassRepository, classReadingBookRepository, demoAccountService);

        User student = new User();
        student.setId(STUDENT_ID);
        student.setRole("student");
        org.mockito.Mockito.lenient().when(userRepository.findById(STUDENT_ID)).thenReturn(Optional.of(student));
        org.mockito.Mockito.lenient().when(demoAccountService.isDemoAccount(org.mockito.ArgumentMatchers.anyLong()))
            .thenReturn(false);
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

    /*
     * 심사계정 브라우저 격리: 책수다방 글(책 속 생각 쓰기)은 여러 심사위원이
     * 같은 ss01을 동시에 쓸 수 있으므로 공용 DB에 저장되면 안 된다.
     * 프론트가 이미 이 API를 호출하지 않지만, 우회 호출 시에도 저장이
     * 막히는지 백엔드에서 직접 검증한다.
     */
    @Test
    void saveBookThoughtResponse_demoAccount_neverPersistsToSharedDb() {
        when(demoAccountService.isDemoAccount(STUDENT_ID)).thenReturn(true);

        com.victory.dto.BookThoughtResponseRequest request = new com.victory.dto.BookThoughtResponseRequest();
        setField(request, "questionType", "direct");
        setField(request, "question", "왜 그랬을까?");
        setField(request, "answer", "그런 것 같다.");
        setField(request, "classReadingBookId", BOOK_ID);

        assertThatThrownBy(() -> service.saveBookThoughtResponse(STUDENT_ID, request))
            .isInstanceOf(ResponseStatusException.class);

        org.mockito.Mockito.verify(responseRepository, org.mockito.Mockito.never()).save(any(Response.class));
    }

    // =========================================================
    // 교사용 책수다방 직접 참여 (생각 나누기/답글)
    // =========================================================

    private static final Long TEACHER_ID = 500L;
    private static final Long WRITER_STUDENT_ID = 2L;
    private static final Long CLASS_ID = 20L;
    private static final Long QUESTION_ID = 900L;

    private com.victory.entity.SchoolClass buildSchoolClass(Long id) {
        com.victory.entity.SchoolClass schoolClass = new com.victory.entity.SchoolClass();
        schoolClass.setId(id);
        return schoolClass;
    }

    private com.victory.entity.ClassStudent buildClassStudent(Long classId, Long studentId) {
        com.victory.entity.ClassStudent classStudent = new com.victory.entity.ClassStudent();
        classStudent.setSchoolClass(buildSchoolClass(classId));
        User writer = new User();
        writer.setId(studentId);
        writer.setRole("student");
        classStudent.setStudent(writer);
        return classStudent;
    }

    private Response buildApprovedQuestion(Long id, Long writerId, Long classReadingBookId) {
        Response question = new Response();
        question.setId(id);
        User writer = new User();
        writer.setId(writerId);
        question.setStudent(writer);
        java.util.Map<String, Object> extraData = new java.util.HashMap<>();
        extraData.put("activityType", "book_thought");
        extraData.put("approvalStatus", "APPROVED");
        extraData.put("classReadingBookId", classReadingBookId);
        question.setExtraData(extraData);
        return question;
    }

    private void stubTeacherClassSetup(Long classReadingBookId) {
        User teacher = new User();
        teacher.setId(TEACHER_ID);
        teacher.setRole("teacher");
        when(userRepository.findById(TEACHER_ID)).thenReturn(Optional.of(teacher));
        when(schoolClassRepository.findByTeacherId(TEACHER_ID))
            .thenReturn(Optional.of(buildSchoolClass(CLASS_ID)));
        when(classStudentRepository.findByStudentId(WRITER_STUDENT_ID))
            .thenReturn(Optional.of(buildClassStudent(CLASS_ID, WRITER_STUDENT_ID)));

        com.victory.entity.ClassReadingBook classReadingBook = new com.victory.entity.ClassReadingBook();
        classReadingBook.setId(classReadingBookId);
        classReadingBook.setSchoolClass(buildSchoolClass(CLASS_ID));
        when(classReadingBookRepository.findById(classReadingBookId))
            .thenReturn(Optional.of(classReadingBook));
    }

    /* 교사가 담당 학급의 승인된 질문에 생각을 남기면 즉시 approved로 저장되고(승인 대기 없음) authorRole=teacher로 표시된다 */
    @Test
    void saveBookChatThoughtAsTeacher_savesImmediatelyApprovedWithTeacherAuthorRole() {
        Long bookId = 30L;
        stubTeacherClassSetup(bookId);
        Response question = buildApprovedQuestion(QUESTION_ID, WRITER_STUDENT_ID, bookId);
        when(responseRepository.findById(QUESTION_ID)).thenReturn(Optional.of(question));
        when(responseRepository.findByParent_IdAndModeAndContentTypeAndStudent_IdAndDeletedAtIsNullOrderByIdAsc(
                QUESTION_ID, "class", "thought", TEACHER_ID))
            .thenReturn(new ArrayList<>());
        when(responseRepository.save(any(Response.class))).thenAnswer(inv -> {
            Response saved = inv.getArgument(0);
            saved.setId(999L);
            return saved;
        });

        com.victory.dto.BookChatThoughtRequest request = new com.victory.dto.BookChatThoughtRequest();
        setField(request, "main", "선생님은 이 장면이 인상 깊었어요.");
        setField(request, "reason", "친구를 배려한 점이 좋았기 때문이에요.");

        com.victory.dto.BookChatThoughtItem result =
            service.saveBookChatThoughtAsTeacher(TEACHER_ID, QUESTION_ID, request);

        assertThat(result.getAuthorRole()).isEqualTo("teacher");

        org.mockito.ArgumentCaptor<Response> captor = org.mockito.ArgumentCaptor.forClass(Response.class);
        org.mockito.Mockito.verify(responseRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo("approved");
    }

    /* 교사가 담당하지 않는(다른 학급) 질문에는 접근할 수 없다 */
    @Test
    void saveBookChatThoughtAsTeacher_rejectsQuestionFromDifferentClass() {
        Long bookId = 31L;
        User teacher = new User();
        teacher.setId(TEACHER_ID);
        teacher.setRole("teacher");
        when(userRepository.findById(TEACHER_ID)).thenReturn(Optional.of(teacher));
        when(schoolClassRepository.findByTeacherId(TEACHER_ID))
            .thenReturn(Optional.of(buildSchoolClass(CLASS_ID)));
        // 질문 작성자는 다른 학급(CLASS_ID + 1) 소속
        when(classStudentRepository.findByStudentId(WRITER_STUDENT_ID))
            .thenReturn(Optional.of(buildClassStudent(CLASS_ID + 1, WRITER_STUDENT_ID)));

        Response question = buildApprovedQuestion(QUESTION_ID, WRITER_STUDENT_ID, bookId);
        when(responseRepository.findById(QUESTION_ID)).thenReturn(Optional.of(question));

        com.victory.dto.BookChatThoughtRequest request = new com.victory.dto.BookChatThoughtRequest();
        setField(request, "main", "생각");
        setField(request, "reason", "이유");

        assertThatThrownBy(() -> service.saveBookChatThoughtAsTeacher(TEACHER_ID, QUESTION_ID, request))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("403");

        org.mockito.Mockito.verify(responseRepository, org.mockito.Mockito.never()).save(any(Response.class));
    }

    /*
     * 재발 버그 회귀 테스트: REJECTED 질문을 작성자가 수정해 재제출하면,
     * 그 질문이 예전에 APPROVED였을 때 친구들이 남긴 퀴즈 답/생각/
     * 답글(생각에 달린 답글 + 질문 자체에 바로 달린 답글 모두)이 실제로
     * 소프트 삭제되어야 한다 - 작성자 화면에서만 숨기는 방식은 실패다.
     */
    @Test
    void reviseRejectedBookThoughtResponse_softDeletesAllExistingParticipation() {
        Response rejectedQuestion = new Response();
        rejectedQuestion.setId(QUESTION_ID);
        User writer = new User();
        writer.setId(WRITER_STUDENT_ID);
        writer.setRole("student");
        rejectedQuestion.setStudent(writer);
        java.util.Map<String, Object> questionExtra = new java.util.HashMap<>();
        questionExtra.put("activityType", "book_thought");
        questionExtra.put("approvalStatus", "REJECTED");
        rejectedQuestion.setExtraData(questionExtra);

        when(responseRepository.findById(QUESTION_ID)).thenReturn(Optional.of(rejectedQuestion));
        org.mockito.Mockito.lenient().when(demoAccountService.isDemoAccount(WRITER_STUDENT_ID)).thenReturn(false);

        Response quizAnswer = new Response();
        quizAnswer.setId(501L);
        Response friendThought = new Response();
        friendThought.setId(502L);
        Response replyUnderThought = new Response();
        replyUnderThought.setId(503L);
        Response replyUnderQuestion = new Response();
        replyUnderQuestion.setId(504L);

        when(responseRepository.findByParent_IdAndModeAndContentTypeAndDeletedAtIsNullOrderByIdAsc(
            QUESTION_ID, "class", "quiz_answer")).thenReturn(List.of(quizAnswer));
        when(responseRepository.findByParent_IdAndModeAndContentTypeAndDeletedAtIsNullOrderByIdAsc(
            QUESTION_ID, "class", "thought")).thenReturn(List.of(friendThought));
        when(responseRepository.findByParent_IdAndModeAndContentTypeAndDeletedAtIsNullOrderByIdAsc(
            friendThought.getId(), "class", "reply")).thenReturn(List.of(replyUnderThought));
        when(responseRepository.findByParent_IdAndModeAndContentTypeAndDeletedAtIsNullOrderByIdAsc(
            QUESTION_ID, "class", "reply")).thenReturn(List.of(replyUnderQuestion));
        when(responseRepository.save(any(Response.class))).thenAnswer(inv -> inv.getArgument(0));

        com.victory.dto.BookThoughtResponseRequest request = new com.victory.dto.BookThoughtResponseRequest();
        setField(request, "questionType", "direct");
        setField(request, "question", "고친 질문");
        setField(request, "answer", "고친 답");
        setField(request, "classReadingBookId", 10L);

        service.reviseRejectedBookThoughtResponse(WRITER_STUDENT_ID, QUESTION_ID, request);

        assertThat(quizAnswer.getDeletedAt()).isNotNull();
        assertThat(friendThought.getDeletedAt()).isNotNull();
        assertThat(replyUnderThought.getDeletedAt()).isNotNull();
        assertThat(replyUnderQuestion.getDeletedAt()).isNotNull();
    }
}
