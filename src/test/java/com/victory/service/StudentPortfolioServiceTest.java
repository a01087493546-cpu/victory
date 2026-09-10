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
import com.victory.repository.BookRecommendationRepository;
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
    private final BookRecommendationRepository bookRecommendations = mock(BookRecommendationRepository.class);
    private final ReadingCompetencyCalculator readingCompetencyCalculator = new ReadingCompetencyCalculator();
    private final DemoReadingCompetencyProvider demoReadingCompetencyProvider = new DemoReadingCompetencyProvider();
    private final PracticeStageNarrativeBuilder practiceStageNarrativeBuilder = new PracticeStageNarrativeBuilder();
    private final DemoPracticeStageProvider demoPracticeStageProvider =
        new DemoPracticeStageProvider(new DemoPracticePortfolioAiProvider());
    private final StudentPortfolioService service = new StudentPortfolioService(users, classes, memberships,
        classBooks, records, responses, summaries, logs, practice, individual, bookRecommendations,
        readingCompetencyCalculator, demoReadingCompetencyProvider, practiceStageNarrativeBuilder,
        demoPracticeStageProvider);

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
        assertThat(result.beforeStage().participated()).isTrue();
        assertThat(result.duringStage().participated()).isTrue();
        assertThat(result.afterStage().participated()).isTrue();
        assertThat(result.beforeStage().growthNote()).isNotBlank();
        assertThat(result.duringStage().growthNote()).isNotBlank();
        assertThat(result.afterStage().growthNote()).isNotBlank();
        verify(practice).getClassAchievement(1L, 10L, 20L);
    }

    @Test
    void practicePortfolio_stageDetailsReflectActualStudentTextNotJustParticipation() {
        ClassReadingBook book = new ClassReadingBook();
        book.setId(20L); book.setSchoolClass(schoolClass); book.setBookTitle("마당을 나온 암탉");
        when(classBooks.findBySchoolClassId(10L)).thenReturn(Optional.of(book));
        StudentAchievementItem item = new StudentAchievementItem(2L, 3, "김학생", 0d, 0d, 0d, 0d,
            0d, 0d, 100d, true, 3, 3, 100d, 100d, false, List.of());
        when(practice.getClassAchievement(1L, 10L, 20L)).thenReturn(
            new PracticeAchievementResponse(10L, 20L, 1, 1, 100d, 0, List.of(item)));
        Response before = response("before");
        before.setContent("까마귀는 왜 알을 품어줬을까?");
        before.setExtraData(java.util.Map.of("question", "표지를 보고 궁금한 점은?"));
        Response during = response("during");
        during.setContent("주인공이 용감해서 그렇게 행동했을 거예요");
        when(responses.findByStudent_IdAndModeAndDeletedAtIsNullAndCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByIdAsc(
            eq(2L), eq("class"), any(), any())).thenReturn(List.of(before, during));
        Summary summary = new Summary();
        summary.setClassReadingBookId(20L);
        summary.setSummaryText("이 책은 가족의 소중함을 알려줘요");
        summary.setIsShared(true);
        when(summaries.findByStudent_IdAndCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByIdAsc(eq(2L), any(), any()))
            .thenReturn(List.of(summary));

        PracticePortfolioResponse result = service.getPracticePortfolio(1L, 10L, 2L, from, to);

        assertThat(result.beforeStage().shortTitle()).isEqualTo("질문 만들기에 참여함");
        assertThat(result.beforeStage().growthNote()).contains("까마귀는 왜 알을 품어줬을까?");
        assertThat(result.duringStage().shortTitle()).isEqualTo("근거를 들어 답함");
        assertThat(result.duringStage().growthNote()).contains("주인공이 용감해서 그렇게 행동했을 거예요");
        assertThat(result.afterStage().shortTitle()).isEqualTo("간추리기와 공유 활동 참여");
        assertThat(result.afterStage().growthNote()).contains("이 책은 가족의 소중함을 알려줘요").contains("나눴어요");
    }

    /*
     * 심사계정은 실제 class-mode Response 쿼리 결과(mock)를 더 이상 쓰지
     * 않고 DemoPracticeStageProvider의 결정적 진행 패턴을 쓴다 - "읽기
     * 전 미완료인데 읽기 중은 완료"처럼 앞뒤가 안 맞는 조합을 막기
     * 위함(요구사항: 자연스러운 진행 순서만 허용). 그래서 mock된
     * responses/summaries는 이 테스트에서 더는 참조되지 않는다.
     */
    @Test
    void demoPracticePortfolio_keepsExistingDemoAchievementAndSameResponseShape() {
        teacher.setDemoAccount(true); student.setDemoAccount(true); student.setLoginId("ss01");
        ClassReadingBook book = new ClassReadingBook();
        book.setId(20L); book.setSchoolClass(schoolClass); book.setBookTitle("마당을 나온 암탉");
        when(classBooks.findBySchoolClassId(10L)).thenReturn(Optional.of(book));
        StudentAchievementItem item = new StudentAchievementItem(2L, 3, "김학생", 0d, 0d, 0d, 0d,
            0d, 0d, 92.0, true, 5, 4, 90.0, 91.0, false, List.of());
        when(practice.getClassAchievement(1L, 10L, 20L)).thenReturn(
            new PracticeAchievementResponse(10L, 20L, 8, 6, 75d, 0, List.of(item)));

        PracticePortfolioResponse first = service.getPracticePortfolio(1L, 10L, 2L, from, to);
        PracticePortfolioResponse second = service.getPracticePortfolio(1L, 10L, 2L, from, to);

        assertThat(first).isEqualTo(second);
        assertThat(first.participationRate()).isEqualTo(92.0);
        assertThat(first.comprehensionRate()).isEqualTo(90.0);
        // ss01(김초롱)은 DemoPracticeStageProvider에서 전 단계 완료 패턴
        assertThat(first.beforeStatus()).isEqualTo("완료");
        assertThat(first.duringStatus()).isEqualTo("완료");
        assertThat(first.afterStatus()).isEqualTo("완료");
        assertThat(first.beforeStage().growthNote()).isNotBlank();
    }

    /* demo 진행 패턴이 학생마다 다르고, "뒤 단계만 완료"처럼 비논리적인 조합이 나오지 않는지. */
    @Test
    void demoPracticePortfolio_followsLogicalProgressionAndVariesByStudent() {
        teacher.setDemoAccount(true); student.setDemoAccount(true); student.setLoginId("demo_student_05"); // 김민지: 전부 미완료 패턴
        ClassReadingBook book = new ClassReadingBook();
        book.setId(20L); book.setSchoolClass(schoolClass); book.setBookTitle("마당을 나온 암탉");
        when(classBooks.findBySchoolClassId(10L)).thenReturn(Optional.of(book));
        StudentAchievementItem item = new StudentAchievementItem(2L, 3, "김학생", 0d, 0d, 0d, 0d,
            0d, 0d, 60.0, true, 2, 1, 55.0, 50.0, false, List.of());
        when(practice.getClassAchievement(1L, 10L, 20L)).thenReturn(
            new PracticeAchievementResponse(10L, 20L, 8, 6, 75d, 0, List.of(item)));

        PracticePortfolioResponse result = service.getPracticePortfolio(1L, 10L, 2L, from, to);

        assertThat(result.beforeStatus()).isEqualTo("미완료");
        assertThat(result.duringStatus()).isEqualTo("미완료");
        assertThat(result.afterStatus()).isEqualTo("미완료");
        assertThat(result.beforeStage().growthNote())
            .isEqualTo("현재 기록에서는 이 단계 활동 기록이 아직 없어요.");
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
    void demoPortfolio_isDeterministicAndUsesFixedStudentCompetencies() {
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
        assertThat(first.readingCompetencies().questionGeneration().score()).isEqualTo(55);
        assertThat(first.readingCompetencies().readingPersistence().score()).isEqualTo(65);
        assertThat(first.readingCompetencies().thoughtRefinement().score()).isEqualTo(50);
        assertThat(first.readingCompetencies().thoughtSharing().score()).isEqualTo(45);
    }

    @Test
    void noCompletedBooks_returnsStableZeroAverages() {
        when(records.findByStudent_IdAndFinishedAtIsNotNull(2L)).thenReturn(List.of());
        when(records.findByStudent_IdAndFinishedAtIsNull(2L)).thenReturn(Optional.empty());
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

        var competencies = result.readingCompetencies();
        assertThat(competencies.questionGeneration().score()).isZero();
        assertThat(competencies.questionGeneration().level()).isEqualTo("노력 필요");
        assertThat(competencies.readingPersistence().score()).isZero();
        assertThat(competencies.thoughtRefinement().score()).isZero();
        assertThat(competencies.thoughtSharing().score()).isZero();
    }

    /*
     * 완독 2권 + 진행 중 1권을 모두 포함해 4개 역량이 책 단위 활동
     * (질문 단계 완료 플래그, 책수다방 글, 친구 추천)에서 정확히
     * 계산되는지 확인한다. student_stat_reward_log(logs)는 이 계산에
     * 전혀 관여하지 않아야 하므로 courage=999처럼 극단적인 값을 넣어도
     * readingCompetencies에 영향이 없어야 한다.
     */
    @Test
    void individualPortfolio_calculatesReadingCompetenciesFromBookLevelActivity() {
        ReadingRecord bookA = completed(80, 100, LocalDateTime.of(2026, 3, 1, 10, 0));
        bookA.setId(100L); bookA.setBeforeDone(true); bookA.setDuringDone(true); bookA.setAfterDone(true);
        ReadingRecord bookB = completed(80, 80, LocalDateTime.of(2026, 5, 1, 10, 0));
        bookB.setId(101L); bookB.setBeforeDone(true); bookB.setDuringDone(false); bookB.setAfterDone(false);
        ReadingRecord inProgress = new ReadingRecord();
        inProgress.setId(102L); inProgress.setBeforeDone(true); inProgress.setDuringDone(true); inProgress.setAfterDone(false);

        when(records.findByStudent_IdAndFinishedAtIsNotNull(2L)).thenReturn(List.of(bookA, bookB));
        when(records.findByStudent_IdAndFinishedAtIsNull(2L)).thenReturn(Optional.of(inProgress));
        when(responses.findByStudent_IdAndModeAndDeletedAtIsNullAndCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByIdAsc(
            eq(2L), eq("individual"), any(), any())).thenReturn(List.of());
        when(summaries.findByStudent_IdAndCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByIdAsc(eq(2L), any(), any()))
            .thenReturn(List.of());
        when(logs.findByStudent_IdAndGrantedAtGreaterThanEqualAndGrantedAtLessThan(eq(2L), any(), any()))
            .thenReturn(List.of(log("CHAT", "courage", 999)));
        when(individual.getMonthlyCompletionStats(2L)).thenReturn(new MonthlyCompletionStatsResponse(2026,
            List.of(0,0,0,0,0,0,0,0,0,0,0,0)));

        // bookA: 책수다방 글 있음(나눔 참여). bookB: 아무 나눔 없음.
        when(responses.findByReadingRecord_IdAndModeAndDeletedAtIsNullOrderByIdAsc(100L, "individual"))
            .thenReturn(List.of(chat()));
        when(responses.findByReadingRecord_IdAndModeAndDeletedAtIsNullOrderByIdAsc(101L, "individual"))
            .thenReturn(List.of());
        // inProgress: 친구 추천으로 나눔 참여.
        when(responses.findByReadingRecord_IdAndModeAndDeletedAtIsNullOrderByIdAsc(102L, "individual"))
            .thenReturn(List.of());
        when(bookRecommendations.findByReadingRecord_Id(100L)).thenReturn(List.of());
        when(bookRecommendations.findByReadingRecord_Id(101L)).thenReturn(List.of());
        when(bookRecommendations.findByReadingRecord_Id(102L)).thenReturn(List.of(new com.victory.entity.BookRecommendation()));

        IndividualPortfolioResponse result = service.getIndividualPortfolio(1L, 10L, 2L, from, to);
        var competencies = result.readingCompetencies();

        // 질문 생성 역량: (100 + 33.33 + 66.67) / 3 = 66.67 -> 67
        assertThat(competencies.questionGeneration().score()).isEqualTo(67);
        assertThat(competencies.questionGeneration().level()).isEqualTo("우수");
        // 독서 지속/생각 다듬기 역량: 완독 2권 평균을 그대로 재사용.
        assertThat(result.averageReadingPracticeScore()).isEqualByComparingTo("80.00");
        assertThat(competencies.readingPersistence().score()).isEqualTo(80);
        assertThat(result.averageRecordCompletionScore()).isEqualByComparingTo("90.00");
        assertThat(competencies.thoughtRefinement().score()).isEqualTo(90);
        // 생각 나눔 역량: 책 3권 중 2권(bookA, inProgress) 나눔 참여 -> 66.67 -> 67
        assertThat(competencies.thoughtSharing().score()).isEqualTo(67);
        assertThat(competencies.thoughtSharing().level()).isEqualTo("우수");
    }

    /*
     * 책 단위로 귀속되지 않는 책수다방 댓글(chat_reply)은 나눔 참여
     * 책이 아직 전체보다 적을 때만 최대 1권 분의 보너스로만 반영된다.
     */
    @Test
    void individualPortfolio_thoughtSharing_unattributedReplyGivesAtMostOneBonusBook() {
        ReadingRecord bookA = completed(50, 50, LocalDateTime.of(2026, 3, 1, 10, 0));
        bookA.setId(200L);
        ReadingRecord bookB = completed(50, 50, LocalDateTime.of(2026, 4, 1, 10, 0));
        bookB.setId(201L);

        when(records.findByStudent_IdAndFinishedAtIsNotNull(2L)).thenReturn(List.of(bookA, bookB));
        when(records.findByStudent_IdAndFinishedAtIsNull(2L)).thenReturn(Optional.empty());
        Response reply = new Response(); reply.setContentType("chat_reply");
        when(responses.findByStudent_IdAndModeAndDeletedAtIsNullAndCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByIdAsc(
            eq(2L), eq("individual"), any(), any())).thenReturn(List.of(reply));
        when(summaries.findByStudent_IdAndCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByIdAsc(eq(2L), any(), any()))
            .thenReturn(List.of());
        when(logs.findByStudent_IdAndGrantedAtGreaterThanEqualAndGrantedAtLessThan(eq(2L), any(), any())).thenReturn(List.of());
        when(individual.getMonthlyCompletionStats(2L)).thenReturn(new MonthlyCompletionStatsResponse(2026,
            List.of(0,0,0,0,0,0,0,0,0,0,0,0)));
        when(responses.findByReadingRecord_IdAndModeAndDeletedAtIsNullOrderByIdAsc(200L, "individual")).thenReturn(List.of());
        when(responses.findByReadingRecord_IdAndModeAndDeletedAtIsNullOrderByIdAsc(201L, "individual")).thenReturn(List.of());
        when(bookRecommendations.findByReadingRecord_Id(200L)).thenReturn(List.of());
        when(bookRecommendations.findByReadingRecord_Id(201L)).thenReturn(List.of());

        IndividualPortfolioResponse result = service.getIndividualPortfolio(1L, 10L, 2L, from, to);

        // 귀속된 나눔 활동은 0권이지만, 댓글 보너스로 1권만 인정 -> 1/2 = 50
        assertThat(result.readingCompetencies().thoughtSharing().score()).isEqualTo(50);
        assertThat(result.readingCompetencies().thoughtSharing().level()).isEqualTo("보통");
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
