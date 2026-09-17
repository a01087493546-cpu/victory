package com.victory.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.stereotype.Component;

import com.victory.dto.IndividualAchievementLevel;

/*
 * 개별읽기 지표(교사용 평가) 계산 공식을 모아 둔 순수 계산기. DB/네트워크에
 * 의존하지 않아 단위 테스트로 공식만 독립적으로 검증할 수 있다
 * (PracticeAchievementCalculator와 같은 패턴). 계산 과정은 모두 double로
 * 하고, 화면에 보여줄 자릿수 반올림은 이 클래스의 round2()에서만 한다.
 *
 * 개별읽기 AI 피드백("확인받기")은 학생이 선택적으로 쓰는 도움 기능으로
 * 분리되었으므로, 이 계산기는 AI 통과 여부/시도 횟수를 절대 입력으로 받지
 * 않는다(과거에는 contentSuitabilityScore가 "3회 이내 AI 통과 비율"이었다 -
 * 연습읽기 전용 PracticeAchievementCalculator는 그 방식을 그대로 유지하며,
 * 이 클래스와는 별개다).
 */
@Component
public class IndividualAchievementCalculator {

    private static final int ACTIVITY_TYPE_TOTAL = 5;
    private static final int READING_DAYS_TARGET = 15;
    private static final int STAGE_TOTAL = 3;
    private static final int CORE_RECORD_TOTAL = 5;

    /*
     * 독서일수점수 = MIN(50, 독서일수 / 15 × 50)
     */
    public double readingDaysScore(int readingDays) {
        int days = Math.max(0, readingDays);
        double raw = (days / (double) READING_DAYS_TARGET) * 50.0;

        return clamp(Math.min(50.0, raw));
    }

    /*
     * 활동참여점수 = MIN(50, 참여한 활동 종류 수 / 5 × 50)
     */
    public double activityTypeScore(int activityTypeCount) {
        int count = Math.max(0, Math.min(ACTIVITY_TYPE_TOTAL, activityTypeCount));
        double raw = (count / (double) ACTIVITY_TYPE_TOTAL) * 50.0;

        return clamp(Math.min(50.0, raw));
    }

    /*
     * 독서실천도 = 독서일수점수 + 활동참여점수 (최대 100)
     */
    public double readingPracticeScore(double readingDaysScore, double activityTypeScore) {
        return clamp(readingDaysScore + activityTypeScore);
    }

    /*
     * 활동완료율 = 완료한 단계 수(읽기 전/중/후) / 3 × 100
     */
    public double stageCompletionRate(int completedStageCount) {
        int count = Math.max(0, Math.min(STAGE_TOTAL, completedStageCount));

        return clamp((count / (double) STAGE_TOTAL) * 100.0);
    }

    /*
     * 기록충실도 = 충족한 핵심 기록 수 / 5 × 100. AI 판단은 전혀 쓰지 않고
     * 실제 독서 기록의 작성/참여 여부(boolean)만으로 계산한다. 핵심 기록
     * 5개(각 20점): 읽기 전 질문·답 작성, 읽기 중 질문·답 작성, 읽기 후
     * 간추리기 작성, 책수다방 참여 1회 이상, 해당 책의 필수 기록(읽기
     * 전/중/후) 전체에 미작성 항목 없음.
     */
    public double recordFaithfulnessScore(
            boolean wrotePreQuestion,
            boolean wroteDuringQuestion,
            boolean wroteAfterSummary,
            boolean joinedBookChat,
            boolean noMissingRequiredRecord) {

        int fulfilledCount = (wrotePreQuestion ? 1 : 0)
            + (wroteDuringQuestion ? 1 : 0)
            + (wroteAfterSummary ? 1 : 0)
            + (joinedBookChat ? 1 : 0)
            + (noMissingRequiredRecord ? 1 : 0);

        return clamp((fulfilledCount / (double) CORE_RECORD_TOTAL) * 100.0);
    }

    /*
     * 기록완성도 = 단계완료율 × 0.5 + 기록충실도 × 0.5
     */
    public double recordCompletionScore(double stageCompletionRate, double recordFaithfulnessScore) {
        return clamp(stageCompletionRate * 0.5 + recordFaithfulnessScore * 0.5);
    }

    /*
     * 종합달성도 = (독서실천도 + 기록완성도) / 2. 내부 계산은 소수점을 유지한다.
     */
    public double overallAchievementScore(double readingPracticeScore, double recordCompletionScore) {
        return clamp((readingPracticeScore + recordCompletionScore) / 2.0);
    }

    /*
     * 화면용 최종 반올림 정수. Java Math.round는 반올림 기준이 HALF_UP과
     * 같아(음수가 아닌 값에서) .5는 항상 올림된다 - 84.5 → 85.
     */
    public int roundedOverallAchievementScore(double overallAchievementScore) {
        long rounded = Math.round(overallAchievementScore);

        return (int) Math.max(0, Math.min(100, rounded));
    }

    /*
     * 반올림된 종합달성도 기준 등급.
     */
    public IndividualAchievementLevel achievementLevel(int roundedOverallAchievementScore) {
        return IndividualAchievementLevel.fromRoundedScore(roundedOverallAchievementScore);
    }

    /*
     * 화면에 보여줄 자릿수(소수 둘째 자리)로만 반올림한다. 계산 과정에서는
     * 절대 이 메서드를 쓰지 않고, 최종 결과 필드를 채울 때만 쓴다.
     */
    public double round2(double value) {
        return BigDecimal.valueOf(value)
            .setScale(2, RoundingMode.HALF_UP)
            .doubleValue();
    }

    private double clamp(double value) {
        return Math.max(0.0, Math.min(100.0, value));
    }
}
