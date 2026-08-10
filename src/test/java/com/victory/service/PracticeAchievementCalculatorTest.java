package com.victory.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class PracticeAchievementCalculatorTest {

    private final PracticeAchievementCalculator calculator = new PracticeAchievementCalculator();

    /* 검증 1: 전체쪽수 0일 때 진도 0 */
    @Test
    void pageProgress_zeroWhenTotalPagesIsZero() {
        assertThat(calculator.pageProgress(50, 0)).isEqualTo(0.0);
    }

    @Test
    void pageProgress_zeroWhenTotalPagesIsNull() {
        assertThat(calculator.pageProgress(50, null)).isEqualTo(0.0);
    }

    /* 검증 2: 누적쪽수가 전체쪽수보다 클 때 100 제한 */
    @Test
    void pageProgress_cappedAt100WhenCurrentExceedsTotal() {
        assertThat(calculator.pageProgress(150, 100)).isEqualTo(100.0);
    }

    @Test
    void pageProgress_normalRatio() {
        assertThat(calculator.pageProgress(50, 100)).isEqualTo(50.0);
    }

    @Test
    void reviewScore_100WhenReviewTrue() {
        assertThat(calculator.reviewScore(Map.of("review", true))).isEqualTo(100.0);
    }

    @Test
    void reviewScore_0WhenReviewFalseOrMissing() {
        assertThat(calculator.reviewScore(Map.of("review", false))).isEqualTo(0.0);
        assertThat(calculator.reviewScore(Map.of())).isEqualTo(0.0);
        assertThat(calculator.reviewScore(null)).isEqualTo(0.0);
    }

    @Test
    void finalReadingProgress_weightedFormula() {
        // review 100 * 0.2 + page 50 * 0.8 = 20 + 40 = 60
        assertThat(calculator.finalReadingProgress(100.0, 50.0)).isEqualTo(60.0);
    }

    /* 검증 3: 질문 작성 단계 0/1/2/3개 참여율 */
    @Test
    void questionParticipationRate_zeroStages() {
        assertThat(calculator.questionParticipationRate(false, false, false)).isEqualTo(0.0);
    }

    @Test
    void questionParticipationRate_oneStage() {
        assertThat(calculator.questionParticipationRate(true, false, false))
            .isCloseTo(33.333, org.assertj.core.data.Offset.offset(0.01));
    }

    @Test
    void questionParticipationRate_twoStages() {
        assertThat(calculator.questionParticipationRate(true, true, false))
            .isCloseTo(66.667, org.assertj.core.data.Offset.offset(0.01));
    }

    @Test
    void questionParticipationRate_threeStages() {
        assertThat(calculator.questionParticipationRate(true, true, true)).isEqualTo(100.0);
    }

    /* 검증 4: 책수다방 글 0/1/3/5개 구간 */
    @Test
    void thoughtSharingParticipationRate_zeroPosts() {
        assertThat(calculator.thoughtSharingParticipationRate(0)).isEqualTo(0.0);
    }

    @Test
    void thoughtSharingParticipationRate_onePost() {
        assertThat(calculator.thoughtSharingParticipationRate(1)).isEqualTo(20.0);
    }

    @Test
    void thoughtSharingParticipationRate_twoPosts() {
        assertThat(calculator.thoughtSharingParticipationRate(2)).isEqualTo(20.0);
    }

    @Test
    void thoughtSharingParticipationRate_threePosts() {
        assertThat(calculator.thoughtSharingParticipationRate(3)).isEqualTo(60.0);
    }

    @Test
    void thoughtSharingParticipationRate_fourPosts() {
        assertThat(calculator.thoughtSharingParticipationRate(4)).isEqualTo(60.0);
    }

    @Test
    void thoughtSharingParticipationRate_fivePosts() {
        assertThat(calculator.thoughtSharingParticipationRate(5)).isEqualTo(100.0);
    }

    @Test
    void thoughtSharingParticipationRate_moreThanFivePosts() {
        assertThat(calculator.thoughtSharingParticipationRate(9)).isEqualTo(100.0);
    }

    @Test
    void participationRate_weightedFormula() {
        assertThat(calculator.participationRate(30.0, 60.0, 90.0)).isEqualTo(57.0);
    }

    /* 검증 5: AI 1회 good 성공 */
    @Test
    void passedWithinAttemptLimit_firstAttemptGood() {
        assertThat(calculator.passedWithinAttemptLimit(List.of("good"))).isTrue();
    }

    /* 검증 6: AI 2회 good 성공 */
    @Test
    void passedWithinAttemptLimit_secondAttemptGood() {
        assertThat(calculator.passedWithinAttemptLimit(List.of("need", "good"))).isTrue();
    }

    /* 검증 7: AI 3회 good 성공 */
    @Test
    void passedWithinAttemptLimit_thirdAttemptGood() {
        assertThat(calculator.passedWithinAttemptLimit(List.of("need", "need", "good"))).isTrue();
    }

    /* 검증 8: AI 4회 good은 이해도 실패 */
    @Test
    void passedWithinAttemptLimit_fourthAttemptGoodDoesNotCount() {
        assertThat(calculator.passedWithinAttemptLimit(List.of("need", "need", "need", "good")))
            .isFalse();
    }

    @Test
    void passedWithinAttemptLimit_neverGood() {
        assertThat(calculator.passedWithinAttemptLimit(List.of("need", "need", "need"))).isFalse();
    }

    @Test
    void passedWithinAttemptLimit_emptyListIsFalse() {
        assertThat(calculator.passedWithinAttemptLimit(List.of())).isFalse();
        assertThat(calculator.passedWithinAttemptLimit(null)).isFalse();
    }

    /* 검증 9: AI 정상 평가 0건이면 comprehensionRate 0 (hasAiEvaluation은 서비스 계층 책임) */
    @Test
    void comprehensionRate_zeroWhenNoEvaluatedQuestions() {
        assertThat(calculator.comprehensionRate(0, 0)).isEqualTo(0.0);
    }

    @Test
    void comprehensionRate_ratio() {
        // 2/3 * 100
        assertThat(calculator.comprehensionRate(2, 3))
            .isCloseTo(66.667, org.assertj.core.data.Offset.offset(0.01));
    }

    @Test
    void achievementRate_weightedAverage() {
        assertThat(calculator.achievementRate(80.0, 60.0)).isEqualTo(70.0);
    }

    @Test
    void round2_roundsToTwoDecimalPlaces() {
        assertThat(calculator.round2(33.33333)).isEqualTo(33.33);
        assertThat(calculator.round2(66.6666667)).isEqualTo(66.67);
    }
}
