package com.victory.service;

import java.time.LocalDate;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.victory.dto.IndividualAchievementLevel;
import com.victory.dto.IndividualAchievementResult;
import com.victory.entity.BookRecommendation;
import com.victory.entity.ReadingProgressLog;
import com.victory.entity.ReadingRecord;
import com.victory.entity.Response;
import com.victory.entity.Summary;
import com.victory.repository.BookRecommendationRepository;
import com.victory.repository.ReadingProgressLogRepository;
import com.victory.repository.ReadingRecordRepository;
import com.victory.repository.ResponseRepository;
import com.victory.repository.SummaryRepository;

import lombok.RequiredArgsConstructor;

/*
 * 개별읽기 지표(교사용 평가) 계산 서비스. 하나의 readingRecordId를 입력받아
 * 독서실천도/기록완성도/종합달성도/등급/전체 누적 완독 권수를 계산한다.
 *
 * 이번 단계는 계산만 다룬다 - 교사용 API/화면, 오늘 참여율, 지원 필요 학생
 * 판정, 스냅샷 저장은 다음 단계 범위다(PracticeAchievementService와 달리
 * 아직 스냅샷 테이블이 없다).
 *
 * 과거 readingRecordId가 NULL인 책수다방·추천 글은 Repository 조회
 * 조건(reading_record_id = :readingRecordId) 자체에서 걸러지므로 이
 * 서비스가 따로 필터링하지 않아도 절대 섞이지 않는다.
 */
@Service
@RequiredArgsConstructor
public class IndividualAchievementService {

    private static final String MODE_INDIVIDUAL = "individual";
    private static final String CONTENT_TYPE_ANSWER = "answer";
    private static final String CONTENT_TYPE_CHAT_POST = "chat_post";
    private static final String STAGE_BEFORE = "before";
    private static final String STAGE_DURING = "during";
    private static final String STAGE_AFTER = "after";

    private final ReadingRecordRepository readingRecordRepository;
    private final ResponseRepository responseRepository;
    private final SummaryRepository summaryRepository;
    private final ReadingProgressLogRepository readingProgressLogRepository;
    private final BookRecommendationRepository bookRecommendationRepository;
    private final IndividualAchievementCalculator calculator;

    @Transactional(readOnly = true)
    public IndividualAchievementResult calculate(Long readingRecordId) {
        ReadingRecord record = readingRecordRepository.findById(readingRecordId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "개별읽기 기록을 찾을 수 없습니다. readingRecordId=" + readingRecordId
            ));

        Long studentId = record.getStudent().getId();

        LiveReadingPracticeComputation live = computeLiveReadingPractice(readingRecordId, studentId);

        /*
         * 완독 기록은 저장된 최종 독서실천도가 있으면 그 값을 우선 사용한다.
         * 과거 데이터처럼 값이 null인 경우에는 조회가 깨지지 않도록 실시간
         * 계산값으로만 대체하고, 여기서 DB를 보정 저장하지는 않는다.
         *
         * 예외: 추천 작성 직후 강제 재계산이 필요한 경우는 이 메서드가 아니라
         * calculateLiveReadingPracticeScore()를 따로 호출한다 - 저장값 우선
         * 정책을 이 calculate()에서는 절대 건드리지 않는다.
         */
        double readingPracticeScore = (record.getFinishedAt() != null
                && record.getFinalReadingPracticeScore() != null)
            ? calculator.round2(record.getFinalReadingPracticeScore())
            : live.liveReadingPracticeScore();

        int completedStageCount = (Boolean.TRUE.equals(record.getBeforeDone()) ? 1 : 0)
            + (Boolean.TRUE.equals(record.getDuringDone()) ? 1 : 0)
            + (Boolean.TRUE.equals(record.getAfterDone()) ? 1 : 0);
        double stageCompletionRate = calculator.round2(calculator.stageCompletionRate(completedStageCount));

        /*
         * 기록충실도 - AI 확인 여부/통과 여부는 절대 쓰지 않는다. 핵심 기록
         * 5개 중 앞의 3개(읽기 전/중/후 질문·답 또는 간추리기 작성 여부)는
         * computeLiveReadingPractice()가 이미 계산한 boolean을 그대로
         * 재사용하고, 나머지 2개(책수다방 참여, 필수 기록 전체 완료)는 여기서
         * 판단한다.
         */
        boolean noMissingRequiredRecord = Boolean.TRUE.equals(record.getBeforeDone())
            && Boolean.TRUE.equals(record.getDuringDone())
            && Boolean.TRUE.equals(record.getAfterDone());

        double recordFaithfulnessScore = calculator.round2(
            calculator.recordFaithfulnessScore(
                live.wrotePreQuestion(),
                live.wroteDuringQuestion(),
                live.wroteAfterSummary(),
                live.bookChatPostCount() > 0,
                noMissingRequiredRecord));

        /*
         * 완독 기록은 final_record_completion_score가 이미 저장되어 있으면
         * 그 값을 우선 사용한다(완독 당시 값 고정 정책). 과거 데이터처럼
         * 값이 null인 경우에는 실시간 계산값으로만 대체한다.
         */
        double recordCompletionScore = (record.getFinishedAt() != null
                && record.getFinalRecordCompletionScore() != null)
            ? calculator.round2(record.getFinalRecordCompletionScore())
            : calculator.round2(
                calculator.recordCompletionScore(stageCompletionRate, recordFaithfulnessScore));

        double overallAchievementScore = calculator.round2(
            calculator.overallAchievementScore(readingPracticeScore, recordCompletionScore));
        int roundedOverallAchievementScore = calculator.roundedOverallAchievementScore(overallAchievementScore);
        IndividualAchievementLevel achievementLevel =
            calculator.achievementLevel(roundedOverallAchievementScore);

        long totalCompletedBookCount = readingRecordRepository
            .countByStudent_IdAndFinishedAtIsNotNull(studentId);

        return new IndividualAchievementResult(
            studentId,
            readingRecordId,
            live.readingDays(),
            live.readingDaysScore(),
            live.activityTypeCount(),
            live.activityTypeScore(),
            readingPracticeScore,
            completedStageCount,
            stageCompletionRate,
            recordFaithfulnessScore,
            recordCompletionScore,
            overallAchievementScore,
            roundedOverallAchievementScore,
            achievementLevel,
            totalCompletedBookCount,
            live.wroteQuestionActivity(),
            live.bookChatPostCount(),
            live.latestActivityDate()
        );
    }

    /*
     * 완독 후 친구 추천 글을 새로 저장한 직후 전용. calculate()와 달리
     * 저장된 final_reading_practice_score를 절대 쓰지 않고, 방금 저장된
     * 추천까지 포함한 원본 활동 데이터만으로 독서실천도를 강제로 다시
     * 계산한다(BookRecommendationService가 이 값을 받아 final 점수를
     * 새로 저장한다). 기록완성도·종합달성도는 이 시점에 필요 없으므로
     * 계산하지 않는다 - calculate()의 "저장값 우선" 일반 조회 정책은
     * 이 메서드로 완전히 분리되어 전혀 영향받지 않는다.
     */
    @Transactional(readOnly = true)
    public double calculateLiveReadingPracticeScore(Long readingRecordId) {
        ReadingRecord record = readingRecordRepository.findById(readingRecordId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "개별읽기 기록을 찾을 수 없습니다. readingRecordId=" + readingRecordId
            ));

        Long studentId = record.getStudent().getId();

        return computeLiveReadingPractice(readingRecordId, studentId).liveReadingPracticeScore();
    }

    /*
     * 독서실천도 계산에 필요한 원본 활동 데이터(페이지 기록/읽기 전·중·후
     * 질문 또는 간추리기/책수다방/친구 추천)를 모아 독서일수·활동 종류·
     * 실시간 독서실천도를 계산한다. calculate()와
     * calculateLiveReadingPracticeScore() 둘 다 이 메서드 하나만 쓴다 -
     * 같은 계산을 두 곳에서 따로 구현하지 않는다.
     */
    private LiveReadingPracticeComputation computeLiveReadingPractice(Long readingRecordId, Long studentId) {
        List<Response> responses = responseRepository
            .findByReadingRecord_IdAndModeAndDeletedAtIsNullOrderByIdAsc(readingRecordId, MODE_INDIVIDUAL);
        List<ReadingProgressLog> progressLogs = readingProgressLogRepository
            .findByReadingRecord_Id(readingRecordId);
        Optional<Summary> summary = summaryRepository
            .findByStudent_IdAndReadingRecord_Id(studentId, readingRecordId);
        List<BookRecommendation> recommendations = bookRecommendationRepository
            .findByReadingRecord_Id(readingRecordId);

        Set<LocalDate> activeDates = new HashSet<>();

        for (ReadingProgressLog log : progressLogs) {
            activeDates.add(log.getLogDate());
        }

        boolean wrotePreQuestion = false;
        boolean wroteDuringQuestion = false;
        boolean wroteAfterQuestionOrSummary = summary.isPresent();
        int bookChatPostCount = 0;

        for (Response response : responses) {
            activeDates.add(activityDateOf(response));

            if (CONTENT_TYPE_ANSWER.equals(response.getContentType())) {
                String stage = response.getStage();

                if (STAGE_BEFORE.equals(stage)) {
                    wrotePreQuestion = true;
                } else if (STAGE_DURING.equals(stage)) {
                    wroteDuringQuestion = true;
                } else if (STAGE_AFTER.equals(stage)) {
                    wroteAfterQuestionOrSummary = true;
                }
            } else if (CONTENT_TYPE_CHAT_POST.equals(response.getContentType())) {
                bookChatPostCount++;
            }
        }

        boolean wroteChatPost = bookChatPostCount > 0;

        summary.ifPresent(s -> activeDates.add(s.getCreatedAt().toLocalDate()));

        boolean wroteRecommendation = !recommendations.isEmpty();

        for (BookRecommendation recommendation : recommendations) {
            activeDates.add(recommendation.getCreatedAt().toLocalDate());
        }

        int readingDays = activeDates.size();

        int activityTypeCount = (wrotePreQuestion ? 1 : 0)
            + (wroteDuringQuestion ? 1 : 0)
            + (wroteAfterQuestionOrSummary ? 1 : 0)
            + (wroteChatPost ? 1 : 0)
            + (wroteRecommendation ? 1 : 0);

        double readingDaysScore = calculator.round2(calculator.readingDaysScore(readingDays));
        double activityTypeScore = calculator.round2(calculator.activityTypeScore(activityTypeCount));
        double liveReadingPracticeScore = calculator.round2(
            calculator.readingPracticeScore(readingDaysScore, activityTypeScore));

        boolean wroteQuestionActivity = wrotePreQuestion || wroteDuringQuestion;
        LocalDate latestActivityDate = activeDates.isEmpty() ? null : Collections.max(activeDates);

        return new LiveReadingPracticeComputation(
            readingDays,
            readingDaysScore,
            activityTypeCount,
            activityTypeScore,
            liveReadingPracticeScore,
            wroteQuestionActivity,
            wrotePreQuestion,
            wroteDuringQuestion,
            wroteAfterQuestionOrSummary,
            bookChatPostCount,
            latestActivityDate
        );
    }

    /*
     * wroteAfterSummary는 wroteAfterQuestionOrSummary와 같은 값이다(읽기 후
     * summary 저장 또는 after 단계 답변 존재) - 기록충실도 5개 항목의
     * "읽기 후 간추리기 완료"에 이 값을 그대로 재사용한다.
     */
    private record LiveReadingPracticeComputation(
        int readingDays,
        double readingDaysScore,
        int activityTypeCount,
        double activityTypeScore,
        double liveReadingPracticeScore,
        boolean wroteQuestionActivity,
        boolean wrotePreQuestion,
        boolean wroteDuringQuestion,
        boolean wroteAfterSummary,
        int bookChatPostCount,
        LocalDate latestActivityDate
    ) {
    }

    /*
     * activity_date가 있으면 그 값을 쓰고(읽기 중처럼 날짜별로 반복되는
     * 활동, 책수다방 글), 없으면(읽기 전/읽기 후처럼 한 번만 쓰는 활동)
     * created_at의 날짜를 쓴다. created_at은 이 프로젝트 전반에서 이미
     * Asia/Seoul 기준 서버 시각으로 기록되므로(LocalDateTime.now(), 다른
     * 서비스들과 동일한 관례) 별도 시간대 변환 없이 toLocalDate()로
     * 충분하다 - UTC를 그대로 자르는 것과는 다르다.
     */
    private LocalDate activityDateOf(Response response) {
        if (response.getActivityDate() != null) {
            return response.getActivityDate();
        }

        return response.getCreatedAt().toLocalDate();
    }
}
