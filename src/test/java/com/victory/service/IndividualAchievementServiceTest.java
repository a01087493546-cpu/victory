package com.victory.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import com.victory.dto.IndividualAchievementLevel;
import com.victory.dto.IndividualAchievementResult;
import com.victory.entity.AiEvaluationAttempt;
import com.victory.entity.BookRecommendation;
import com.victory.entity.ReadingProgressLog;
import com.victory.entity.ReadingRecord;
import com.victory.entity.Response;
import com.victory.entity.Summary;
import com.victory.entity.User;
import com.victory.repository.AiEvaluationAttemptRepository;
import com.victory.repository.BookRecommendationRepository;
import com.victory.repository.ReadingProgressLogRepository;
import com.victory.repository.ReadingRecordRepository;
import com.victory.repository.ResponseRepository;
import com.victory.repository.SummaryRepository;

class IndividualAchievementServiceTest {

    private static final Long STUDENT_ID = 1L;
    private static final Long READING_RECORD_ID = 10L;
    private static final List<String> AI_TYPE_WHITELIST = List.of(
        "pre_reading_question", "during_reading_question", "individual_question", "individual_summary");

    private final ReadingRecordRepository readingRecordRepository = mock(ReadingRecordRepository.class);
    private final ResponseRepository responseRepository = mock(ResponseRepository.class);
    private final SummaryRepository summaryRepository = mock(SummaryRepository.class);
    private final ReadingProgressLogRepository readingProgressLogRepository =
        mock(ReadingProgressLogRepository.class);
    private final BookRecommendationRepository bookRecommendationRepository =
        mock(BookRecommendationRepository.class);
    private final AiEvaluationAttemptRepository aiEvaluationAttemptRepository =
        mock(AiEvaluationAttemptRepository.class);
    private final IndividualAchievementCalculator calculator = new IndividualAchievementCalculator();

    private final IndividualAchievementService service = new IndividualAchievementService(
        readingRecordRepository, responseRepository, summaryRepository, readingProgressLogRepository,
        bookRecommendationRepository, aiEvaluationAttemptRepository, calculator);

    private ReadingRecord buildRecord(boolean beforeDone, boolean duringDone, boolean afterDone) {
        ReadingRecord record = new ReadingRecord();
        record.setId(READING_RECORD_ID);
        User student = new User();
        student.setId(STUDENT_ID);
        student.setName("학생1");
        student.setRole("student");
        record.setStudent(student);
        record.setBeforeDone(beforeDone);
        record.setDuringDone(duringDone);
        record.setAfterDone(afterDone);
        return record;
    }

    private void stubEmptyDataExcept(ReadingRecord record) {
        when(readingRecordRepository.findById(READING_RECORD_ID)).thenReturn(Optional.of(record));
        when(responseRepository.findByReadingRecord_IdAndModeAndDeletedAtIsNullOrderByIdAsc(
            eq(READING_RECORD_ID), eq("individual"))).thenReturn(List.of());
        when(readingProgressLogRepository.findByReadingRecord_Id(READING_RECORD_ID)).thenReturn(List.of());
        when(summaryRepository.findByStudent_IdAndReadingRecord_Id(STUDENT_ID, READING_RECORD_ID))
            .thenReturn(Optional.empty());
        when(bookRecommendationRepository.findByReadingRecord_Id(READING_RECORD_ID)).thenReturn(List.of());
        when(aiEvaluationAttemptRepository.findByReadingRecordIdAndActivityTypeIn(
            eq(READING_RECORD_ID), any())).thenReturn(List.of());
        when(readingRecordRepository.countByStudent_IdAndFinishedAtIsNotNull(STUDENT_ID)).thenReturn(0L);
    }

    private Response answerResponse(String stage, LocalDate activityDate, LocalDateTime createdAt) {
        Response response = new Response();
        response.setMode("individual");
        response.setContentType("answer");
        response.setStage(stage);
        response.setActivityDate(activityDate);
        response.setCreatedAt(createdAt);
        return response;
    }

    private Response chatPostResponse(LocalDate activityDate, LocalDateTime createdAt) {
        Response response = new Response();
        response.setMode("individual");
        response.setContentType("chat_post");
        response.setActivityDate(activityDate);
        response.setCreatedAt(createdAt);
        return response;
    }

    private ReadingProgressLog progressLog(LocalDate logDate) {
        ReadingProgressLog log = new ReadingProgressLog();
        log.setLogDate(logDate);
        return log;
    }

    private BookRecommendation recommendation(LocalDateTime createdAt) {
        BookRecommendation recommendation = new BookRecommendation();
        recommendation.setCreatedAt(createdAt);
        return recommendation;
    }

    private Summary summary(LocalDateTime createdAt) {
        Summary summary = new Summary();
        summary.setCreatedAt(createdAt);
        return summary;
    }

    private AiEvaluationAttempt attempt(String evaluationKey, int attemptNumber, String status) {
        AiEvaluationAttempt attempt = new AiEvaluationAttempt();
        attempt.setStudentId(STUDENT_ID);
        attempt.setReadingRecordId(READING_RECORD_ID);
        attempt.setActivityType("individual_question");
        attempt.setEvaluationKey(evaluationKey);
        attempt.setAttemptNumber(attemptNumber);
        attempt.setStatus(status);
        return attempt;
    }

    /* 검증 1: 같은 날 페이지·질문·책수다방을 모두 수행 → 독서일수 1일 */
    @Test
    void calculate_sameDayMultipleActivityKinds_readingDaysIsOne() {
        ReadingRecord record = buildRecord(false, false, false);
        LocalDate day = LocalDate.of(2026, 6, 1);
        stubEmptyDataExcept(record);
        when(readingProgressLogRepository.findByReadingRecord_Id(READING_RECORD_ID))
            .thenReturn(List.of(progressLog(day)));
        when(responseRepository.findByReadingRecord_IdAndModeAndDeletedAtIsNullOrderByIdAsc(
            eq(READING_RECORD_ID), eq("individual")))
            .thenReturn(List.of(
                answerResponse("during", day, day.atStartOfDay()),
                chatPostResponse(day, day.atStartOfDay())));

        IndividualAchievementResult result = service.calculate(READING_RECORD_ID);

        assertThat(result.getReadingDays()).isEqualTo(1);
    }

    /* 검증 2: 서로 다른 3일에 활동 → 독서일수 3일 */
    @Test
    void calculate_threeDifferentDays_readingDaysIsThree() {
        ReadingRecord record = buildRecord(false, false, false);
        LocalDate d1 = LocalDate.of(2026, 6, 1);
        LocalDate d2 = LocalDate.of(2026, 6, 2);
        LocalDate d3 = LocalDate.of(2026, 6, 3);
        stubEmptyDataExcept(record);
        when(readingProgressLogRepository.findByReadingRecord_Id(READING_RECORD_ID))
            .thenReturn(List.of(progressLog(d1)));
        when(responseRepository.findByReadingRecord_IdAndModeAndDeletedAtIsNullOrderByIdAsc(
            eq(READING_RECORD_ID), eq("individual")))
            .thenReturn(List.of(answerResponse("during", d2, d2.atStartOfDay())));
        when(bookRecommendationRepository.findByReadingRecord_Id(READING_RECORD_ID))
            .thenReturn(List.of(recommendation(d3.atStartOfDay())));

        IndividualAchievementResult result = service.calculate(READING_RECORD_ID);

        assertThat(result.getReadingDays()).isEqualTo(3);
    }

    /* 검증 5: 읽기 전만 참여 → 1종, 10점 */
    @Test
    void calculate_onlyPreReadingQuestion_activityTypeCountIsOneAndScoreIsTen() {
        ReadingRecord record = buildRecord(false, false, false);
        LocalDate day = LocalDate.of(2026, 6, 1);
        stubEmptyDataExcept(record);
        when(responseRepository.findByReadingRecord_IdAndModeAndDeletedAtIsNullOrderByIdAsc(
            eq(READING_RECORD_ID), eq("individual")))
            .thenReturn(List.of(answerResponse("before", null, day.atStartOfDay())));

        IndividualAchievementResult result = service.calculate(READING_RECORD_ID);

        assertThat(result.getActivityTypeCount()).isEqualTo(1);
        assertThat(result.getActivityTypeScore()).isEqualTo(10.0);
    }

    /* 검증 6: 5종 모두 참여 → 5종, 50점 */
    @Test
    void calculate_allFiveActivityKinds_activityTypeCountIsFiveAndScoreIsFifty() {
        ReadingRecord record = buildRecord(false, false, false);
        LocalDate day = LocalDate.of(2026, 6, 1);
        stubEmptyDataExcept(record);
        when(responseRepository.findByReadingRecord_IdAndModeAndDeletedAtIsNullOrderByIdAsc(
            eq(READING_RECORD_ID), eq("individual")))
            .thenReturn(List.of(
                answerResponse("before", null, day.atStartOfDay()),
                answerResponse("during", day, day.atStartOfDay()),
                answerResponse("after", null, day.atStartOfDay()),
                chatPostResponse(day, day.atStartOfDay())));
        when(bookRecommendationRepository.findByReadingRecord_Id(READING_RECORD_ID))
            .thenReturn(List.of(recommendation(day.atStartOfDay())));

        IndividualAchievementResult result = service.calculate(READING_RECORD_ID);

        assertThat(result.getActivityTypeCount()).isEqualTo(5);
        assertThat(result.getActivityTypeScore()).isEqualTo(50.0);
    }

    /* 검증 7: 같은 활동을 여러 번 해도 활동 종류 수는 중복 증가하지 않음 */
    @Test
    void calculate_sameActivityKindMultipleTimes_activityTypeCountNotDuplicated() {
        ReadingRecord record = buildRecord(false, false, false);
        LocalDate d1 = LocalDate.of(2026, 6, 1);
        LocalDate d2 = LocalDate.of(2026, 6, 2);
        stubEmptyDataExcept(record);
        when(responseRepository.findByReadingRecord_IdAndModeAndDeletedAtIsNullOrderByIdAsc(
            eq(READING_RECORD_ID), eq("individual")))
            .thenReturn(List.of(
                answerResponse("during", d1, d1.atStartOfDay()),
                answerResponse("during", d2, d2.atStartOfDay())));

        IndividualAchievementResult result = service.calculate(READING_RECORD_ID);

        assertThat(result.getActivityTypeCount()).isEqualTo(1);
        assertThat(result.getReadingDays()).isEqualTo(2);
    }

    /* 검증 8~10: 활동완료율은 ReadingRecord의 beforeDone/duringDone/afterDone을 그대로 반영 */
    @Test
    void calculate_stageCompletion_reflectsRecordFlagsOnly() {
        ReadingRecord record = buildRecord(true, true, false);
        stubEmptyDataExcept(record);

        IndividualAchievementResult result = service.calculate(READING_RECORD_ID);

        assertThat(result.getCompletedStageCount()).isEqualTo(2);
        assertThat(result.getStageCompletionRate()).isCloseTo(66.67, org.assertj.core.data.Offset.offset(0.01));
    }

    /* 검증 11~14: evaluationKey A/B/C 조합 → 분모 3, 성공 2, 적합성 약 66.67 */
    @Test
    void calculate_aiEvaluation_groupsByEvaluationKeyAcrossThreeQuestions() {
        ReadingRecord record = buildRecord(false, false, false);
        stubEmptyDataExcept(record);
        when(aiEvaluationAttemptRepository.findByReadingRecordIdAndActivityTypeIn(
            eq(READING_RECORD_ID), any()))
            .thenReturn(List.of(
                // A: 1회 need, 2회 good → 성공
                attempt("A", 2, "good"),
                attempt("A", 1, "need"),
                // B: 1~3회 need, 4회 good → 실패
                attempt("B", 1, "need"),
                attempt("B", 4, "good"),
                attempt("B", 3, "need"),
                attempt("B", 2, "need"),
                // C: 1회 good → 성공
                attempt("C", 1, "good")));

        IndividualAchievementResult result = service.calculate(READING_RECORD_ID);

        assertThat(result.getInspectedItemCount()).isEqualTo(3);
        assertThat(result.getPassedWithinThreeCount()).isEqualTo(2);
        assertThat(result.getContentSuitabilityScore())
            .isCloseTo(66.67, org.assertj.core.data.Offset.offset(0.01));
    }

    /*
     * "읽기 후 질문 또는 간추리기"는 하나로 묶인 활동 종류다 - 간추리기만
     * 있어도(읽기 후 answer 응답이 없어도) 그 종류가 채워진 것으로 본다.
     */
    @Test
    void calculate_summaryAlone_countsAsAfterActivityType() {
        ReadingRecord record = buildRecord(false, false, false);
        LocalDate day = LocalDate.of(2026, 6, 5);
        stubEmptyDataExcept(record);
        when(summaryRepository.findByStudent_IdAndReadingRecord_Id(STUDENT_ID, READING_RECORD_ID))
            .thenReturn(Optional.of(summary(day.atStartOfDay())));

        IndividualAchievementResult result = service.calculate(READING_RECORD_ID);

        assertThat(result.getActivityTypeCount()).isEqualTo(1);
        assertThat(result.getReadingDays()).isEqualTo(1);
    }

    /* 검증 15: 검사 대상 0개 → 0점 */
    @Test
    void calculate_zeroAiAttempts_contentSuitabilityScoreIsZero() {
        ReadingRecord record = buildRecord(false, false, false);
        stubEmptyDataExcept(record);

        IndividualAchievementResult result = service.calculate(READING_RECORD_ID);

        assertThat(result.getInspectedItemCount()).isEqualTo(0);
        assertThat(result.getContentSuitabilityScore()).isEqualTo(0.0);
    }

    /* 검증 16: 같은 evaluationKey의 여러 시도 → 분모 1개 */
    @Test
    void calculate_sameEvaluationKeyMultipleAttempts_countedOnceInDenominator() {
        ReadingRecord record = buildRecord(false, false, false);
        stubEmptyDataExcept(record);
        when(aiEvaluationAttemptRepository.findByReadingRecordIdAndActivityTypeIn(
            eq(READING_RECORD_ID), any()))
            .thenReturn(List.of(
                attempt("X", 1, "need"),
                attempt("X", 2, "need"),
                attempt("X", 3, "good")));

        IndividualAchievementResult result = service.calculate(READING_RECORD_ID);

        assertThat(result.getInspectedItemCount()).isEqualTo(1);
        assertThat(result.getPassedWithinThreeCount()).isEqualTo(1);
    }

    /*
     * 검증 17/18: 다른 readingRecord의 시도·온책읽기(classReadingBookId) 평가는
     * 계산에서 제외되어야 한다. 이 서비스는 정확히 이 readingRecordId +
     * 개별읽기 activityType 화이트리스트로만 조회하므로, 다른 책의 시도나
     * readingRecordId가 채워지지 않는 온책읽기 시도는 애초에 이 조회
     * 결과에 나타날 수 없다(JPA가 reading_record_id = :id로 변환하므로
     * NULL이거나 다른 값인 행은 SQL 3치 논리상 절대 일치하지 않는다).
     * 여기서는 서비스가 실제로 그 정확한 조건으로 조회를 호출하는지 검증한다.
     */
    @Test
    void calculate_aiEvaluationQuery_scopedToExactReadingRecordIdAndWhitelist() {
        ReadingRecord record = buildRecord(false, false, false);
        stubEmptyDataExcept(record);

        service.calculate(READING_RECORD_ID);

        verify(aiEvaluationAttemptRepository)
            .findByReadingRecordIdAndActivityTypeIn(eq(READING_RECORD_ID), eq(AI_TYPE_WHITELIST));
    }

    /*
     * 검증 27: 과거 readingRecordId가 NULL인 책수다방·추천 글은 계산에서
     * 제외되어야 한다. bookRecommendationRepository.findByReadingRecord_Id는
     * reading_record_id = :id로만 조회하므로 NULL 값 행은 결과에 포함될 수
     * 없다 - 여기서는 서비스가 정확히 이 readingRecordId로만 조회함을 검증한다.
     */
    @Test
    void calculate_bookRecommendationQuery_scopedToExactReadingRecordId() {
        ReadingRecord record = buildRecord(false, false, false);
        stubEmptyDataExcept(record);

        service.calculate(READING_RECORD_ID);

        verify(bookRecommendationRepository).findByReadingRecord_Id(eq(READING_RECORD_ID));
    }

    /* 검증 22/23: 학생의 완독 기록 수만 세고, 다른 학생의 기록은 포함하지 않음 */
    @Test
    void calculate_totalCompletedBookCount_onlyThisStudent() {
        ReadingRecord record = buildRecord(false, false, false);
        stubEmptyDataExcept(record);
        when(readingRecordRepository.countByStudent_IdAndFinishedAtIsNotNull(STUDENT_ID)).thenReturn(3L);

        IndividualAchievementResult result = service.calculate(READING_RECORD_ID);

        assertThat(result.getTotalCompletedBookCount()).isEqualTo(3L);
        verify(readingRecordRepository).countByStudent_IdAndFinishedAtIsNotNull(eq(STUDENT_ID));
    }

    /* 검증 24: 데이터가 하나도 없는 새 ReadingRecord */
    @Test
    void calculate_newEmptyRecord_allZerosAndNeedSupportLevel() {
        ReadingRecord record = buildRecord(false, false, false);
        stubEmptyDataExcept(record);

        IndividualAchievementResult result = service.calculate(READING_RECORD_ID);

        assertThat(result.getReadingDays()).isEqualTo(0);
        assertThat(result.getActivityTypeCount()).isEqualTo(0);
        assertThat(result.getReadingPracticeScore()).isEqualTo(0.0);
        assertThat(result.getCompletedStageCount()).isEqualTo(0);
        assertThat(result.getStageCompletionRate()).isEqualTo(0.0);
        assertThat(result.getInspectedItemCount()).isEqualTo(0);
        assertThat(result.getContentSuitabilityScore()).isEqualTo(0.0);
        assertThat(result.getRecordCompletionScore()).isEqualTo(0.0);
        assertThat(result.getOverallAchievementScore()).isEqualTo(0.0);
        assertThat(result.getAchievementLevel()).isEqualTo(IndividualAchievementLevel.NEED_SUPPORT);
        assertThat(result.getAchievementLevel().getLabel()).isEqualTo("집중 지원");
        assertThat(result.isWroteQuestionActivity()).isFalse();
        assertThat(result.getBookChatPostCount()).isEqualTo(0);
        assertThat(result.getLatestActivityDate()).isNull();
    }

    /* 교사 대시보드가 재사용하는 값: 최근 활동일은 여러 활동 날짜 중 가장 최신 날짜다 */
    @Test
    void calculate_latestActivityDate_isMaxOfAllActivityDates() {
        ReadingRecord record = buildRecord(true, false, false);
        LocalDate earlier = LocalDate.of(2026, 6, 1);
        LocalDate latest = LocalDate.of(2026, 6, 5);
        stubEmptyDataExcept(record);
        when(readingProgressLogRepository.findByReadingRecord_Id(READING_RECORD_ID))
            .thenReturn(List.of(progressLog(earlier)));
        when(bookRecommendationRepository.findByReadingRecord_Id(READING_RECORD_ID))
            .thenReturn(List.of(recommendation(latest.atStartOfDay())));

        IndividualAchievementResult result = service.calculate(READING_RECORD_ID);

        assertThat(result.getLatestActivityDate()).isEqualTo(latest);
    }

    /* 교사 대시보드가 재사용하는 값: 책수다방 글 수, 질문 활동 여부 */
    @Test
    void calculate_bookChatPostCountAndQuestionActivity_reflectRawData() {
        ReadingRecord record = buildRecord(true, false, false);
        LocalDate day = LocalDate.of(2026, 6, 1);
        stubEmptyDataExcept(record);
        when(responseRepository.findByReadingRecord_IdAndModeAndDeletedAtIsNullOrderByIdAsc(
            eq(READING_RECORD_ID), eq("individual")))
            .thenReturn(List.of(
                answerResponse("before", null, day.atStartOfDay()),
                chatPostResponse(day, day.atStartOfDay()),
                chatPostResponse(day, day.atStartOfDay())));

        IndividualAchievementResult result = service.calculate(READING_RECORD_ID);

        assertThat(result.isWroteQuestionActivity()).isTrue();
        assertThat(result.getBookChatPostCount()).isEqualTo(2);
    }

    /* 검증 25: 존재하지 않는 readingRecordId */
    @Test
    void calculate_nonExistentReadingRecordId_throwsNotFound() {
        when(readingRecordRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.calculate(999L))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("999");
    }

    /* 완독 시점 고정값 우선 사용: finalRecordCompletionScore가 있으면 그 값을 그대로 쓴다 */
    @Test
    void calculate_finishedRecordWithStoredFinalScore_usesStoredValue() {
        ReadingRecord record = buildRecord(true, true, true);
        record.setFinishedAt(LocalDateTime.of(2026, 6, 10, 12, 0));
        record.setFinalRecordCompletionScore(77);
        stubEmptyDataExcept(record);

        IndividualAchievementResult result = service.calculate(READING_RECORD_ID);

        // 실시간 계산이었다면 stageCompletionRate(100) * 0.5 + contentSuitability(0) * 0.5 = 50이 되었을 것이다.
        assertThat(result.getRecordCompletionScore()).isEqualTo(77.0);
    }

    @Test
    void calculate_finishedRecordWithStoredReadingPracticeScore_usesStoredValue() {
        ReadingRecord record = buildRecord(false, false, false);
        record.setFinishedAt(LocalDateTime.of(2026, 6, 10, 12, 0));
        record.setFinalReadingPracticeScore(83);
        stubEmptyDataExcept(record);

        IndividualAchievementResult result = service.calculate(READING_RECORD_ID);

        // 실시간 계산이었다면 활동 데이터가 없어 0점이지만, 완독 저장값을 우선 사용한다.
        assertThat(result.getReadingPracticeScore()).isEqualTo(83.0);
    }

    @Test
    void calculate_finishedRecordWithNullFinalScores_fallsBackToLiveCalculation() {
        ReadingRecord record = buildRecord(true, true, true);
        record.setFinishedAt(LocalDateTime.of(2026, 6, 10, 12, 0));
        stubEmptyDataExcept(record);

        IndividualAchievementResult result = service.calculate(READING_RECORD_ID);

        assertThat(result.getReadingPracticeScore()).isEqualTo(0.0);
        assertThat(result.getRecordCompletionScore()).isEqualTo(50.0);
    }

    /*
     * calculateLiveReadingPracticeScore(): 완독 후 친구 추천 작성 직후 전용
     * 강제 재계산. calculate()와 달리 저장된 finalReadingPracticeScore가
     * 있어도 절대 쓰지 않고, 항상 원본 활동 데이터로만 계산한다 - 이것이
     * 이번 수정의 핵심이다(완독 후 추천 작성 시 43점에 고정되던 버그).
     */
    @Test
    void calculateLiveReadingPracticeScore_ignoresStoredFinalScoreEvenWhenFinished() {
        ReadingRecord record = buildRecord(true, true, true);
        record.setFinishedAt(LocalDateTime.of(2026, 6, 10, 12, 0));
        record.setFinalReadingPracticeScore(43);
        LocalDate day = LocalDate.of(2026, 8, 2);
        stubEmptyDataExcept(record);
        when(responseRepository.findByReadingRecord_IdAndModeAndDeletedAtIsNullOrderByIdAsc(
            eq(READING_RECORD_ID), eq("individual")))
            .thenReturn(List.of(
                answerResponse("before", null, day.atStartOfDay()),
                answerResponse("during", day, day.atStartOfDay()),
                answerResponse("after", null, day.atStartOfDay()),
                chatPostResponse(day, day.atStartOfDay())));
        // 방금 저장된 친구 추천 - activityTypeCount가 4 -> 5로 늘어난다.
        when(bookRecommendationRepository.findByReadingRecord_Id(READING_RECORD_ID))
            .thenReturn(List.of(recommendation(day.atStartOfDay())));

        double live = service.calculateLiveReadingPracticeScore(READING_RECORD_ID);

        // readingDays=1 -> 3.33, activityTypeCount=5(전부) -> 50 => 53.33. 저장된 43과 다르다.
        assertThat(live).isEqualTo(53.33);
    }

    /* 검증 6: 같은 날짜라도 활동 종류가 늘면(친구 추천 신규) 점수가 오른다 */
    @Test
    void calculateLiveReadingPracticeScore_sameDayButNewActivityType_increasesScore() {
        ReadingRecord recordBefore = buildRecord(true, true, true);
        recordBefore.setFinishedAt(LocalDateTime.of(2026, 6, 10, 12, 0));
        LocalDate day = LocalDate.of(2026, 8, 2);

        // 추천 전: 4종(전/중/후/책수다방)만 있음
        when(readingRecordRepository.findById(READING_RECORD_ID)).thenReturn(java.util.Optional.of(recordBefore));
        when(responseRepository.findByReadingRecord_IdAndModeAndDeletedAtIsNullOrderByIdAsc(
            eq(READING_RECORD_ID), eq("individual")))
            .thenReturn(List.of(
                answerResponse("before", null, day.atStartOfDay()),
                answerResponse("during", day, day.atStartOfDay()),
                answerResponse("after", null, day.atStartOfDay()),
                chatPostResponse(day, day.atStartOfDay())));
        when(readingProgressLogRepository.findByReadingRecord_Id(READING_RECORD_ID)).thenReturn(List.of());
        when(summaryRepository.findByStudent_IdAndReadingRecord_Id(STUDENT_ID, READING_RECORD_ID))
            .thenReturn(java.util.Optional.empty());
        when(bookRecommendationRepository.findByReadingRecord_Id(READING_RECORD_ID)).thenReturn(List.of());

        double beforeRecommendation = service.calculateLiveReadingPracticeScore(READING_RECORD_ID);
        // readingDays=1 -> 3.33, activityTypeCount=4 -> 40 => 43.33
        assertThat(beforeRecommendation).isEqualTo(43.33);

        // 추천 후: 같은 날짜에 추천 1건이 추가됨 (readingDays는 그대로 1일)
        when(bookRecommendationRepository.findByReadingRecord_Id(READING_RECORD_ID))
            .thenReturn(List.of(recommendation(day.atStartOfDay())));

        double afterRecommendation = service.calculateLiveReadingPracticeScore(READING_RECORD_ID);
        // activityTypeCount=5 -> 50 => 53.33
        assertThat(afterRecommendation).isEqualTo(53.33);
        assertThat(afterRecommendation).isGreaterThan(beforeRecommendation);
    }

    /* 검증 5: 추천 작성일이 새로운 날짜면 독서일수도 함께 증가한다 */
    @Test
    void calculateLiveReadingPracticeScore_newActivityDate_increasesReadingDaysToo() {
        ReadingRecord record = buildRecord(true, true, true);
        record.setFinishedAt(LocalDateTime.of(2026, 6, 10, 12, 0));
        LocalDate oldDay = LocalDate.of(2026, 6, 10);
        LocalDate newDay = LocalDate.of(2026, 8, 2);
        stubEmptyDataExcept(record);
        when(responseRepository.findByReadingRecord_IdAndModeAndDeletedAtIsNullOrderByIdAsc(
            eq(READING_RECORD_ID), eq("individual")))
            .thenReturn(List.of(answerResponse("before", null, oldDay.atStartOfDay())));
        when(bookRecommendationRepository.findByReadingRecord_Id(READING_RECORD_ID))
            .thenReturn(List.of(recommendation(newDay.atStartOfDay())));

        double live = service.calculateLiveReadingPracticeScore(READING_RECORD_ID);

        // readingDays=2(oldDay+newDay) -> 2/15*50=6.67, activityTypeCount=2(전+추천) -> 20 => 26.67
        assertThat(live).isEqualTo(26.67);
    }

    /* 검증 7: 이미 그 활동 종류가 포함돼 있었다면(예: 이미 추천 있음) 재계산 값이 동일할 수 있다 */
    @Test
    void calculateLiveReadingPracticeScore_recommendationTypeAlreadyCounted_scoreUnchanged() {
        ReadingRecord record = buildRecord(true, true, true);
        record.setFinishedAt(LocalDateTime.of(2026, 6, 10, 12, 0));
        LocalDate day = LocalDate.of(2026, 8, 2);
        stubEmptyDataExcept(record);
        when(bookRecommendationRepository.findByReadingRecord_Id(READING_RECORD_ID))
            .thenReturn(List.of(recommendation(day.atStartOfDay()), recommendation(day.atStartOfDay())));

        double live = service.calculateLiveReadingPracticeScore(READING_RECORD_ID);

        // 추천 글이 몇 건이든 활동 "종류"는 1종으로만 세므로 결과는 동일하다.
        assertThat(live).isEqualTo(calculator.round2(
            calculator.readingPracticeScore(calculator.readingDaysScore(1), calculator.activityTypeScore(1))));
    }

    /* 검증 25 대응: 존재하지 않는 readingRecordId */
    @Test
    void calculateLiveReadingPracticeScore_nonExistentReadingRecordId_throwsNotFound() {
        assertThatThrownBy(() -> service.calculateLiveReadingPracticeScore(999L))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("999");
    }

    /* 검증 12: 일반 calculate()는 이 새 메서드와 무관하게 여전히 저장값 우선 정책을 유지한다 */
    @Test
    void calculate_stillPrefersStoredFinalScore_unaffectedByLiveRecalculationMethod() {
        ReadingRecord record = buildRecord(true, true, true);
        record.setFinishedAt(LocalDateTime.of(2026, 6, 10, 12, 0));
        record.setFinalReadingPracticeScore(43);
        LocalDate day = LocalDate.of(2026, 8, 2);
        stubEmptyDataExcept(record);
        when(bookRecommendationRepository.findByReadingRecord_Id(READING_RECORD_ID))
            .thenReturn(List.of(recommendation(day.atStartOfDay())));

        IndividualAchievementResult result = service.calculate(READING_RECORD_ID);

        // 원본 데이터로는 더 높은 점수가 나올 수 있지만, calculate()는 여전히 저장된 43을 반환해야 한다.
        assertThat(result.getReadingPracticeScore()).isEqualTo(43.0);
    }

    /* 검증 28: activityDate가 없는 응답은 createdAt 날짜로 대체하고, UTC로 잘리지 않는다 */
    @Test
    void calculate_missingActivityDate_fallsBackToCreatedAtDateWithoutShifting() {
        ReadingRecord record = buildRecord(false, false, false);
        LocalDateTime lateNight = LocalDateTime.of(2026, 8, 2, 23, 59);
        stubEmptyDataExcept(record);
        when(responseRepository.findByReadingRecord_IdAndModeAndDeletedAtIsNullOrderByIdAsc(
            eq(READING_RECORD_ID), eq("individual")))
            .thenReturn(List.of(answerResponse("before", null, lateNight)));

        IndividualAchievementResult result = service.calculate(READING_RECORD_ID);

        assertThat(result.getReadingDays()).isEqualTo(1);
    }
}
