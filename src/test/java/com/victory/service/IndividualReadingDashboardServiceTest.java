package com.victory.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import com.victory.dto.IndividualAchievementLevel;
import com.victory.dto.IndividualAchievementResult;
import com.victory.dto.TeacherIndividualReadingDashboardResponse;
import com.victory.dto.TeacherIndividualReadingStudentResponse;
import com.victory.entity.Book;
import com.victory.entity.ClassStudent;
import com.victory.entity.ReadingRecord;
import com.victory.entity.SchoolClass;
import com.victory.entity.User;
import com.victory.repository.ClassStudentRepository;
import com.victory.repository.ReadingRecordRepository;
import com.victory.repository.SchoolClassRepository;
import com.victory.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class IndividualReadingDashboardServiceTest {

    private static final Long TEACHER_ID = 66L;
    private static final Long CLASS_ID = 26L;

    private static final Long S01_ID = 67L;
    private static final Long S02_ID = 68L;
    private static final Long S03_ID = 69L;

    @Mock private UserRepository userRepository;
    @Mock private SchoolClassRepository schoolClassRepository;
    @Mock private ClassStudentRepository classStudentRepository;
    @Mock private ReadingRecordRepository readingRecordRepository;
    @Mock private IndividualAchievementService individualAchievementService;

    private IndividualReadingDashboardService service;

    private User teacher;
    private SchoolClass schoolClass;

    @BeforeEach
    void setUp() {
        service = new IndividualReadingDashboardService(
            userRepository, schoolClassRepository, classStudentRepository,
            readingRecordRepository, individualAchievementService);

        teacher = new User();
        teacher.setId(TEACHER_ID);
        teacher.setRole("teacher");

        schoolClass = new SchoolClass();
        schoolClass.setId(CLASS_ID);
        schoolClass.setClassName("6학년 1반");

        lenient().when(userRepository.findById(TEACHER_ID)).thenReturn(Optional.of(teacher));
        lenient().when(schoolClassRepository.findByTeacherId(TEACHER_ID)).thenReturn(Optional.of(schoolClass));
    }

    private User buildStudent(Long id, String name) {
        User student = new User();
        student.setId(id);
        student.setName(name);
        student.setRole("student");
        return student;
    }

    private ClassStudent buildMembership(User student, int number) {
        ClassStudent classStudent = new ClassStudent();
        classStudent.setSchoolClass(schoolClass);
        classStudent.setStudent(student);
        classStudent.setStudentNumber(number);
        return classStudent;
    }

    private ReadingRecord buildRecord(Long id, User student, LocalDateTime finishedAt, String title) {
        return buildRecord(id, student, finishedAt, title, false, false, false);
    }

    private ReadingRecord buildRecord(
            Long id, User student, LocalDateTime finishedAt, String title,
            boolean beforeDone, boolean duringDone, boolean afterDone) {
        ReadingRecord record = new ReadingRecord();
        record.setId(id);
        record.setStudent(student);
        record.setFinishedAt(finishedAt);
        record.setBeforeDone(beforeDone);
        record.setDuringDone(duringDone);
        record.setAfterDone(afterDone);
        Book book = new Book();
        book.setTitle(title);
        record.setBook(book);
        return record;
    }

    private IndividualAchievementResult result(
            Long studentId, Long readingRecordId, int readingDays, double readingPracticeScore,
            int completedStageCount, double recordCompletionScore, int inspectedItemCount,
            double contentSuitabilityScore, long totalCompletedBookCount,
            boolean wroteQuestionActivity, int bookChatPostCount, LocalDate latestActivityDate) {

        double overall = (readingPracticeScore + recordCompletionScore) / 2.0;
        int rounded = (int) Math.round(overall);

        return new IndividualAchievementResult(
            studentId, readingRecordId,
            readingDays, 0.0, 0, 0.0,
            readingPracticeScore,
            completedStageCount, 0.0,
            inspectedItemCount, 0,
            contentSuitabilityScore, recordCompletionScore,
            overall, rounded,
            IndividualAchievementLevel.fromRoundedScore(rounded),
            totalCompletedBookCount,
            wroteQuestionActivity, bookChatPostCount, latestActivityDate);
    }

    /* 검증 1/3: 교사 자신의 학급 학생 목록(s01/s02/s03) 모두 반환 */
    @Test
    void getDashboard_returnsAllRosterStudents() {
        User s01 = buildStudent(S01_ID, "이재환");
        User s02 = buildStudent(S02_ID, "김만수");
        User s03 = buildStudent(S03_ID, "김강민");

        when(classStudentRepository.findBySchoolClassId(CLASS_ID)).thenReturn(List.of(
            buildMembership(s01, 1), buildMembership(s02, 2), buildMembership(s03, 3)));

        // s01: 진행 중 기록 없음 -> 최근 완독 id 12 대표
        when(readingRecordRepository.findByStudent_IdAndFinishedAtIsNull(S01_ID)).thenReturn(Optional.empty());
        ReadingRecord s01Record = buildRecord(12L, s01, LocalDateTime.of(2026, 8, 2, 15, 32), "AI검사테스트책");
        when(readingRecordRepository.findFirstByStudent_IdAndFinishedAtIsNotNullOrderByFinishedAtDesc(S01_ID))
            .thenReturn(Optional.of(s01Record));
        when(individualAchievementService.calculate(12L)).thenReturn(
            result(S01_ID, 12L, 10, 43.0, 3, 100.0, 5, 100.0, 2, true, 2, LocalDate.of(2026, 7, 20)));

        // s02: 진행 중 기록 id 13 대표
        ReadingRecord s02Record = buildRecord(13L, s02, null, "진행중책");
        when(readingRecordRepository.findByStudent_IdAndFinishedAtIsNull(S02_ID))
            .thenReturn(Optional.of(s02Record));
        when(individualAchievementService.calculate(13L)).thenReturn(
            result(S02_ID, 13L, 5, 50.0, 1, 40.0, 2, 50.0, 1, true, 0, LocalDate.of(2026, 8, 2)));

        // s03: 기록 전혀 없음
        when(readingRecordRepository.findByStudent_IdAndFinishedAtIsNull(S03_ID)).thenReturn(Optional.empty());
        when(readingRecordRepository.findFirstByStudent_IdAndFinishedAtIsNotNullOrderByFinishedAtDesc(S03_ID))
            .thenReturn(Optional.empty());

        TeacherIndividualReadingDashboardResponse response = service.getDashboard(TEACHER_ID, CLASS_ID);

        assertThat(response.getStudents()).hasSize(3);
        assertThat(response.getStudents().stream().map(TeacherIndividualReadingStudentResponse::getStudentName))
            .containsExactly("이재환", "김만수", "김강민");
    }

    /* 검증 4/5/6: s01 - 완독 권수 2권, 대표 기록 id 12, 점수 43/100/71.5/72/우수 */
    @Test
    void getDashboard_s01_usesLatestFinishedRecordAsRepresentative() {
        User s01 = buildStudent(S01_ID, "이재환");
        when(classStudentRepository.findBySchoolClassId(CLASS_ID)).thenReturn(List.of(buildMembership(s01, 1)));
        when(readingRecordRepository.findByStudent_IdAndFinishedAtIsNull(S01_ID)).thenReturn(Optional.empty());

        ReadingRecord record12 = buildRecord(12L, s01, LocalDateTime.of(2026, 8, 2, 15, 32), "AI검사테스트책");
        when(readingRecordRepository.findFirstByStudent_IdAndFinishedAtIsNotNullOrderByFinishedAtDesc(S01_ID))
            .thenReturn(Optional.of(record12));
        when(individualAchievementService.calculate(12L)).thenReturn(
            result(S01_ID, 12L, 10, 43.0, 3, 100.0, 5, 100.0, 2, true, 2, LocalDate.of(2026, 7, 20)));

        TeacherIndividualReadingStudentResponse student = service.getDashboard(TEACHER_ID, CLASS_ID)
            .getStudents().get(0);

        assertThat(student.getTotalCompletedBookCount()).isEqualTo(2L);
        assertThat(student.getRepresentativeReadingRecordId()).isEqualTo(12L);
        assertThat(student.getCurrentBookTitle()).isEqualTo("AI검사테스트책");
        assertThat(student.getReadingPracticeScore()).isEqualTo(43.0);
        assertThat(student.getRecordCompletionScore()).isEqualTo(100.0);
        assertThat(student.getOverallAchievementScore()).isEqualTo(71.5);
        assertThat(student.getRoundedOverallAchievementScore()).isEqualTo(72);
        assertThat(student.getAchievementLevel()).isEqualTo("우수");
        // 대표 기록이 완독 기록이므로 "지금 읽고 있는 책"은 0권이어야 한다.
        assertThat(student.getActiveReadingBookCount()).isEqualTo(0);
    }

    /* 검증 7/8: s02 - 완독 권수 1권, 진행 중 id 13을 대표로 사용 */
    @Test
    void getDashboard_s02_usesInProgressRecordAsRepresentative() {
        User s02 = buildStudent(S02_ID, "김만수");
        when(classStudentRepository.findBySchoolClassId(CLASS_ID)).thenReturn(List.of(buildMembership(s02, 2)));

        ReadingRecord record13 = buildRecord(13L, s02, null, "진행중책");
        when(readingRecordRepository.findByStudent_IdAndFinishedAtIsNull(S02_ID))
            .thenReturn(Optional.of(record13));
        when(individualAchievementService.calculate(13L)).thenReturn(
            result(S02_ID, 13L, 5, 50.0, 1, 40.0, 2, 50.0, 1, true, 0, LocalDate.of(2026, 8, 2)));

        TeacherIndividualReadingStudentResponse student = service.getDashboard(TEACHER_ID, CLASS_ID)
            .getStudents().get(0);

        assertThat(student.getTotalCompletedBookCount()).isEqualTo(1L);
        assertThat(student.getRepresentativeReadingRecordId()).isEqualTo(13L);
        // 대표 기록이 진행 중(finished_at IS NULL)이므로 1권이어야 한다.
        assertThat(student.getActiveReadingBookCount()).isEqualTo(1);
        // 진행 중 기록을 대표로 썼으니, 완독 record를 조회하는 메서드는 아예 호출되지 않아야 한다.
    }

    /*
     * 기록이 있지만 종합달성도가 0~49점인 학생은 "미참여"가 아니라
     * 기존 등급 공식 그대로 "집중 지원"이어야 한다 - "미참여"는 대표
     * 기록이 아예 없는 학생에게만 붙는 별도 상태다.
     */
    @Test
    void getDashboard_lowScoreStudentWithRecord_staysNeedSupportNotNotParticipating() {
        User s02 = buildStudent(S02_ID, "김만수");
        when(classStudentRepository.findBySchoolClassId(CLASS_ID)).thenReturn(List.of(buildMembership(s02, 2)));

        ReadingRecord record13 = buildRecord(13L, s02, null, "진행중책");
        when(readingRecordRepository.findByStudent_IdAndFinishedAtIsNull(S02_ID))
            .thenReturn(Optional.of(record13));
        when(individualAchievementService.calculate(13L)).thenReturn(
            result(S02_ID, 13L, 1, 23.33, 1, 66.66, 4, 100.0, 1, true, 0, LocalDate.of(2026, 8, 2)));

        TeacherIndividualReadingStudentResponse student = service.getDashboard(TEACHER_ID, CLASS_ID)
            .getStudents().get(0);

        assertThat(student.getRoundedOverallAchievementScore()).isEqualTo(45);
        assertThat(student.getAchievementLevel()).isEqualTo("집중 지원");
    }

    /* 검증 9/10/11: s03 - 기록 없음. 목록 포함 + 모든 값 0/미참여/null */
    @Test
    void getDashboard_s03_noRecords_returnsZeroDefaultsButStaysInList() {
        User s03 = buildStudent(S03_ID, "김강민");
        when(classStudentRepository.findBySchoolClassId(CLASS_ID)).thenReturn(List.of(buildMembership(s03, 3)));
        when(readingRecordRepository.findByStudent_IdAndFinishedAtIsNull(S03_ID)).thenReturn(Optional.empty());
        when(readingRecordRepository.findFirstByStudent_IdAndFinishedAtIsNotNullOrderByFinishedAtDesc(S03_ID))
            .thenReturn(Optional.empty());

        TeacherIndividualReadingStudentResponse student = service.getDashboard(TEACHER_ID, CLASS_ID)
            .getStudents().get(0);

        assertThat(student.getRepresentativeReadingRecordId()).isNull();
        assertThat(student.getCurrentBookTitle()).isNull();
        assertThat(student.getActiveReadingBookCount()).isEqualTo(0);
        assertThat(student.getTotalCompletedBookCount()).isEqualTo(0L);
        assertThat(student.getReadingDays()).isEqualTo(0);
        assertThat(student.getReadingPracticeScore()).isEqualTo(0.0);
        assertThat(student.getCompletedStageCount()).isEqualTo(0);
        assertThat(student.getRecordCompletionScore()).isEqualTo(0.0);
        assertThat(student.getInspectedItemCount()).isEqualTo(0);
        assertThat(student.getContentSuitabilityScore()).isEqualTo(0.0);
        assertThat(student.getOverallAchievementScore()).isEqualTo(0.0);
        assertThat(student.getRoundedOverallAchievementScore()).isEqualTo(0);
        assertThat(student.getAchievementLevel()).isEqualTo("미참여");
        assertThat(student.getLatestActivityDate()).isNull();
        assertThat(student.isActiveToday()).isFalse();
        assertThat(student.getSupportReasons()).isEmpty();
    }

    /*
     * 검증 2/3/4/9/10: 오늘 참여율 분모는 "ReadingRecord가 1건이라도 있는
     * 학생"이다 - 진행 중 학생과 완독만 한 학생을 모두 포함해야 한다.
     * 실제 이재환(완독만 있음)·김만수(진행 중) 조합과 동일한 시나리오.
     */
    @Test
    void getDashboard_todayParticipation_countsBothInProgressAndCompletedOnlyStudents() {
        User inProgressStudent = buildStudent(S02_ID, "김만수");
        User completedOnlyStudent = buildStudent(S01_ID, "이재환");

        when(classStudentRepository.findBySchoolClassId(CLASS_ID)).thenReturn(List.of(
            buildMembership(completedOnlyStudent, 1), buildMembership(inProgressStudent, 2)));

        LocalDate today = LocalDate.now(java.time.ZoneId.of("Asia/Seoul"));

        // 이재환: 완독 기록만 있고 진행 중 기록은 없음 - 오늘도 활동(예: 추천 글) -> 분모/분자 모두 포함되어야 한다.
        when(readingRecordRepository.findByStudent_IdAndFinishedAtIsNull(S01_ID)).thenReturn(Optional.empty());
        ReadingRecord finishedRecord = buildRecord(12L, completedOnlyStudent,
            LocalDateTime.of(2026, 6, 1, 0, 0), "완독한 책");
        when(readingRecordRepository.findFirstByStudent_IdAndFinishedAtIsNotNullOrderByFinishedAtDesc(S01_ID))
            .thenReturn(Optional.of(finishedRecord));
        when(individualAchievementService.calculate(12L)).thenReturn(
            result(S01_ID, 12L, 20, 80.0, 3, 100.0, 3, 100.0, 1, true, 1, today));

        // 김만수: 진행 중 기록, 오늘 활동함
        ReadingRecord inProgressRecord = buildRecord(13L, inProgressStudent, null, "읽는 중");
        when(readingRecordRepository.findByStudent_IdAndFinishedAtIsNull(S02_ID))
            .thenReturn(Optional.of(inProgressRecord));
        when(individualAchievementService.calculate(13L)).thenReturn(
            result(S02_ID, 13L, 5, 50.0, 1, 50.0, 1, 100.0, 0, true, 1, today));

        TeacherIndividualReadingDashboardResponse response = service.getDashboard(TEACHER_ID, CLASS_ID);

        assertThat(response.getIndividualReadingStudentCount()).isEqualTo(2);
        assertThat(response.getTodayActiveStudentCount()).isEqualTo(2);
        assertThat(response.getTodayParticipationRate()).isEqualTo(100.0);
    }

    /*
     * 검증 7: 완독만 있고 오늘 활동이 없는 학생은 분모에는 포함되지만
     * 분자(오늘 참여)에서는 제외된다.
     */
    @Test
    void getDashboard_completedOnlyStudentInactiveToday_countsInDenominatorNotNumerator() {
        User completedOnlyStudent = buildStudent(S01_ID, "이재환");
        when(classStudentRepository.findBySchoolClassId(CLASS_ID))
            .thenReturn(List.of(buildMembership(completedOnlyStudent, 1)));

        when(readingRecordRepository.findByStudent_IdAndFinishedAtIsNull(S01_ID)).thenReturn(Optional.empty());
        ReadingRecord finishedRecord = buildRecord(12L, completedOnlyStudent,
            LocalDateTime.of(2026, 6, 1, 0, 0), "완독한 책");
        when(readingRecordRepository.findFirstByStudent_IdAndFinishedAtIsNotNullOrderByFinishedAtDesc(S01_ID))
            .thenReturn(Optional.of(finishedRecord));
        // 최근 활동일이 오늘이 아님(예: 예전에 완독한 뒤 오늘은 아무 활동도 안 함)
        when(individualAchievementService.calculate(12L)).thenReturn(
            result(S01_ID, 12L, 1, 43.0, 3, 100.0, 9, 100.0, 2, true, 2, LocalDate.of(2026, 6, 1)));

        TeacherIndividualReadingDashboardResponse response = service.getDashboard(TEACHER_ID, CLASS_ID);

        assertThat(response.getIndividualReadingStudentCount()).isEqualTo(1);
        assertThat(response.getTodayActiveStudentCount()).isEqualTo(0);
        assertThat(response.getTodayParticipationRate()).isEqualTo(0.0);
    }

    /* 검증 1/8: ReadingRecord가 전혀 없는 학생만 있으면 분모 0, 비율 대신 null + 안내 메시지 */
    @Test
    void getDashboard_noStudentsWithAnyRecord_ratioIsNullWithMessage() {
        User s03 = buildStudent(S03_ID, "김강민");
        when(classStudentRepository.findBySchoolClassId(CLASS_ID)).thenReturn(List.of(buildMembership(s03, 3)));
        when(readingRecordRepository.findByStudent_IdAndFinishedAtIsNull(S03_ID)).thenReturn(Optional.empty());
        when(readingRecordRepository.findFirstByStudent_IdAndFinishedAtIsNotNullOrderByFinishedAtDesc(S03_ID))
            .thenReturn(Optional.empty());

        TeacherIndividualReadingDashboardResponse response = service.getDashboard(TEACHER_ID, CLASS_ID);

        assertThat(response.getIndividualReadingStudentCount()).isEqualTo(0);
        assertThat(response.getTodayActiveStudentCount()).isEqualTo(0);
        assertThat(response.getTodayParticipationRate()).isNull();
        assertThat(response.getTodayParticipationMessage()).isEqualTo("개별읽기 참여 학생 없음");
    }

    /*
     * 검증 16/17: 여러 사유 동시 반환(순서 포함) + 판단 불가 사유는 제외.
     * 진행 중, 읽기 전·중은 끝냈지만 읽기 후는 아직인 학생 - 질문 활동은
     * 이미 썼으므로(duringDone=true) 질문 참여 부족은 제외되어야 하고,
     * inspectedItemCount=0이라 기록내용 적합성도 제외되어야 한다.
     */
    @Test
    void getDashboard_supportReasons_multipleAtOnceAndSkipsUnjudgeable() {
        User student = buildStudent(S01_ID, "이재환");
        when(classStudentRepository.findBySchoolClassId(CLASS_ID)).thenReturn(List.of(buildMembership(student, 1)));

        ReadingRecord record = buildRecord(1L, student, null, "책", true, true, false);
        when(readingRecordRepository.findByStudent_IdAndFinishedAtIsNull(S01_ID)).thenReturn(Optional.of(record));

        when(individualAchievementService.calculate(1L)).thenReturn(
            result(S01_ID, 1L, 5, 10.0, 2, 20.0, 0, 0.0, 0, true, 0, LocalDate.of(2026, 6, 1)));

        TeacherIndividualReadingStudentResponse res = service.getDashboard(TEACHER_ID, CLASS_ID)
            .getStudents().get(0);

        // 우선순위대로: 1)단계 미완료 2)독서실천도 부족 3)질문(제외) 4)생각 나누기 부족 5)적합성(제외)
        assertThat(res.getSupportReasons()).containsExactly(
            "읽기 후 활동을 완료해야 해요", "독서실천도가 낮아요", "생각 나누기 참여가 필요해요");
        assertThat(res.getSupportReasons()).doesNotContain(
            "질문 만들기 참여가 필요해요", "기록 내용을 조금 더 다듬어야 해요");
    }

    /* 실제 김만수 시나리오: 읽기 전 완료·읽기 중 미완료, 진행 중, readingPracticeScore=23.33 */
    @Test
    void getDashboard_kimManSooScenario_matchesExpectedSupportReasons() {
        User s02 = buildStudent(S02_ID, "김만수");
        when(classStudentRepository.findBySchoolClassId(CLASS_ID)).thenReturn(List.of(buildMembership(s02, 2)));

        ReadingRecord record13 = buildRecord(13L, s02, null, "루루", true, false, false);
        when(readingRecordRepository.findByStudent_IdAndFinishedAtIsNull(S02_ID))
            .thenReturn(Optional.of(record13));
        when(individualAchievementService.calculate(13L)).thenReturn(
            result(S02_ID, 13L, 1, 23.33, 1, 66.66, 4, 100.0, 1, true, 0, LocalDate.of(2026, 8, 2)));

        TeacherIndividualReadingDashboardResponse response = service.getDashboard(TEACHER_ID, CLASS_ID);
        TeacherIndividualReadingStudentResponse s02Response = response.getStudents().get(0);

        assertThat(s02Response.getSupportReasons()).containsExactly(
            "읽기 중 활동을 완료해야 해요", "독서실천도가 낮아요");
        assertThat(response.getSupportNeededStudentCount()).isEqualTo(1);
    }

    /* 완독 기록은 beforeDone/duringDone/afterDone이 어떻든 단계 미완료 사유를 절대 반환하지 않는다 */
    @Test
    void getDashboard_finishedRecord_neverReturnsStageIncompleteReason() {
        User student = buildStudent(S01_ID, "이재환");
        when(classStudentRepository.findBySchoolClassId(CLASS_ID)).thenReturn(List.of(buildMembership(student, 1)));
        when(readingRecordRepository.findByStudent_IdAndFinishedAtIsNull(S01_ID)).thenReturn(Optional.empty());

        ReadingRecord finished = buildRecord(
            12L, student, LocalDateTime.of(2026, 8, 2, 15, 32), "AI검사테스트책", true, true, true);
        when(readingRecordRepository.findFirstByStudent_IdAndFinishedAtIsNotNullOrderByFinishedAtDesc(S01_ID))
            .thenReturn(Optional.of(finished));
        when(individualAchievementService.calculate(12L)).thenReturn(
            result(S01_ID, 12L, 1, 43.0, 3, 100.0, 9, 100.0, 2, true, 2, LocalDate.of(2026, 7, 20)));

        TeacherIndividualReadingStudentResponse res = service.getDashboard(TEACHER_ID, CLASS_ID)
            .getStudents().get(0);

        assertThat(res.getSupportReasons()).doesNotContain(
            "읽기 중 활동을 완료해야 해요", "읽기 후 활동을 완료해야 해요");
    }

    /* 등록만 하고 아무 활동도 없는 초기 기록은 즉시 지원 대상으로 잡지 않는다 */
    @Test
    void getDashboard_freshRecordWithNoActivity_returnsNoSupportReasons() {
        User student = buildStudent(S01_ID, "이재환");
        when(classStudentRepository.findBySchoolClassId(CLASS_ID)).thenReturn(List.of(buildMembership(student, 1)));

        ReadingRecord record = buildRecord(1L, student, null, "새 책", false, false, false);
        when(readingRecordRepository.findByStudent_IdAndFinishedAtIsNull(S01_ID)).thenReturn(Optional.of(record));
        when(individualAchievementService.calculate(1L)).thenReturn(
            result(S01_ID, 1L, 0, 0.0, 0, 0.0, 0, 0.0, 0, false, 0, null));

        TeacherIndividualReadingStudentResponse res = service.getDashboard(TEACHER_ID, CLASS_ID)
            .getStudents().get(0);

        assertThat(res.getSupportReasons()).isEmpty();
    }

    /* 질문 활동 0 + 판정 가능(completedStageCount>=1) -> 질문 참여 부족만 발생하는 단독 시나리오 */
    @Test
    void getDashboard_zeroQuestionActivityButJudgeable_returnsQuestionReasonOnly() {
        User student = buildStudent(S01_ID, "이재환");
        when(classStudentRepository.findBySchoolClassId(CLASS_ID)).thenReturn(List.of(buildMembership(student, 1)));

        // completedStageCount>=1인데 wroteQuestionActivity=false인 경우를 그대로 재현(실제로는 드물지만 방어 로직 검증용)
        ReadingRecord record = buildRecord(1L, student, null, "책", true, true, true);
        when(readingRecordRepository.findByStudent_IdAndFinishedAtIsNull(S01_ID)).thenReturn(Optional.of(record));
        when(individualAchievementService.calculate(1L)).thenReturn(
            result(S01_ID, 1L, 5, 90.0, 3, 90.0, 0, 0.0, 0, false, 1, LocalDate.of(2026, 6, 1)));

        TeacherIndividualReadingStudentResponse res = service.getDashboard(TEACHER_ID, CLASS_ID)
            .getStudents().get(0);

        assertThat(res.getSupportReasons()).containsExactly("질문 만들기 참여가 필요해요");
    }

    /* 책수다방 가능 단계(duringDone=true)인데 chat_post 0건 -> 생각 나누기 부족만 발생 */
    @Test
    void getDashboard_bookChatEligibleButZeroPosts_returnsBookChatReasonOnly() {
        User student = buildStudent(S01_ID, "이재환");
        when(classStudentRepository.findBySchoolClassId(CLASS_ID)).thenReturn(List.of(buildMembership(student, 1)));

        // afterDone까지 true라 단계 미완료 사유는 뜨지 않고, finishedAt은 아직 null(완독 처리 전)이라
        // currentlyReading은 그대로 true다.
        ReadingRecord record = buildRecord(1L, student, null, "책", true, true, true);
        when(readingRecordRepository.findByStudent_IdAndFinishedAtIsNull(S01_ID)).thenReturn(Optional.of(record));
        when(individualAchievementService.calculate(1L)).thenReturn(
            result(S01_ID, 1L, 10, 90.0, 3, 90.0, 0, 0.0, 0, true, 0, LocalDate.of(2026, 6, 1)));

        TeacherIndividualReadingStudentResponse res = service.getDashboard(TEACHER_ID, CLASS_ID)
            .getStudents().get(0);

        assertThat(res.getSupportReasons()).containsExactly("생각 나누기 참여가 필요해요");
    }

    /* inspectedItemCount>=1, contentSuitabilityScore=49 -> 기록내용 적합성 부족만 발생 */
    @Test
    void getDashboard_lowContentSuitability_returnsContentSuitabilityReasonOnly() {
        User student = buildStudent(S01_ID, "이재환");
        when(classStudentRepository.findBySchoolClassId(CLASS_ID)).thenReturn(List.of(buildMembership(student, 1)));
        when(readingRecordRepository.findByStudent_IdAndFinishedAtIsNull(S01_ID)).thenReturn(Optional.empty());

        ReadingRecord finished = buildRecord(
            12L, student, LocalDateTime.of(2026, 8, 2, 0, 0), "책", true, true, true);
        when(readingRecordRepository.findFirstByStudent_IdAndFinishedAtIsNotNullOrderByFinishedAtDesc(S01_ID))
            .thenReturn(Optional.of(finished));
        when(individualAchievementService.calculate(12L)).thenReturn(
            result(S01_ID, 12L, 10, 90.0, 3, 90.0, 3, 49.0, 1, true, 2, LocalDate.of(2026, 8, 2)));

        TeacherIndividualReadingStudentResponse res = service.getDashboard(TEACHER_ID, CLASS_ID)
            .getStudents().get(0);

        assertThat(res.getSupportReasons()).containsExactly("기록 내용을 조금 더 다듬어야 해요");
    }

    /* 완독 기록의 낮은 독서실천도는 readingDays>=3일 때만 판정(과거 기록을 과도하게 경고하지 않음) */
    @Test
    void getDashboard_finishedRecordLowScoreButFewReadingDays_skipsPracticeReason() {
        User student = buildStudent(S01_ID, "이재환");
        when(classStudentRepository.findBySchoolClassId(CLASS_ID)).thenReturn(List.of(buildMembership(student, 1)));
        when(readingRecordRepository.findByStudent_IdAndFinishedAtIsNull(S01_ID)).thenReturn(Optional.empty());

        ReadingRecord finished = buildRecord(
            12L, student, LocalDateTime.of(2026, 8, 2, 0, 0), "책", true, true, true);
        when(readingRecordRepository.findFirstByStudent_IdAndFinishedAtIsNotNullOrderByFinishedAtDesc(S01_ID))
            .thenReturn(Optional.of(finished));
        // readingDays=1(<3)이므로 완독 기록에는 독서실천도 부족을 판정하지 않는다.
        when(individualAchievementService.calculate(12L)).thenReturn(
            result(S01_ID, 12L, 1, 10.0, 3, 90.0, 3, 90.0, 1, true, 2, LocalDate.of(2026, 7, 1)));

        TeacherIndividualReadingStudentResponse res = service.getDashboard(TEACHER_ID, CLASS_ID)
            .getStudents().get(0);

        assertThat(res.getSupportReasons()).doesNotContain("독서실천도가 낮아요");
    }

    /* 지원 필요 학생 수 = supportReasons가 1개 이상인 학생 수 */
    @Test
    void getDashboard_supportNeededStudentCount_countsStudentsWithAtLeastOneReason() {
        User needsHelp = buildStudent(S01_ID, "이재환");
        User doingFine = buildStudent(S02_ID, "김만수");
        when(classStudentRepository.findBySchoolClassId(CLASS_ID)).thenReturn(List.of(
            buildMembership(needsHelp, 1), buildMembership(doingFine, 2)));

        ReadingRecord recordA = buildRecord(1L, needsHelp, null, "책A");
        when(readingRecordRepository.findByStudent_IdAndFinishedAtIsNull(S01_ID)).thenReturn(Optional.of(recordA));
        when(individualAchievementService.calculate(1L)).thenReturn(
            result(S01_ID, 1L, 5, 10.0, 0, 0.0, 0, 0.0, 0, false, 0, LocalDate.of(2026, 6, 1)));

        ReadingRecord recordB = buildRecord(2L, doingFine, null, "책B");
        when(readingRecordRepository.findByStudent_IdAndFinishedAtIsNull(S02_ID)).thenReturn(Optional.of(recordB));
        when(individualAchievementService.calculate(2L)).thenReturn(
            result(S02_ID, 2L, 20, 90.0, 3, 90.0, 5, 90.0, 1, true, 3, LocalDate.of(2026, 8, 2)));

        TeacherIndividualReadingDashboardResponse response = service.getDashboard(TEACHER_ID, CLASS_ID);

        assertThat(response.getSupportNeededStudentCount()).isEqualTo(1);
    }

    /* 검증 19: 다른 교사의 학급 접근 거부 */
    @Test
    void getDashboard_classIdNotOwnedByTeacher_throwsForbidden() {
        assertThatThrownBy(() -> service.getDashboard(TEACHER_ID, 999L))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("담당 학급");
    }

    /* 검증 20: student 역할 계정 접근 거부(서비스 계층 방어) */
    @Test
    void getDashboard_studentRoleAccount_rejected() {
        User studentAccount = new User();
        studentAccount.setId(TEACHER_ID);
        studentAccount.setRole("student");
        when(userRepository.findById(TEACHER_ID)).thenReturn(Optional.of(studentAccount));

        assertThatThrownBy(() -> service.getDashboard(TEACHER_ID, CLASS_ID))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("교사 계정");
    }

    /* 검증 21: 교사 본인 소속 학급 자체가 없는 경우 */
    @Test
    void getDashboard_teacherHasNoClass_throwsNotFound() {
        when(schoolClassRepository.findByTeacherId(TEACHER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getDashboard(TEACHER_ID, CLASS_ID))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("담당 학급");
    }

    @Test
    void getDashboard_nonExistentTeacherId_throwsNotFound() {
        when(userRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getDashboard(404L, CLASS_ID))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("교사");
    }
}
