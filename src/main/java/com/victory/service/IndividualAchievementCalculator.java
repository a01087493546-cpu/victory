package com.victory.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import org.springframework.stereotype.Component;

import com.victory.dto.IndividualAchievementLevel;

/*
 * 개별읽기 지표(교사용 평가) 계산 공식을 모아 둔 순수 계산기. DB/네트워크에
 * 의존하지 않아 단위 테스트로 공식만 독립적으로 검증할 수 있다
 * (PracticeAchievementCalculator와 같은 패턴). 계산 과정은 모두 double로
 * 하고, 화면에 보여줄 자릿수 반올림은 이 클래스의 round2()에서만 한다.
 */
@Component
public class IndividualAchievementCalculator {

    private static final int ATTEMPT_SUCCESS_LIMIT = 3;
    private static final int ACTIVITY_TYPE_TOTAL = 5;
    private static final int READING_DAYS_TARGET = 15;
    private static final int STAGE_TOTAL = 3;

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
     * 기록내용적합성 = 3회 이내 good 수 / AI 검사를 1회 이상 받은 전체
     * 평가 대상 수 × 100. 검사 대상이 0개면 0점으로 계산한다(나눗셈 0 방지).
     */
    public double contentSuitabilityScore(int passedWithinThreeCount, int inspectedItemCount) {
        if (inspectedItemCount <= 0) {
            return 0.0;
        }

        double raw = (Math.max(0, passedWithinThreeCount) / (double) inspectedItemCount) * 100.0;

        return clamp(raw);
    }

    /*
     * 기록완성도 = 활동완료율 × 0.5 + 기록내용적합성 × 0.5
     */
    public double recordCompletionScore(double stageCompletionRate, double contentSuitabilityScore) {
        return clamp(stageCompletionRate * 0.5 + contentSuitabilityScore * 0.5);
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
     * 한 평가 대상(evaluationKey)의 AI 검사 시도 상태를 attempt_number
     * 오름차순으로 받아, "3회 이내에 good을 받았는지"를 판정한다.
     * 4회차 이후에만 good이 있으면 실패로 본다.
     */
    public boolean passedWithinAttemptLimit(List<String> orderedStatuses) {
        if (orderedStatuses == null || orderedStatuses.isEmpty()) {
            return false;
        }

        int limit = Math.min(orderedStatuses.size(), ATTEMPT_SUCCESS_LIMIT);

        for (int i = 0; i < limit; i++) {
            if ("good".equals(orderedStatuses.get(i))) {
                return true;
            }
        }

        return false;
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
