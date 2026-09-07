package com.victory.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.victory.dto.IndividualPortfolioResponse;
import com.victory.dto.MonthlyCompletionStatsResponse;
import com.victory.dto.PracticeAchievementResponse;
import com.victory.dto.PracticePortfolioResponse;
import com.victory.dto.StudentAchievementItem;
import com.victory.entity.ClassReadingBook;
import com.victory.entity.ClassStudent;
import com.victory.entity.ReadingRecord;
import com.victory.entity.Response;
import com.victory.entity.SchoolClass;
import com.victory.entity.StudentStatRewardLog;
import com.victory.entity.Summary;
import com.victory.entity.User;
import com.victory.repository.ClassReadingBookRepository;
import com.victory.repository.ClassStudentRepository;
import com.victory.repository.ReadingRecordRepository;
import com.victory.repository.ResponseRepository;
import com.victory.repository.SchoolClassRepository;
import com.victory.repository.StudentStatRewardLogRepository;
import com.victory.repository.SummaryRepository;
import com.victory.repository.UserRepository;

class StudentPortfolioServiceTest {
    private final UserRepository users = mock(UserRepository.class);
    private final SchoolClassRepository classes = mock(SchoolClassRepository.class);
    private final ClassStudentRepository memberships = mock(ClassStudentRepository.class);
    private final ClassReadingBookRepository classBooks = mock(ClassReadingBookRepository.class);
    private final ReadingRecordRepository records = mock(ReadingRecordRepository.class);
    private final ResponseRepository responses = mock(ResponseRepository.class);
    private final SummaryRepository summaries = mock(SummaryRepository.class);
    private final StudentStatRewardLogRepository logs = mock(StudentStatRewardLogRepository.class);
    private final PracticeAchievementService practice = mock(PracticeAchievementService.class);
    private final IndividualReadingService individual = mock(IndividualReadingService.class);
    private final StudentPortfolioService service = new StudentPortfolioService(users, classes, memberships,
        classBooks, records, responses, summaries, logs, practice, individual);

    private User teacher;
    private User student;
    private SchoolClass schoolClass;
    private final LocalDate from = LocalDate.of(2026, 1, 1);
    private final LocalDate to = LocalDate.of(2026, 12, 31);

    @BeforeEach
    void membership() {
        teacher = user(1L, "teacher", false, "teacher", "선생님");
        student = user(2L, "student", false, "student", "김학생");
        schoolClass = new SchoolClass();
        schoolClass.setId(10L); schoolClass.setTeacher(teacher); schoolClass.setGrade(4); schoolClass.setClassNumber(2);
        ClassStudent member = new ClassStudent();
        member.setSchoolClass(schoolClass); member.setStudent(student); member.setStudentNumber(3);
        when(users.findById(1L)).thenReturn(Optional.of(teacher));
        when(classes.findByTeacherId(1L)).thenReturn(Optional.of(schoolClass));
        when(memberships.findByStudentId(2L)).thenReturn(Optional.of(member));
    }

    @Test
    void practicePortfolio_reusesAchievementAndCountsPeriodActivities() {
        ClassReadingBook book = new ClassReadingBook();
        book.setId(20L); book.setSchoolClass(schoolClass); book.setBookTitle("마당을 나온 암탉");
        when(classBooks.findBySchoolClassId(10L)).thenReturn(Optional.of(book));
        StudentAchievementItem item = new StudentAchievementItem(2L, 3, "김학생", 0d, 0d, 0d, 0d,
            0d, 0d, 82.5, true, 1, 1, 91.0, 86.75, false, List.of());
        when(practice.getClassAchievement(1L, 10L, 20L)).thenReturn(
            new PracticeAchievementResponse(10L, 20L, 1, 1, 100d, 0, List.of(item)));
        when(responses.findByStudent_IdAndModeAndDeletedAtIsNullAndCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByIdAsc(
            eq(2L), eq("class"), any(), any())).thenReturn(List.of(response("before"), response("during"), response("after")));
        Summary summary = new Summary(); summary.setClassReadingBookId(20L);
        when(summaries.findByStudent_IdAndCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByIdAsc(eq(2L), any(), any()))
            .thenReturn(List.of(summary));

        PracticePortfolioResponse result = service.getPracticePortfolio(1L, 10L, 2L, from, to);

        assertThat(result.participationRate()).isEqualTo(82.5);
        assertThat(result.comprehensionRate()).isEqualTo(91.0);
        assertThat(result.beforeParticipation().count()).isEqualTo(1);
        assertThat(result.activityCount()).isEqualTo(4);
        verify(practice).getClassAchievement(1L, 10L, 20L);
    }

    @Test
    void demoPracticePortfolio_keepsExistingDemoAchievementAndSameResponseShape() {
        teacher.setDemoAccount(true); student.setDemoAccount(true);
        ClassReadingBook book = new ClassReadingBook();
        book.setId(20L); book.setSchoolClass(schoolClass); book.setBookTitle("마당을 나온 암탉");
        when(classBooks.findBySchoolClassId(10L)).thenReturn(Optional.of(book));
        StudentAchievementItem item = new StudentAchievementItem(2L, 3, "김학생", 0d, 0d, 0d, 0d,
            0d, 0d, 92.0, true, 5, 4, 90.0, 91.0, false, List.of());
        when(practice.getClassAchievement(1L, 10L, 20L)).thenReturn(
            new PracticeAchievementResponse(10L, 20L, 8, 6, 75d, 0, List.of(item)));
        when(responses.findByStudent_IdAndModeAndDeletedAtIsNullAndCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByIdAsc(
            eq(2L), eq("class"), any(), any())).thenReturn(List.of(response("before"), response("during")));
        when(summaries.findByStudent_IdAndCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByIdAsc(eq(2L), any(), any()))
            .thenReturn(List.of());

        PracticePortfolioResponse first = service.getPracticePortfolio(1L, 10L, 2L, from, to);
        PracticePortfolioResponse second = service.getPracticePortfolio(1L, 10L, 2L, from, to);

        assertThat(first).isEqualTo(second);
        assertThat(first.participationRate()).isEqualTo(92.0);
        assertThat(first.comprehensionRate()).isEqualTo(90.0);
        assertThat(first.beforeStatus()).isEqualTo("완료");
    }

    @Test
    void individualPortfolio_filtersCompletedRecordsCalculatesAveragesActivitiesAndLoggedCompetencies() {
        ReadingRecord first = completed(80, 90, LocalDateTime.of(2026, 3, 1, 10, 0));
        ReadingRecord second = completed(90, 100, LocalDateTime.of(2026, 5, 1, 10, 0));
        ReadingRecord outside = completed(10, 10, LocalDateTime.of(2025, 5, 1, 10, 0));
        when(records.findByStudent_IdAndFinishedAtIsNotNull(2L)).thenReturn(List.of(first, second, outside));
        when(responses.findByStudent_IdAndModeAndDeletedAtIsNullAndCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByIdAsc(
            eq(2L), eq("individual"), any(), any())).thenReturn(List.of(response("before"), response("during"), chat()));
        Summary summary = new Summary(); summary.setReadingRecord(first);
        when(summaries.findByStudent_IdAndCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByIdAsc(eq(2L), any(), any()))
            .thenReturn(List.of(summary));
        StudentStatRewardLog after = log("INDIVIDUAL_AFTER_COMPLETE", "stamina_magic_wisdom", 1);
        StudentStatRewardLog courage = log("CHAT", "courage", 2);
        when(logs.findByStudent_IdAndGrantedAtGreaterThanEqualAndGrantedAtLessThan(eq(2L), any(), any()))
            .thenReturn(List.of(after, courage));
        when(individual.getMonthlyCompletionStats(2L)).thenReturn(new MonthlyCompletionStatsResponse(2026,
            List.of(0,0,1,0,1,0,0,0,0,0,0,0)));

        IndividualPortfolioResponse result = service.getIndividualPortfolio(1L, 10L, 2L, from, to);

        assertThat(result.completedBookCount()).isEqualTo(2);
        assertThat(result.averageReadingPracticeScore()).isEqualByComparingTo("85.00");
        assertThat(result.averageRecordCompletionScore()).isEqualByComparingTo("95.00");
        assertThat(result.activitySummary().questions()).isEqualTo(1);
        assertThat(result.activitySummary().thoughtWriting()).isEqualTo(1);
        assertThat(result.activitySummary().summaries()).isEqualTo(1);
        assertThat(result.activitySummary().bookChatSharing()).isEqualTo(1);
        assertThat(result.competencies().readingPersistence()).isEqualTo(3);
        assertThat(result.competencies().questionGeneration()).isEqualTo(1);
        assertThat(result.competencies().thoughtRefinement()).isEqualTo(1);
        assertThat(result.competencies().thoughtSharing()).isEqualTo(2);
    }

    @Test
    void demoPortfolio_isDeterministicAndDerivesNonEmptyCompetenciesFromSeedActivities() {
        teacher.setDemoAccount(true); student.setDemoAccount(true);
        ReadingRecord record = completed(90, 94, LocalDateTime.of(2026, 4, 1, 10, 0)); record.setId(50L);
        Response before = response("before"); before.setReadingRecord(record); before.setActivityDate(LocalDate.of(2026, 4, 1));
        Response during = response("during"); during.setReadingRecord(record); during.setActivityDate(LocalDate.of(2026, 4, 2));
        Response after = response("after"); after.setReadingRecord(record); after.setActivityDate(LocalDate.of(2026, 4, 3));
        when(records.findByStudent_IdAndFinishedAtIsNotNull(2L)).thenReturn(List.of(record));
        Response chat = chat(); chat.setReadingRecord(record);
        when(responses.findByStudent_IdAndModeAndDeletedAtIsNullOrderByIdAsc(2L, "individual"))
            .thenReturn(List.of(before, during, after, chat));
        Summary summary = new Summary(); summary.setReadingRecord(record);
        when(summaries.findByStudent_IdAndReadingRecordIsNotNullOrderByIdAsc(2L)).thenReturn(List.of(summary));
        when(logs.findByStudent_IdAndGrantedAtGreaterThanEqualAndGrantedAtLessThan(eq(2L), any(), any())).thenReturn(List.of());
        when(individual.getMonthlyCompletionStats(2L)).thenReturn(new MonthlyCompletionStatsResponse(2026,
            List.of(0,0,0,1,0,0,0,0,0,0,0,0)));

        IndividualPortfolioResponse first = service.getIndividualPortfolio(1L, 10L, 2L, from, to);
        IndividualPortfolioResponse second = service.getIndividualPortfolio(1L, 10L, 2L, from, to);

        assertThat(first).isEqualTo(second);
        assertThat(first.demoDerived()).isTrue();
        assertThat(first.competencies().questionGeneration()).isEqualTo(3);
        assertThat(first.competencies().readingPersistence()).isEqualTo(3);
        assertThat(first.competencies().thoughtRefinement()).isEqualTo(3);
        assertThat(first.competencies().thoughtSharing()).isEqualTo(1);
    }

    @Test
    void noCompletedBooks_returnsStableZeroAverages() {
        when(records.findByStudent_IdAndFinishedAtIsNotNull(2L)).thenReturn(List.of());
        when(responses.findByStudent_IdAndModeAndDeletedAtIsNullAndCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByIdAsc(
            eq(2L), eq("individual"), any(), any())).thenReturn(List.of());
        when(summaries.findByStudent_IdAndCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByIdAsc(eq(2L), any(), any()))
            .thenReturn(List.of());
        when(logs.findByStudent_IdAndGrantedAtGreaterThanEqualAndGrantedAtLessThan(eq(2L), any(), any())).thenReturn(List.of());
        when(individual.getMonthlyCompletionStats(2L)).thenReturn(new MonthlyCompletionStatsResponse(2026,
            List.of(0,0,0,0,0,0,0,0,0,0,0,0)));
        IndividualPortfolioResponse result = service.getIndividualPortfolio(1L, 10L, 2L, from, to);
        assertThat(result.completedBookCount()).isZero();
        assertThat(result.averageReadingPracticeScore()).isEqualByComparingTo("0.00");
        assertThat(result.averageRecordCompletionScore()).isEqualByComparingTo("0.00");
    }

    private User user(long id, String role, boolean demo, String login, String name) {
        User user = new User(); user.setId(id); user.setRole(role); user.setDemoAccount(demo); user.setLoginId(login); user.setName(name); return user;
    }
    private Response response(String stage) { Response r = new Response(); r.setStage(stage); r.setContentType("answer"); return r; }
    private Response chat() { Response r = new Response(); r.setContentType("chat_post"); return r; }
    private ReadingRecord completed(int practice, int completion, LocalDateTime at) {
        ReadingRecord r = new ReadingRecord(); r.setFinalReadingPracticeScore(practice); r.setFinalRecordCompletionScore(completion); r.setFinishedAt(at); return r;
    }
    private StudentStatRewardLog log(String reward, String stat, int amount) {
        StudentStatRewardLog log = new StudentStatRewardLog(); log.setRewardType(reward); log.setStatType(stat); log.setAmount(amount); return log;
    }
}
