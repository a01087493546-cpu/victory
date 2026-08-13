package com.victory.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import com.victory.dto.AfterReadingDataResponse;
import com.victory.dto.AfterReadingQuestionSaveRequest;
import com.victory.dto.AfterReadingSummarySaveRequest;
import com.victory.dto.AfterReadingTypePracticeRequest;
import com.victory.entity.ClassReadingBook;
import com.victory.entity.ClassStudent;
import com.victory.entity.Response;
import com.victory.entity.SchoolClass;
import com.victory.entity.Summary;
import com.victory.entity.User;
import com.victory.repository.ClassReadingBookRepository;
import com.victory.repository.ClassStudentRepository;
import com.victory.repository.ContentLikeRepository;
import com.victory.repository.PracticeProgressRepository;
import com.victory.repository.ResponseRepository;
import com.victory.repository.SchoolClassRepository;
import com.victory.repository.SummaryRepository;
import com.victory.repository.UserRepository;

/*
 * "루미 피드백 통과 = 자동저장" 정책 검증 - 통과한 시점에만 저장되고,
 * 실패한 재시도는 기존 통과본을 덮어쓰지 않으며, 재통과하면 같은 행을
 * UPDATE(새 row 누적 X)한다는 것이 핵심이다.
 */
class AfterReadingServiceTest {

    private static final Long STUDENT_ID = 300L;
    private static final Long OTHER_STUDENT_ID = 301L;
    private static final Long CLASS_ID = 30L;
    private static final Long OTHER_CLASS_ID = 31L;
    private static final Long CLASS_READING_BOOK_ID = 700L;
    private static final Long TEACHER_ID = 900L;
    private static final Long SUMMARY_ID = 1000L;

    private final SummaryRepository summaryRepository = mock(SummaryRepository.class);
    private final ResponseRepository responseRepository = mock(ResponseRepository.class);
    private final ContentLikeRepository contentLikeRepository = mock(ContentLikeRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final ClassStudentRepository classStudentRepository = mock(ClassStudentRepository.class);
    private final ClassReadingBookRepository classReadingBookRepository = mock(ClassReadingBookRepository.class);
    private final SchoolClassRepository schoolClassRepository = mock(SchoolClassRepository.class);
    private final PracticeProgressRepository practiceProgressRepository = mock(PracticeProgressRepository.class);
    private final PracticeProgressService practiceProgressService = mock(PracticeProgressService.class);
    private final DemoAccountService demoAccountService = mock(DemoAccountService.class);

    private final AfterReadingService service = new AfterReadingService(
        summaryRepository, responseRepository, contentLikeRepository, userRepository,
        classStudentRepository, classReadingBookRepository, schoolClassRepository,
        practiceProgressRepository, practiceProgressService, demoAccountService);

    private User buildStudent() {
        User student = new User();
        student.setId(STUDENT_ID);
        student.setName("학생1");
        student.setRole("student");
        return student;
    }

    private void stubClassSetup(List<Response> existingResponses) {
        User student = buildStudent();
        when(userRepository.findById(STUDENT_ID)).thenReturn(Optional.of(student));

        SchoolClass schoolClass = new SchoolClass();
        schoolClass.setId(CLASS_ID);

        ClassStudent classStudent = new ClassStudent();
        classStudent.setSchoolClass(schoolClass);
        classStudent.setStudent(student);
        when(classStudentRepository.findByStudentId(STUDENT_ID)).thenReturn(Optional.of(classStudent));

        ClassReadingBook readingBook = new ClassReadingBook();
        readingBook.setId(CLASS_READING_BOOK_ID);
        readingBook.setSchoolClass(schoolClass);
        when(classReadingBookRepository.findById(CLASS_READING_BOOK_ID)).thenReturn(Optional.of(readingBook));

        when(responseRepository.findByStudent_IdAndModeAndContentTypeAndStageAndDeletedAtIsNullOrderByIdAsc(
            any(), any(), any(), any())).thenReturn(existingResponses);
        // save()가 새로 만든 Response도 같은 리스트에 반영해야, save() 직후
        // getMyAfterReadingData()가 다시 조회할 때(findBy...가 같은 리스트를
        // 리턴하는 이 목 설정에서) 방금 저장한 값을 실제 DB처럼 곧바로 볼 수 있다.
        when(responseRepository.save(any(Response.class)))
            .thenAnswer(invocation -> {
                Response saved = invocation.getArgument(0);
                if (!existingResponses.contains(saved)) {
                    existingResponses.add(saved);
                }
                return saved;
            });

        when(practiceProgressRepository.findByStudent_Id(STUDENT_ID)).thenReturn(Optional.empty());
        when(summaryRepository.findByStudent_IdAndClassReadingBookId(STUDENT_ID, CLASS_READING_BOOK_ID))
            .thenReturn(Optional.empty());
    }

    private AfterReadingTypePracticeRequest buildTypePracticeRequest(String q1, String a1) {
        AfterReadingTypePracticeRequest request = new AfterReadingTypePracticeRequest();
        setFieldQuiet(request, "classReadingBookId", CLASS_READING_BOOK_ID);
        setFieldQuiet(request, "bookType", "story");
        setFieldQuiet(request, "question1", q1);
        setFieldQuiet(request, "answer1", a1);
        setFieldQuiet(request, "question2", "질문2");
        setFieldQuiet(request, "answer2", "답2");
        return request;
    }

    private void setField(Object target, String fieldName, Object value) throws ReflectiveOperationException {
        var field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    @Test
    void saveTypePracticeAnswers_firstPass_createsNewResponse() {
        stubClassSetup(new ArrayList<>());

        AfterReadingDataResponse result = service.saveTypePracticeAnswers(
            STUDENT_ID, buildTypePracticeRequest("질문1", "답1"));

        assertThat(result.getTypePracticeAnswers()).containsKey("story");
        assertThat(result.getTypePracticeAnswers().get("story").getQuestion1()).isEqualTo("질문1");
        verify(responseRepository).save(any(Response.class));
    }

    @Test
    void saveTypePracticeAnswers_rePass_overwritesSameRowInsteadOfAppending() {
        Response existing = new Response();
        existing.setId(1L);
        existing.setStudent(buildStudent());
        var extraData = new java.util.HashMap<String, Object>();
        extraData.put("activityType", "after_reading_type_practice");
        extraData.put("classReadingBookId", CLASS_READING_BOOK_ID);
        extraData.put("bookType", "story");
        extraData.put("question1", "예전 질문1");
        extraData.put("answer1", "예전 답1");
        extraData.put("question2", "예전 질문2");
        extraData.put("answer2", "예전 답2");
        existing.setExtraData(extraData);

        List<Response> existingResponses = new ArrayList<>();
        existingResponses.add(existing);
        stubClassSetup(existingResponses);

        AfterReadingDataResponse result = service.saveTypePracticeAnswers(
            STUDENT_ID, buildTypePracticeRequest("새 질문1", "새 답1"));

        assertThat(result.getTypePracticeAnswers().get("story").getQuestion1()).isEqualTo("새 질문1");
        // 새 Response를 만들지 않고 기존 id=1L 행을 그대로 덮어써 저장했는지 확인.
        verify(responseRepository).save(existing);
        assertThat(existing.getExtraData().get("question1")).isEqualTo("새 질문1");
    }

    @Test
    void saveAfterReadingQuestionDraft_savesSingleQuestionWithoutRequiringOtherTwo() {
        stubClassSetup(new ArrayList<>());

        AfterReadingQuestionSaveRequest request = new AfterReadingQuestionSaveRequest();
        setFieldQuiet(request, "classReadingBookId", CLASS_READING_BOOK_ID);
        setFieldQuiet(request, "index", 1);
        setFieldQuiet(request, "question", "주인공은 왜 집을 나갔나요?");
        setFieldQuiet(request, "answer", "친구를 찾으러 갔기 때문입니다.");

        AfterReadingDataResponse result = service.saveAfterReadingQuestionDraft(STUDENT_ID, request);

        assertThat(result.getQuestions()).hasSize(1);
        assertThat(result.getQuestions().get(0).getAnswer()).isEqualTo("친구를 찾으러 갔기 때문입니다.");
        // afterDone/보상은 이 경로에서 절대 건드리지 않는다 - 최종 완료(saveMyAfterReadingData)와만 연결된다.
        assertThat(result.getAfterDone()).isFalse();
    }

    @Test
    void saveAfterReadingSummaryDraft_savesSummaryWithoutMarkingShared() {
        stubClassSetup(new ArrayList<>());

        AfterReadingSummarySaveRequest request = new AfterReadingSummarySaveRequest();
        setFieldQuiet(request, "classReadingBookId", CLASS_READING_BOOK_ID);
        setFieldQuiet(request, "bookType", "story");
        setFieldQuiet(request, "summary", "간추리기 초안입니다.");

        service.saveAfterReadingSummaryDraft(STUDENT_ID, request);

        var summaryCaptor = org.mockito.ArgumentCaptor.forClass(Summary.class);
        verify(summaryRepository).save(summaryCaptor.capture());

        Summary saved = summaryCaptor.getValue();
        assertThat(saved.getSummaryText()).isEqualTo("간추리기 초안입니다.");
        // isShared는 손대지 않으므로 Summary::new의 기본값(false)이 그대로 유지된다 -
        // 최종 완료 전까지 "우리 반 간추리기 모음"에 새어나가면 안 되기 때문이다.
        assertThat(saved.getIsShared()).isFalse();
    }

    private void setFieldQuiet(Object target, String fieldName, Object value) {
        try {
            setField(target, fieldName, value);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    /*
     * 간추리기 승인관리(PENDING/APPROVED/REJECTED) 정책 검증 - 학생 자기
     * PENDING/REJECTED 글은 본인에게만 보이고, 교사 승인/거절은 담당 학급
     * 안에서만 되며, 재수정은 REJECTED 글만 가능하고 PENDING으로 되돌아간다.
     */

    private SchoolClass buildSchoolClass(Long id) {
        SchoolClass schoolClass = new SchoolClass();
        schoolClass.setId(id);
        return schoolClass;
    }

    private User buildTeacher() {
        User teacher = new User();
        teacher.setId(TEACHER_ID);
        teacher.setName("선생님1");
        teacher.setRole("teacher");
        return teacher;
    }

    private void stubTeacherClassSetup() {
        when(userRepository.findById(TEACHER_ID)).thenReturn(Optional.of(buildTeacher()));
        when(schoolClassRepository.findByTeacherId(TEACHER_ID)).thenReturn(Optional.of(buildSchoolClass(CLASS_ID)));
    }

    private Summary buildSummary(Long studentId, String status) {
        Summary summary = new Summary();
        summary.setId(SUMMARY_ID);
        User student = new User();
        student.setId(studentId);
        student.setName("학생1");
        summary.setStudent(student);
        summary.setClassReadingBookId(CLASS_READING_BOOK_ID);
        summary.setSummaryText("간추리기 내용입니다.");
        summary.setIsShared(true);
        summary.setStatus(status);
        summary.setAiPassed(true);
        return summary;
    }

    private void stubStudentClassLookup(Long studentId, Long classId) {
        User student = new User();
        student.setId(studentId);
        student.setRole("student");
        ClassStudent classStudent = new ClassStudent();
        classStudent.setStudent(student);
        classStudent.setSchoolClass(buildSchoolClass(classId));
        when(classStudentRepository.findByStudentId(studentId)).thenReturn(Optional.of(classStudent));
    }

    @Test
    void reviewSummary_approve_setsApprovedAndClearsRejectionReason() {
        stubTeacherClassSetup();
        stubStudentClassLookup(STUDENT_ID, CLASS_ID);
        Summary summary = buildSummary(STUDENT_ID, "pending");
        summary.setRejectionReason("이전 거절 사유");
        when(summaryRepository.findById(SUMMARY_ID)).thenReturn(Optional.of(summary));
        when(classReadingBookRepository.findById(CLASS_READING_BOOK_ID))
            .thenReturn(Optional.of(buildClassReadingBook()));
        when(summaryRepository.save(any(Summary.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.reviewSummary(TEACHER_ID, CLASS_ID, SUMMARY_ID, "approved", null);

        assertThat(result.getStatus()).isEqualTo("approved");
        assertThat(summary.getRejectionReason()).isNull();
    }

    @Test
    void reviewSummary_reject_requiresReasonAndSetsRejected() {
        stubTeacherClassSetup();
        stubStudentClassLookup(STUDENT_ID, CLASS_ID);
        Summary summary = buildSummary(STUDENT_ID, "pending");
        when(summaryRepository.findById(SUMMARY_ID)).thenReturn(Optional.of(summary));
        when(classReadingBookRepository.findById(CLASS_READING_BOOK_ID))
            .thenReturn(Optional.of(buildClassReadingBook()));
        when(summaryRepository.save(any(Summary.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThatThrownBy(() -> service.reviewSummary(TEACHER_ID, CLASS_ID, SUMMARY_ID, "rejected", " "))
            .isInstanceOf(ResponseStatusException.class);
        verify(summaryRepository, never()).save(any(Summary.class));

        var result = service.reviewSummary(TEACHER_ID, CLASS_ID, SUMMARY_ID, "rejected", "중요한 내용이 빠졌어요.");

        assertThat(result.getStatus()).isEqualTo("rejected");
        assertThat(summary.getRejectionReason()).isEqualTo("중요한 내용이 빠졌어요.");
    }

    @Test
    void reviewSummary_approvedToPending_clearsReasonAndPreservesLikes() {
        stubTeacherClassSetup();
        stubStudentClassLookup(STUDENT_ID, CLASS_ID);
        Summary summary = buildSummary(STUDENT_ID, "approved");
        when(summaryRepository.findById(SUMMARY_ID)).thenReturn(Optional.of(summary));
        when(classReadingBookRepository.findById(CLASS_READING_BOOK_ID))
            .thenReturn(Optional.of(buildClassReadingBook()));
        when(summaryRepository.save(any(Summary.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.reviewSummary(TEACHER_ID, CLASS_ID, SUMMARY_ID, "pending", null);

        assertThat(result.getStatus()).isEqualTo("pending");
        assertThat(summary.getRejectionReason()).isNull();
        verify(contentLikeRepository, never()).delete(any());
    }

    @Test
    void reviewSummary_rejectedToPending_clearsRejectionReason() {
        stubTeacherClassSetup();
        stubStudentClassLookup(STUDENT_ID, CLASS_ID);
        Summary summary = buildSummary(STUDENT_ID, "rejected");
        summary.setRejectionReason("이전 거절 사유");
        when(summaryRepository.findById(SUMMARY_ID)).thenReturn(Optional.of(summary));
        when(classReadingBookRepository.findById(CLASS_READING_BOOK_ID))
            .thenReturn(Optional.of(buildClassReadingBook()));
        when(summaryRepository.save(any(Summary.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.reviewSummary(TEACHER_ID, CLASS_ID, SUMMARY_ID, "pending", null);

        assertThat(result.getStatus()).isEqualTo("pending");
        assertThat(summary.getRejectionReason()).isNull();
    }

    @Test
    void reviewSummary_approvedToRejected_setsReasonWithoutTouchingLikes() {
        stubTeacherClassSetup();
        stubStudentClassLookup(STUDENT_ID, CLASS_ID);
        Summary summary = buildSummary(STUDENT_ID, "approved");
        when(summaryRepository.findById(SUMMARY_ID)).thenReturn(Optional.of(summary));
        when(classReadingBookRepository.findById(CLASS_READING_BOOK_ID))
            .thenReturn(Optional.of(buildClassReadingBook()));
        when(summaryRepository.save(any(Summary.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.reviewSummary(TEACHER_ID, CLASS_ID, SUMMARY_ID, "rejected", "다시 정리해볼까?");

        assertThat(result.getStatus()).isEqualTo("rejected");
        assertThat(summary.getRejectionReason()).isEqualTo("다시 정리해볼까?");
        verify(contentLikeRepository, never()).delete(any());
    }

    @Test
    void reviewSummary_rejectedToApproved_clearsRejectionReason() {
        stubTeacherClassSetup();
        stubStudentClassLookup(STUDENT_ID, CLASS_ID);
        Summary summary = buildSummary(STUDENT_ID, "rejected");
        summary.setRejectionReason("이전 거절 사유");
        when(summaryRepository.findById(SUMMARY_ID)).thenReturn(Optional.of(summary));
        when(classReadingBookRepository.findById(CLASS_READING_BOOK_ID))
            .thenReturn(Optional.of(buildClassReadingBook()));
        when(summaryRepository.save(any(Summary.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.reviewSummary(TEACHER_ID, CLASS_ID, SUMMARY_ID, "approved", null);

        assertThat(result.getStatus()).isEqualTo("approved");
        assertThat(summary.getRejectionReason()).isNull();
    }

    @Test
    void reviewSummary_rejectsInvalidStatus() {
        stubTeacherClassSetup();
        stubStudentClassLookup(STUDENT_ID, CLASS_ID);
        Summary summary = buildSummary(STUDENT_ID, "pending");
        when(summaryRepository.findById(SUMMARY_ID)).thenReturn(Optional.of(summary));
        when(classReadingBookRepository.findById(CLASS_READING_BOOK_ID))
            .thenReturn(Optional.of(buildClassReadingBook()));

        assertThatThrownBy(() -> service.reviewSummary(TEACHER_ID, CLASS_ID, SUMMARY_ID, "archived", null))
            .isInstanceOf(ResponseStatusException.class);
        verify(summaryRepository, never()).save(any(Summary.class));
    }

    @Test
    void reviewSummary_rejectsSummaryOutsideTeacherClass() {
        stubTeacherClassSetup();
        Summary summary = buildSummary(STUDENT_ID, "pending");
        when(summaryRepository.findById(SUMMARY_ID)).thenReturn(Optional.of(summary));
        // 다른 학급 소속 온책읽기 책 - validateClassReadingBookBelongsToClass가 403을 던져야 한다.
        when(classReadingBookRepository.findById(CLASS_READING_BOOK_ID))
            .thenReturn(Optional.of(buildClassReadingBook(OTHER_CLASS_ID)));

        assertThatThrownBy(() -> service.reviewSummary(TEACHER_ID, CLASS_ID, SUMMARY_ID, "approved", null))
            .isInstanceOf(ResponseStatusException.class);
        verify(summaryRepository, never()).save(any(Summary.class));
    }

    private ClassReadingBook buildClassReadingBook() {
        return buildClassReadingBook(CLASS_ID);
    }

    private ClassReadingBook buildClassReadingBook(Long classId) {
        ClassReadingBook readingBook = new ClassReadingBook();
        readingBook.setId(CLASS_READING_BOOK_ID);
        readingBook.setSchoolClass(buildSchoolClass(classId));
        return readingBook;
    }

    @Test
    void resubmitSummary_onlyOwnRejectedSummary_resetsToPendingWithNewText() {
        stubStudentClassLookup(STUDENT_ID, CLASS_ID);
        Summary summary = buildSummary(STUDENT_ID, "rejected");
        summary.setRejectionReason("다시 읽고 고쳐보세요.");
        when(summaryRepository.findById(SUMMARY_ID)).thenReturn(Optional.of(summary));
        when(summaryRepository.save(any(Summary.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.resubmitSummary(STUDENT_ID, SUMMARY_ID, "고친 간추리기 내용입니다.");

        assertThat(result.getStatus()).isEqualTo("pending");
        assertThat(summary.getSummaryText()).isEqualTo("고친 간추리기 내용입니다.");
        assertThat(summary.getRejectionReason()).isNull();
    }

    @Test
    void resubmitSummary_rejectsWhenSummaryBelongsToAnotherStudent() {
        Summary summary = buildSummary(OTHER_STUDENT_ID, "rejected");
        when(summaryRepository.findById(SUMMARY_ID)).thenReturn(Optional.of(summary));

        assertThatThrownBy(() -> service.resubmitSummary(STUDENT_ID, SUMMARY_ID, "고친 내용"))
            .isInstanceOf(ResponseStatusException.class);
        verify(summaryRepository, never()).save(any(Summary.class));
    }

    @Test
    void resubmitSummary_rejectsWhenSummaryIsNotRejected() {
        Summary summary = buildSummary(STUDENT_ID, "pending");
        when(summaryRepository.findById(SUMMARY_ID)).thenReturn(Optional.of(summary));

        assertThatThrownBy(() -> service.resubmitSummary(STUDENT_ID, SUMMARY_ID, "고친 내용"))
            .isInstanceOf(ResponseStatusException.class);
        verify(summaryRepository, never()).save(any(Summary.class));
    }

    @Test
    void deleteRejectedSummary_onlyDeletesOwnRejectedSummary() {
        Summary summary = buildSummary(STUDENT_ID, "rejected");
        when(summaryRepository.findById(SUMMARY_ID)).thenReturn(Optional.of(summary));

        service.deleteRejectedSummary(STUDENT_ID, SUMMARY_ID);

        verify(summaryRepository).delete(summary);
    }

    @Test
    void deleteRejectedSummary_rejectsWhenSummaryIsApproved() {
        Summary summary = buildSummary(STUDENT_ID, "approved");
        when(summaryRepository.findById(SUMMARY_ID)).thenReturn(Optional.of(summary));

        assertThatThrownBy(() -> service.deleteRejectedSummary(STUDENT_ID, SUMMARY_ID))
            .isInstanceOf(ResponseStatusException.class);
        verify(summaryRepository, never()).delete(any(Summary.class));
    }

    @Test
    void getSharedSummaries_showsOwnPendingButHidesOthersPendingFromClassmate() {
        // 조회자(STUDENT_ID)의 pending 글은 본인에게 보이고, 다른 학생(OTHER_STUDENT_ID)의
        // pending 글은 승인되기 전까지 급우에게 보이면 안 된다.
        stubStudentClassLookup(STUDENT_ID, CLASS_ID);
        when(classReadingBookRepository.findById(CLASS_READING_BOOK_ID))
            .thenReturn(Optional.of(buildClassReadingBook()));
        when(classReadingBookRepository.findById(CLASS_READING_BOOK_ID))
            .thenReturn(Optional.of(buildClassReadingBook()));

        Summary mine = buildSummary(STUDENT_ID, "pending");
        Summary othersPending = buildSummary(OTHER_STUDENT_ID, "pending");
        othersPending.setId(SUMMARY_ID + 1);
        Summary othersApproved = buildSummary(OTHER_STUDENT_ID, "approved");
        othersApproved.setId(SUMMARY_ID + 2);

        when(summaryRepository.findByClassReadingBookIdAndIsSharedTrueOrderByUpdatedAtDesc(CLASS_READING_BOOK_ID))
            .thenReturn(List.of(mine, othersPending, othersApproved));

        ClassStudent mineClassStudent = new ClassStudent();
        mineClassStudent.setStudent(mine.getStudent());
        mineClassStudent.setSchoolClass(buildSchoolClass(CLASS_ID));
        ClassStudent othersClassStudent = new ClassStudent();
        othersClassStudent.setStudent(othersPending.getStudent());
        othersClassStudent.setSchoolClass(buildSchoolClass(CLASS_ID));
        when(classStudentRepository.findBySchoolClassId(CLASS_ID))
            .thenReturn(List.of(mineClassStudent, othersClassStudent));

        var result = service.getSharedSummaries(STUDENT_ID, CLASS_READING_BOOK_ID);

        assertThat(result).extracting(item -> item.getId())
            .containsExactlyInAnyOrder(mine.getId(), othersApproved.getId());
    }
}
