package com.victory.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.victory.dto.IndividualAchievementLevel;

class IndividualAchievementCalculatorTest {

    private final IndividualAchievementCalculator calculator = new IndividualAchievementCalculator();

    /* 검증 3: 15일 활동 → 독서일수점수 50점 */
    @Test
    void readingDaysScore_15Days_Returns50() {
        assertThat(calculator.readingDaysScore(15)).isEqualTo(50.0);
    }

    /* 검증 4: 20일 활동 → 50점 상한 유지 */
    @Test
    void readingDaysScore_20Days_CappedAt50() {
        assertThat(calculator.readingDaysScore(20)).isEqualTo(50.0);
    }

    @Test
    void readingDaysScore_zeroDays_ReturnsZero() {
        assertThat(calculator.readingDaysScore(0)).isEqualTo(0.0);
    }

    @Test
    void readingDaysScore_partialDays_LinearScale() {
        // 3일 / 15 * 50 = 10
        assertThat(calculator.readingDaysScore(3)).isEqualTo(10.0);
    }

    /* 검증 5: 읽기 전만 참여 → 1종, 10점 */
    @Test
    void activityTypeScore_oneType_Returns10() {
        assertThat(calculator.activityTypeScore(1)).isEqualTo(10.0);
    }

    /* 검증 6: 5종 모두 참여 → 5종, 50점 */
    @Test
    void activityTypeScore_allFiveTypes_Returns50() {
        assertThat(calculator.activityTypeScore(5)).isEqualTo(50.0);
    }

    @Test
    void readingPracticeScore_sumsBothParts() {
        assertThat(calculator.readingPracticeScore(50.0, 30.0)).isEqualTo(80.0);
    }

    @Test
    void readingPracticeScore_cappedAt100() {
        assertThat(calculator.readingPracticeScore(90.0, 90.0)).isEqualTo(100.0);
    }

    /* 검증 8: 읽기 전만 완료 → 1/3 */
    @Test
    void stageCompletionRate_oneOfThree() {
        assertThat(calculator.stageCompletionRate(1)).isCloseTo(33.33, org.assertj.core.data.Offset.offset(0.01));
    }

    /* 검증 9: 읽기 전·중 완료 → 2/3 */
    @Test
    void stageCompletionRate_twoOfThree() {
        assertThat(calculator.stageCompletionRate(2)).isCloseTo(66.67, org.assertj.core.data.Offset.offset(0.01));
    }

    /* 검증 10: 읽기 전·중·후 완료 → 100% */
    @Test
    void stageCompletionRate_allThree_Returns100() {
        assertThat(calculator.stageCompletionRate(3)).isEqualTo(100.0);
    }

    @Test
    void stageCompletionRate_zero() {
        assertThat(calculator.stageCompletionRate(0)).isEqualTo(0.0);
    }

    /* 기록충실도: 핵심 기록 5개 모두 충족 → 100점. AI 통과 여부는 전혀 입력받지 않는다. */
    @Test
    void recordFaithfulnessScore_allFiveFulfilled_Returns100() {
        assertThat(calculator.recordFaithfulnessScore(true, true, true, true, true)).isEqualTo(100.0);
    }

    /* 기록충실도: 핵심 기록 5개 중 4개 충족 → 80점 */
    @Test
    void recordFaithfulnessScore_fourOfFive_Returns80() {
        assertThat(calculator.recordFaithfulnessScore(true, true, true, true, false)).isEqualTo(80.0);
    }

    /* 기록충실도: 아무 기록도 없음 → 0점 */
    @Test
    void recordFaithfulnessScore_none_ReturnsZero() {
        assertThat(calculator.recordFaithfulnessScore(false, false, false, false, false)).isEqualTo(0.0);
    }

    @Test
    void recordCompletionScore_weightedAverage() {
        assertThat(calculator.recordCompletionScore(100.0, 0.0)).isEqualTo(50.0);
        assertThat(calculator.recordCompletionScore(0.0, 100.0)).isEqualTo(50.0);
    }

    /* 검증 19: 독서실천도 80, 기록완성도 60 → 종합달성도 70, 우수 */
    @Test
    void overallAchievementScore_averageOfTwoParts() {
        double overall = calculator.overallAchievementScore(80.0, 60.0);
        assertThat(overall).isEqualTo(70.0);

        int rounded = calculator.roundedOverallAchievementScore(overall);
        assertThat(rounded).isEqualTo(70);
        assertThat(calculator.achievementLevel(rounded)).isEqualTo(IndividualAchievementLevel.GOOD);
        assertThat(calculator.achievementLevel(rounded).getLabel()).isEqualTo("우수");
    }

    /* 검증 20: 종합달성도 84.4 → 반올림 84, 우수 */
    @Test
    void roundedOverallAchievementScore_84_4_RoundsDownTo84() {
        int rounded = calculator.roundedOverallAchievementScore(84.4);
        assertThat(rounded).isEqualTo(84);
        assertThat(calculator.achievementLevel(rounded)).isEqualTo(IndividualAchievementLevel.GOOD);
    }

    /*
     * 검증 21: 종합달성도 84.5 이상 → 반올림 85, 매우 우수.
     * Java Math.round(double)는 0.5를 항상 위로 올린다(HALF_UP과 동일한
     * 결과, 음수가 아닌 값 기준) - 84.5 → 85로 명확히 검증한다.
     */
    @Test
    void roundedOverallAchievementScore_84_5_RoundsUpTo85() {
        int rounded = calculator.roundedOverallAchievementScore(84.5);
        assertThat(rounded).isEqualTo(85);
        assertThat(calculator.achievementLevel(rounded)).isEqualTo(IndividualAchievementLevel.VERY_GOOD);
        assertThat(calculator.achievementLevel(rounded).getLabel()).isEqualTo("매우 우수");
    }

    @Test
    void achievementLevel_boundaries() {
        assertThat(calculator.achievementLevel(100)).isEqualTo(IndividualAchievementLevel.VERY_GOOD);
        assertThat(calculator.achievementLevel(85)).isEqualTo(IndividualAchievementLevel.VERY_GOOD);
        assertThat(calculator.achievementLevel(84)).isEqualTo(IndividualAchievementLevel.GOOD);
        assertThat(calculator.achievementLevel(70)).isEqualTo(IndividualAchievementLevel.GOOD);
        assertThat(calculator.achievementLevel(69)).isEqualTo(IndividualAchievementLevel.NORMAL);
        assertThat(calculator.achievementLevel(50)).isEqualTo(IndividualAchievementLevel.NORMAL);
        assertThat(calculator.achievementLevel(49)).isEqualTo(IndividualAchievementLevel.NEED_SUPPORT);
        assertThat(calculator.achievementLevel(0)).isEqualTo(IndividualAchievementLevel.NEED_SUPPORT);
    }

    /* 검증 26: 점수는 0~100 범위를 벗어나지 않는다 */
    @Test
    void scores_neverExceedUpperOrLowerBound() {
        assertThat(calculator.readingDaysScore(1000)).isEqualTo(50.0);
        assertThat(calculator.activityTypeScore(1000)).isEqualTo(50.0);
        assertThat(calculator.readingPracticeScore(50.0, 50.0)).isEqualTo(100.0);
        assertThat(calculator.recordFaithfulnessScore(true, true, true, true, true)).isEqualTo(100.0);
        assertThat(calculator.recordCompletionScore(100.0, 100.0)).isEqualTo(100.0);
        assertThat(calculator.overallAchievementScore(100.0, 100.0)).isEqualTo(100.0);
        assertThat(calculator.roundedOverallAchievementScore(150.0)).isEqualTo(100);
        assertThat(calculator.readingDaysScore(-5)).isEqualTo(0.0);
    }
}
