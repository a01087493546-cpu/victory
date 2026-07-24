package com.victory.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import com.victory.dto.PracticeAchievementResponse;
import com.victory.entity.AiEvaluationAttempt;
import com.victory.entity.ClassReadingBook;
import com.victory.entity.ClassStudent;
import com.victory.entity.PracticeAchievementSnapshot;
import com.victory.entity.PracticeProgress;
import com.victory.entity.Response;
import com.victory.entity.SchoolClass;
import com.victory.entity.User;
import com.victory.repository.AiEvaluationAttemptRepository;
import com.victory.repository.ClassReadingBookRepository;
import com.victory.repository.ClassStudentRepository;
import com.victory.repository.PracticeAchievementSnapshotRepository;
import com.victory.repository.PracticeProgressRepository;
import com.victory.repository.ResponseRepository;
import com.victory.repository.SchoolClassRepository;
import com.victory.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class PracticeAchievementServiceTest {

    private static final Long TEACHER_ID = 1L;
    private static final Long CLASS_ID = 10L;
    private static final Long OTHER_CLASS_ID = 99L;
    private static final Long BOOK_ID = 100L;
    private static final Long STUDENT_ID = 20L;

    @Mock private SchoolClassRepository schoolClassRepository;
    @Mock private ClassStudentRepository classStudentRepository;
    @Mock private ClassReadingBookRepository classReadingBookRepository;
    @Mock private PracticeProgressRepository practiceProgressRepository;
    @Mock private ResponseRepository responseRepository;
    @Mock private UserRepository userRepository;
    @Mock private AiEvaluationAttemptRepository aiEvaluationAttemptRepository;
    @Mock private PracticeAchievementSnapshotRepository snapshotRepository;

    private final PracticeAchievementCalculator calculator = new PracticeAchievementCalculator();

    private PracticeAchievementService service;

    private User teacher;
    private SchoolClass schoolClass;
    private ClassReadingBook classReadingBook;

    @BeforeEach
    void setUp() {
        service = new PracticeAchievementService(
            schoolClassRepository,
            classStudentRepository,
            classReadingBookRepository,
            practiceProgressRepository,
            responseRepository,
            userRepository,
            aiEvaluationAttemptRepository,
            snapshotRepository,
            calculator
        );

        teacher = new User();
        teacher.setId(TEACHER_ID);
        teacher.setRole("teacher");
        teacher.setName("teacher");

        schoolClass = new SchoolClass();
        schoolClass.setId(CLASS_ID);
        schoolClass.setTeacher(teacher);

        classReadingBook = new ClassReadingBook();
        classReadingBook.setId(BOOK_ID);
        classReadingBook.setSchoolClass(schoolClass);
        classReadingBook.setTotalPages(100);
        classReadingBook.setCurrentPage(50);

        // lenient: 스냅샷 저장 테스트처럼 교사/학급 조회를 아예 거치지 않는
        // 테스트도 있어서, 여기 공통 setUp에서는 엄격한 stub 미사용 검사를 끈다.
        lenient().when(userRepository.findById(TEACHER_ID)).thenReturn(Optional.of(teacher));
        lenient().when(schoolClassRepository.findByTeacherId(TEACHER_ID)).thenReturn(Optional.of(schoolClass));
        lenient().when(classReadingBookRepository.findBySchoolClassId(CLASS_ID))
            .thenReturn(Optional.of(classReadingBook));
    }

    private ClassStudent buildClassStudent(Long studentId, Integer studentNumber, String name) {
        User student = new User();
        student.setId(studentId);
        student.setName(name);
        student.setRole("student");

        ClassStudent classStudent = new ClassStudent();
        classStudent.setSchoolClass(schoolClass);
        classStudent.setStudent(student);
        classStudent.setStudentNumber(studentNumber);

        return classStudent;
    }

    private Response buildResponse(Long studentId, String activityType, LocalDateTime createdAt) {
        Response response = new Response();
        response.setContent("답");
        response.setCreatedAt(createdAt);
        response.setUpdatedAt(createdAt);
        response.setExtraData(activityType == null ? Map.of() : Map.of("activityType", activityType));
        return response;
    }

    /* 검증 13: 다른 교사의 학급 접근 시 403 */
    @Test
    void getClassAchievement_throwsForbiddenWhenClassBelongsToAnotherTeacher() {
        assertThatThrownBy(() ->
            service.getClassAchievement(TEACHER_ID, OTHER_CLASS_ID, null)
        )
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("403");
    }

    /* 검증 15: 오늘 질문 여러 개 작성한 학생을 오늘 참여 인원 한 명으로 계산 */
    @Test
    void getClassAchievement_countsStudentOnceEvenWithMultipleTodayResponses() {
        ClassStudent classStudent = buildClassStudent(STUDENT_ID, 1, "학생1");
        when(classStudentRepository.findBySchoolClassId(CLASS_ID)).thenReturn(List.of(classStudent));

        when(practiceProgressRepository.findByStudent_Id(STUDENT_ID)).thenReturn(Optional.empty());

        LocalDateTime now = LocalDateTime.now();

        // 오늘 책 속 생각쓰기(book_thought) 글 2개 - 같은 학생, 같은 날
        when(responseRepository.findByStudent_IdAndModeAndContentTypeAndStageAndDeletedAtIsNullOrderByIdAsc(
                eq(STUDENT_ID), eq("class"), eq("answer"), eq("before")))
            .thenReturn(List.of());
        when(responseRepository.findByStudent_IdAndModeAndContentTypeAndStageAndDeletedAtIsNullOrderByIdAsc(
                eq(STUDENT_ID), eq("class"), eq("answer"), eq("during")))
            .thenReturn(List.of(
                buildResponse(STUDENT_ID, "book_thought", now),
                buildResponse(STUDENT_ID, "book_thought", now)
            ));
        when(responseRepository.findByStudent_IdAndModeAndContentTypeAndStageAndDeletedAtIsNullOrderByIdAsc(
                eq(STUDENT_ID), eq("class"), eq("answer"), eq("after")))
            .thenReturn(List.of());

        when(aiEvaluationAttemptRepository.findByStudentIdInAndClassReadingBookIdOrderByEvaluatedAtAsc(anyList(), eq(BOOK_ID)))
            .thenReturn(List.of());

        PracticeAchievementResponse response = service.getClassAchievement(TEACHER_ID, CLASS_ID, null);

        assertThat(response.getTotalStudentCount()).isEqualTo(1);
        assertThat(response.getTodayParticipatingStudentCount()).isEqualTo(1);
        assertThat(response.getTodayParticipationRate()).isEqualTo(100.0);
    }

    /* 검증 10: 동일 질문의 재시도가 분모에 여러 번 포함되지 않음 */
    @Test
    void getClassAchievement_deduplicatesRetriesOfSameQuestion() {
        ClassStudent classStudent = buildClassStudent(STUDENT_ID, 1, "학생1");
        when(classStudentRepository.findBySchoolClassId(CLASS_ID)).thenReturn(List.of(classStudent));
        when(practiceProgressRepository.findByStudent_Id(STUDENT_ID)).thenReturn(Optional.empty());

        stubEmptyResponses(STUDENT_ID);

        // 같은 질문(evaluationKey가 같음)에 need -> need -> good 3번 시도
        AiEvaluationAttempt attempt1 = new AiEvaluationAttempt();
        attempt1.setStudentId(STUDENT_ID);
        attempt1.setActivityType("during_reading_practice_deep");
        attempt1.setQuestionType("direct");
        attempt1.setEvaluationKey("q-1");
        attempt1.setAttemptNumber(1);
        attempt1.setStatus("need");

        AiEvaluationAttempt attempt2 = new AiEvaluationAttempt();
        attempt2.setStudentId(STUDENT_ID);
        attempt2.setActivityType("during_reading_practice_deep");
        attempt2.setQuestionType("direct");
        attempt2.setEvaluationKey("q-1");
        attempt2.setAttemptNumber(2);
        attempt2.setStatus("need");

        AiEvaluationAttempt attempt3 = new AiEvaluationAttempt();
        attempt3.setStudentId(STUDENT_ID);
        attempt3.setActivityType("during_reading_practice_deep");
        attempt3.setQuestionType("direct");
        attempt3.setEvaluationKey("q-1");
        attempt3.setAttemptNumber(3);
        attempt3.setStatus("good");

        when(aiEvaluationAttemptRepository.findByStudentIdInAndClassReadingBookIdOrderByEvaluatedAtAsc(anyList(), eq(BOOK_ID)))
            .thenReturn(List.of(attempt1, attempt2, attempt3));

        PracticeAchievementResponse response = service.getClassAchievement(TEACHER_ID, CLASS_ID, null);

        assertThat(response.getStudents()).hasSize(1);
        assertThat(response.getStudents().get(0).getAiEvaluatedQuestionCount()).isEqualTo(1);
        assertThat(response.getStudents().get(0).getPassedWithinThreeAttemptsCount()).isEqualTo(1);
        assertThat(response.getStudents().get(0).getHasAiEvaluation()).isTrue();
    }

    /* 검증 9: AI 정상 평가 0건이면 hasAiEvaluation=false */
    @Test
    void getClassAchievement_noAiEvaluationWhenNoAttempts() {
        ClassStudent classStudent = buildClassStudent(STUDENT_ID, 1, "학생1");
        when(classStudentRepository.findBySchoolClassId(CLASS_ID)).thenReturn(List.of(classStudent));
        when(practiceProgressRepository.findByStudent_Id(STUDENT_ID)).thenReturn(Optional.empty());
        stubEmptyResponses(STUDENT_ID);
        when(aiEvaluationAttemptRepository.findByStudentIdInAndClassReadingBookIdOrderByEvaluatedAtAsc(anyList(), eq(BOOK_ID)))
            .thenReturn(List.of());

        PracticeAchievementResponse response = service.getClassAchievement(TEACHER_ID, CLASS_ID, null);

        assertThat(response.getStudents().get(0).getHasAiEvaluation()).isFalse();
        assertThat(response.getStudents().get(0).getComprehensionRate()).isEqualTo(0.0);
    }

    /* 검증(evaluationKey) 1: 같은 evaluationKey로 bad -> good이면 질문 1개, 2회차 성공 */
    @Test
    void getClassAchievement_sameEvaluationKeySecondAttemptGoodCountsAsSuccess() {
        ClassStudent classStudent = buildClassStudent(STUDENT_ID, 1, "학생1");
        when(classStudentRepository.findBySchoolClassId(CLASS_ID)).thenReturn(List.of(classStudent));
        when(practiceProgressRepository.findByStudent_Id(STUDENT_ID)).thenReturn(Optional.empty());
        stubEmptyResponses(STUDENT_ID);

        when(aiEvaluationAttemptRepository.findByStudentIdInAndClassReadingBookIdOrderByEvaluatedAtAsc(anyList(), eq(BOOK_ID)))
            .thenReturn(List.of(
                buildAttempt("q-A", 1, "need"),
                buildAttempt("q-A", 2, "good")
            ));

        PracticeAchievementResponse response = service.getClassAchievement(TEACHER_ID, CLASS_ID, null);

        assertThat(response.getStudents().get(0).getAiEvaluatedQuestionCount()).isEqualTo(1);
        assertThat(response.getStudents().get(0).getPassedWithinThreeAttemptsCount()).isEqualTo(1);
    }

    /* 검증(evaluationKey) 3: 같은 evaluationKey 4회차 good이면 실패 */
    @Test
    void getClassAchievement_sameEvaluationKeyFourthAttemptGoodDoesNotCountAsSuccess() {
        ClassStudent classStudent = buildClassStudent(STUDENT_ID, 1, "학생1");
        when(classStudentRepository.findBySchoolClassId(CLASS_ID)).thenReturn(List.of(classStudent));
        when(practiceProgressRepository.findByStudent_Id(STUDENT_ID)).thenReturn(Optional.empty());
        stubEmptyResponses(STUDENT_ID);

        when(aiEvaluationAttemptRepository.findByStudentIdInAndClassReadingBookIdOrderByEvaluatedAtAsc(anyList(), eq(BOOK_ID)))
            .thenReturn(List.of(
                buildAttempt("q-B", 1, "need"),
                buildAttempt("q-B", 2, "need"),
                buildAttempt("q-B", 3, "need"),
                buildAttempt("q-B", 4, "good")
            ));

        PracticeAchievementResponse response = service.getClassAchievement(TEACHER_ID, CLASS_ID, null);

        assertThat(response.getStudents().get(0).getAiEvaluatedQuestionCount()).isEqualTo(1);
        assertThat(response.getStudents().get(0).getPassedWithinThreeAttemptsCount()).isEqualTo(0);
        assertThat(response.getStudents().get(0).getHasAiEvaluation()).isTrue();
    }

    /* 검증(evaluationKey) 4: 서로 다른 evaluationKey는 서로 다른 질문으로 계산 */
    @Test
    void getClassAchievement_differentEvaluationKeysCountAsDifferentQuestions() {
        ClassStudent classStudent = buildClassStudent(STUDENT_ID, 1, "학생1");
        when(classStudentRepository.findBySchoolClassId(CLASS_ID)).thenReturn(List.of(classStudent));
        when(practiceProgressRepository.findByStudent_Id(STUDENT_ID)).thenReturn(Optional.empty());
        stubEmptyResponses(STUDENT_ID);

        when(aiEvaluationAttemptRepository.findByStudentIdInAndClassReadingBookIdOrderByEvaluatedAtAsc(anyList(), eq(BOOK_ID)))
            .thenReturn(List.of(
                buildAttempt("q-A", 1, "good"),
                buildAttempt("q-B", 1, "need"),
                buildAttempt("q-B", 2, "good"),
                buildAttempt("q-C", 1, "need"),
                buildAttempt("q-C", 2, "need"),
                buildAttempt("q-C", 3, "need"),
                buildAttempt("q-C", 4, "good")
            ));

        PracticeAchievementResponse response = service.getClassAchievement(TEACHER_ID, CLASS_ID, null);

        // 질문 3개(q-A,q-B,q-C), 3회 이내 성공은 q-A/q-B 2개, q-C는 4회차라 실패
        assertThat(response.getStudents().get(0).getAiEvaluatedQuestionCount()).isEqualTo(3);
        assertThat(response.getStudents().get(0).getPassedWithinThreeAttemptsCount()).isEqualTo(2);
        assertThat(response.getStudents().get(0).getComprehensionRate())
            .isCloseTo(66.67, org.assertj.core.data.Offset.offset(0.01));
    }

    /*
     * 읽기 후 book_question/final_summary가 이해도 계산에서 누락되지
     * 않는지 확인 - PracticeAchievementService는 activityType 허용
     * 목록으로 거르지 않고 evaluationKey 기준으로만 계산하므로, 읽기 후
     * 유형도 다른 유형과 동일하게 포함되어야 한다.
     */
    @Test
    void getClassAchievement_includesAfterReadingBookQuestionAndFinalSummary() {
        ClassStudent classStudent = buildClassStudent(STUDENT_ID, 1, "학생1");
        when(classStudentRepository.findBySchoolClassId(CLASS_ID)).thenReturn(List.of(classStudent));
        when(practiceProgressRepository.findByStudent_Id(STUDENT_ID)).thenReturn(Optional.empty());
        stubEmptyResponses(STUDENT_ID);

        AiEvaluationAttempt bookQuestion1 = new AiEvaluationAttempt();
        bookQuestion1.setStudentId(STUDENT_ID);
        bookQuestion1.setActivityType("book_question");
        bookQuestion1.setQuestionType("1");
        bookQuestion1.setEvaluationKey("practice-after-100-20-book-question-1");
        bookQuestion1.setAttemptNumber(1);
        bookQuestion1.setStatus("good");

        AiEvaluationAttempt finalSummary = new AiEvaluationAttempt();
        finalSummary.setStudentId(STUDENT_ID);
        finalSummary.setActivityType("final_summary");
        finalSummary.setEvaluationKey("practice-after-100-20-final-summary");
        finalSummary.setAttemptNumber(1);
        finalSummary.setStatus("need");

        AiEvaluationAttempt finalSummaryRetry = new AiEvaluationAttempt();
        finalSummaryRetry.setStudentId(STUDENT_ID);
        finalSummaryRetry.setActivityType("final_summary");
        finalSummaryRetry.setEvaluationKey("practice-after-100-20-final-summary");
        finalSummaryRetry.setAttemptNumber(2);
        finalSummaryRetry.setStatus("good");

        when(aiEvaluationAttemptRepository.findByStudentIdInAndClassReadingBookIdOrderByEvaluatedAtAsc(anyList(), eq(BOOK_ID)))
            .thenReturn(List.of(bookQuestion1, finalSummary, finalSummaryRetry));

        PracticeAchievementResponse response = service.getClassAchievement(TEACHER_ID, CLASS_ID, null);

        // 질문 2개(book-question-1, final-summary), 둘 다 3회 이내 성공
        assertThat(response.getStudents().get(0).getAiEvaluatedQuestionCount()).isEqualTo(2);
        assertThat(response.getStudents().get(0).getPassedWithinThreeAttemptsCount()).isEqualTo(2);
        assertThat(response.getStudents().get(0).getComprehensionRate()).isEqualTo(100.0);
    }

    /*
     * 책별 데이터 분리 검증: 같은 학생이라도 다른 온책읽기 책(예: 다른 학급/
     * 다른 해)의 평가 기록은 현재 책의 이해도 계산에 절대 섞이면 안 된다.
     * 현재 학급 책(BOOK_ID)에는 실제로 조회되지 않을 다른 책(OTHER_BOOK_ID)에
     * 성공 기록을 잔뜩 스텁해 두고, 결과가 BOOK_ID로 스텁한 값과만 일치하는지
     * 확인한다.
     */
    @Test
    void getClassAchievement_doesNotMixAttemptsFromDifferentClassReadingBook() {
        Long otherBookId = 777L;

        ClassStudent classStudent = buildClassStudent(STUDENT_ID, 1, "학생1");
        when(classStudentRepository.findBySchoolClassId(CLASS_ID)).thenReturn(List.of(classStudent));
        when(practiceProgressRepository.findByStudent_Id(STUDENT_ID)).thenReturn(Optional.empty());
        stubEmptyResponses(STUDENT_ID);

        when(aiEvaluationAttemptRepository.findByStudentIdInAndClassReadingBookIdOrderByEvaluatedAtAsc(anyList(), eq(BOOK_ID)))
            .thenReturn(List.of(buildAttempt("q-A", 1, "good")));

        // 다른 책(otherBookId)에는 서로 다른 질문 5개를 성공시켜, 실수로
        // studentId만으로 조회하면 이 값들까지 섞여 결과가 달라짐을 검증한다.
        // (서비스가 정상 동작하면 이 스텁은 애초에 호출되지 않아야 하므로 lenient)
        lenient().when(aiEvaluationAttemptRepository.findByStudentIdInAndClassReadingBookIdOrderByEvaluatedAtAsc(anyList(), eq(otherBookId)))
            .thenReturn(List.of(
                buildAttempt("other-1", 1, "good"),
                buildAttempt("other-2", 1, "good"),
                buildAttempt("other-3", 1, "good"),
                buildAttempt("other-4", 1, "good"),
                buildAttempt("other-5", 1, "good")
            ));

        PracticeAchievementResponse response = service.getClassAchievement(TEACHER_ID, CLASS_ID, null);

        assertThat(response.getStudents().get(0).getAiEvaluatedQuestionCount()).isEqualTo(1);
        assertThat(response.getStudents().get(0).getPassedWithinThreeAttemptsCount()).isEqualTo(1);

        verify(aiEvaluationAttemptRepository)
            .findByStudentIdInAndClassReadingBookIdOrderByEvaluatedAtAsc(anyList(), eq(BOOK_ID));
        verify(aiEvaluationAttemptRepository, never())
            .findByStudentIdInAndClassReadingBookIdOrderByEvaluatedAtAsc(anyList(), eq(otherBookId));
    }

    /*
     * evaluationKey가 없는(과거 방식) 레코드는 질문 단위로 묶을 수 없으므로
     * 이해도 계산에서 완전히 제외되어야 한다.
     */
    @Test
    void getClassAchievement_excludesAttemptsWithoutEvaluationKey() {
        ClassStudent classStudent = buildClassStudent(STUDENT_ID, 1, "학생1");
        when(classStudentRepository.findBySchoolClassId(CLASS_ID)).thenReturn(List.of(classStudent));
        when(practiceProgressRepository.findByStudent_Id(STUDENT_ID)).thenReturn(Optional.empty());
        stubEmptyResponses(STUDENT_ID);

        AiEvaluationAttempt legacyAttempt = new AiEvaluationAttempt();
        legacyAttempt.setStudentId(STUDENT_ID);
        legacyAttempt.setActivityType("during_reading_question");
        legacyAttempt.setQuestionType("direct");
        legacyAttempt.setStatus("good");
        // evaluationKey/attemptNumber 없음(과거 방식 레코드 시뮬레이션)

        when(aiEvaluationAttemptRepository.findByStudentIdInAndClassReadingBookIdOrderByEvaluatedAtAsc(anyList(), eq(BOOK_ID)))
            .thenReturn(List.of(legacyAttempt));

        PracticeAchievementResponse response = service.getClassAchievement(TEACHER_ID, CLASS_ID, null);

        assertThat(response.getStudents().get(0).getAiEvaluatedQuestionCount()).isEqualTo(0);
        assertThat(response.getStudents().get(0).getHasAiEvaluation()).isFalse();
    }

    /* 학생 개인 classReadDone=false이면 읽기 후 미제출 사유 없음 */
    @Test
    void getClassAchievement_doesNotFlagAfterQuestionWhenClassReadNotDoneYet() {
        ClassStudent classStudent = buildClassStudent(STUDENT_ID, 1, "학생1");
        when(classStudentRepository.findBySchoolClassId(CLASS_ID)).thenReturn(List.of(classStudent));

        PracticeProgress progress = new PracticeProgress();
        progress.setBeforeDone(true);
        progress.setClassReadDone(false);
        when(practiceProgressRepository.findByStudent_Id(STUDENT_ID)).thenReturn(Optional.of(progress));

        // 읽기 전/책수다방은 작성 완료, 읽기 후는 아직 단계가 안 열림
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        when(responseRepository.findByStudent_IdAndModeAndContentTypeAndStageAndDeletedAtIsNullOrderByIdAsc(
                eq(STUDENT_ID), eq("class"), eq("answer"), eq("before")))
            .thenReturn(List.of(buildResponse(STUDENT_ID, null, now)));
        when(responseRepository.findByStudent_IdAndModeAndContentTypeAndStageAndDeletedAtIsNullOrderByIdAsc(
                eq(STUDENT_ID), eq("class"), eq("answer"), eq("during")))
            .thenReturn(List.of(buildResponse(STUDENT_ID, "book_thought", now)));
        when(responseRepository.findByStudent_IdAndModeAndContentTypeAndStageAndDeletedAtIsNullOrderByIdAsc(
                eq(STUDENT_ID), eq("class"), eq("answer"), eq("after")))
            .thenReturn(List.of());

        when(aiEvaluationAttemptRepository.findByStudentIdInAndClassReadingBookIdOrderByEvaluatedAtAsc(anyList(), eq(BOOK_ID)))
            .thenReturn(List.of());

        PracticeAchievementResponse response = service.getClassAchievement(TEACHER_ID, CLASS_ID, null);

        boolean hasNoQuestionSubmission = response.getStudents().get(0).getSupportReasons().stream()
            .anyMatch(r -> "NO_QUESTION_SUBMISSION".equals(r.getCode()));

        assertThat(hasNoQuestionSubmission).isFalse();
    }

    private AiEvaluationAttempt buildAttempt(String evaluationKey, int attemptNumber, String status) {
        AiEvaluationAttempt attempt = new AiEvaluationAttempt();
        attempt.setStudentId(STUDENT_ID);
        attempt.setActivityType("during_reading_question");
        attempt.setQuestionType("direct");
        attempt.setEvaluationKey(evaluationKey);
        attempt.setAttemptNumber(attemptNumber);
        attempt.setStatus(status);
        return attempt;
    }

    /* 검증 11: 지원 사유 여러 개여도 supportNeededCount는 학생 한 명 */
    @Test
    void getClassAchievement_countsStudentOnceEvenWithMultipleSupportReasons() {
        ClassStudent classStudent = buildClassStudent(STUDENT_ID, 1, "학생1");
        when(classStudentRepository.findBySchoolClassId(CLASS_ID)).thenReturn(List.of(classStudent));

        // beforeDone=true(읽기 중 단계 열림) 이지만 아무 질문도 안 씀 -> 참여도도 낮고, 질문 미제출도 해당
        PracticeProgress progress = new PracticeProgress();
        progress.setBeforeDone(true);
        progress.setClassReadDone(false);
        when(practiceProgressRepository.findByStudent_Id(STUDENT_ID)).thenReturn(Optional.of(progress));

        stubEmptyResponses(STUDENT_ID);
        when(aiEvaluationAttemptRepository.findByStudentIdInAndClassReadingBookIdOrderByEvaluatedAtAsc(anyList(), eq(BOOK_ID)))
            .thenReturn(List.of());

        // 전체쪽수 0으로 만들어 참여도를 확실히 30% 밑으로 (읽기전 질문도 안 써서 questionParticipationRate도 0)
        classReadingBook.setTotalPages(0);
        classReadingBook.setCurrentPage(0);

        PracticeAchievementResponse response = service.getClassAchievement(TEACHER_ID, CLASS_ID, null);

        assertThat(response.getSupportNeededCount()).isEqualTo(1);
        assertThat(response.getStudents().get(0).getSupportReasons().size()).isGreaterThanOrEqualTo(2);
        assertThat(response.getStudents().get(0).getNeedsSupport()).isTrue();
    }

    /* 검증 12: 아직 책수다방이 열리지 않았을 때 글 0개로 지원 판정하지 않음 */
    @Test
    void getClassAchievement_doesNotFlagThoughtSharingWhenStageNotOpenYet() {
        ClassStudent classStudent = buildClassStudent(STUDENT_ID, 1, "학생1");
        when(classStudentRepository.findBySchoolClassId(CLASS_ID)).thenReturn(List.of(classStudent));

        // beforeDone=false -> 읽기 중(책수다방) 단계가 아직 안 열림
        PracticeProgress progress = new PracticeProgress();
        progress.setBeforeDone(false);
        progress.setClassReadDone(false);
        when(practiceProgressRepository.findByStudent_Id(STUDENT_ID)).thenReturn(Optional.of(progress));

        stubEmptyResponses(STUDENT_ID);
        when(aiEvaluationAttemptRepository.findByStudentIdInAndClassReadingBookIdOrderByEvaluatedAtAsc(anyList(), eq(BOOK_ID)))
            .thenReturn(List.of());

        PracticeAchievementResponse response = service.getClassAchievement(TEACHER_ID, CLASS_ID, null);

        boolean hasThoughtSharingReason = response.getStudents().get(0).getSupportReasons().stream()
            .anyMatch(r -> "NO_THOUGHT_SHARING".equals(r.getCode()));

        assertThat(hasThoughtSharingReason).isFalse();
    }

    /* 검증 14: 다른 학급 학생 데이터가 섞이지 않음 - 이 학급 roster만 조회함을 확인 */
    @Test
    void getClassAchievement_onlyQueriesRosterForRequestedClass() {
        when(classStudentRepository.findBySchoolClassId(CLASS_ID)).thenReturn(List.of());

        service.getClassAchievement(TEACHER_ID, CLASS_ID, null);

        verify(classStudentRepository, times(1)).findBySchoolClassId(CLASS_ID);
        verify(classStudentRepository, never()).findBySchoolClassId(OTHER_CLASS_ID);
    }

    /* 검증 16: 스냅샷 중복 실행 시 날짜별 레코드가 한 개만 존재 */
    @Test
    void saveSnapshotIfAbsent_doesNotInsertTwiceForSameDate() {
        LocalDate date = LocalDate.of(2026, 7, 21);

        when(snapshotRepository.findByStudentIdAndClassReadingBookIdAndSnapshotDate(STUDENT_ID, BOOK_ID, date))
            .thenReturn(Optional.empty())
            .thenReturn(Optional.of(mock(PracticeAchievementSnapshot.class)));

        when(practiceProgressRepository.findByStudent_Id(STUDENT_ID)).thenReturn(Optional.empty());
        stubEmptyResponses(STUDENT_ID);
        when(aiEvaluationAttemptRepository.findByStudentIdInAndClassReadingBookIdOrderByEvaluatedAtAsc(anyList(), eq(BOOK_ID)))
            .thenReturn(List.of());

        service.saveSnapshotIfAbsent(STUDENT_ID, BOOK_ID, classReadingBook, date);
        service.saveSnapshotIfAbsent(STUDENT_ID, BOOK_ID, classReadingBook, date);

        verify(snapshotRepository, times(1)).save(any(PracticeAchievementSnapshot.class));
    }

    private void stubEmptyResponses(Long studentId) {
        when(responseRepository.findByStudent_IdAndModeAndContentTypeAndStageAndDeletedAtIsNullOrderByIdAsc(
                eq(studentId), eq("class"), eq("answer"), eq("before")))
            .thenReturn(List.of());
        when(responseRepository.findByStudent_IdAndModeAndContentTypeAndStageAndDeletedAtIsNullOrderByIdAsc(
                eq(studentId), eq("class"), eq("answer"), eq("during")))
            .thenReturn(List.of());
        when(responseRepository.findByStudent_IdAndModeAndContentTypeAndStageAndDeletedAtIsNullOrderByIdAsc(
                eq(studentId), eq("class"), eq("answer"), eq("after")))
            .thenReturn(List.of());
    }
}
