package com.victory.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.util.List;

import org.junit.jupiter.api.Test;

class ReadingCompetencyCalculatorTest {
    private final ReadingCompetencyCalculator calculator = new ReadingCompetencyCalculator();

    @Test
    void questionGeneration_zeroWhenNoBooksInScope() {
        assertThat(calculator.questionGenerationScore(List.of())).isZero();
        assertThat(calculator.questionGenerationScore(null)).isZero();
    }

    @Test
    void questionGeneration_zeroWhenNoStageCompletedInAnyBook() {
        assertThat(calculator.questionGenerationScore(List.of(0, 0, 0))).isZero();
    }

    @Test
    void questionGeneration_midValueWhenPartiallyCompleted() {
        // 1권, 3단계 중 2단계 완료 -> 66.67
        assertThat(calculator.questionGenerationScore(List.of(2))).isCloseTo(66.6667, within(0.01));
    }

    @Test
    void questionGeneration_hundredWhenAllBooksFullyCompleted() {
        assertThat(calculator.questionGenerationScore(List.of(3, 3))).isEqualTo(100.0);
    }

    @Test
    void questionGeneration_doesNotInflateFromRawQuestionCount() {
        // 완료 단계 수는 boolean 플래그 기준이라 3을 넘는 값이 들어와도(방어적으로) 3으로 clamp된다.
        assertThat(calculator.questionGenerationScore(List.of(5))).isEqualTo(100.0);
        // 단계를 하나만 완료한 책 2권과, 세 단계를 모두 완료한 책 1권을 비교해도
        // "책마다 완료한 단계 비율의 평균"이라 raw 활동 개수와 무관하게 결정된다.
        assertThat(calculator.questionGenerationScore(List.of(1, 1)))
            .isLessThan(calculator.questionGenerationScore(List.of(3)));
    }

    @Test
    void readingPersistence_reusesAverageReadingPracticeScoreDirectly() {
        assertThat(calculator.readingPersistenceScore(85.0)).isEqualTo(85.0);
    }

    @Test
    void readingPersistence_zeroWhenNoCompletedBooks() {
        assertThat(calculator.readingPersistenceScore(null)).isZero();
    }

    @Test
    void readingPersistence_clampsAboveHundredAndBelowZero() {
        assertThat(calculator.readingPersistenceScore(150.0)).isEqualTo(100.0);
        assertThat(calculator.readingPersistenceScore(-10.0)).isZero();
    }

    @Test
    void thoughtRefinement_reusesAverageRecordCompletionScoreDirectly() {
        assertThat(calculator.thoughtRefinementScore(63.0)).isEqualTo(63.0);
    }

    @Test
    void thoughtRefinement_zeroWhenNoCompletedBooks() {
        assertThat(calculator.thoughtRefinementScore(null)).isZero();
    }

    @Test
    void thoughtSharing_zeroWhenNoBooksInScope() {
        assertThat(calculator.thoughtSharingScore(0, 0, true)).isZero();
    }

    @Test
    void thoughtSharing_ratioOfBooksWithAttributedSharing() {
        assertThat(calculator.thoughtSharingScore(1, 2, false)).isEqualTo(50.0);
    }

    @Test
    void thoughtSharing_hundredWhenEveryBookHasSharing() {
        assertThat(calculator.thoughtSharingScore(3, 3, false)).isEqualTo(100.0);
    }

    @Test
    void thoughtSharing_unattributedReplyAddsAtMostOneBonusBook() {
        assertThat(calculator.thoughtSharingScore(0, 2, true)).isEqualTo(50.0);
    }

    @Test
    void thoughtSharing_unattributedReplyBonusNeverPushesPastFullParticipation() {
        assertThat(calculator.thoughtSharingScore(2, 2, true)).isEqualTo(100.0);
    }

    @Test
    void thoughtSharing_doesNotConvertRawReplyCountDirectlyToScore() {
        // hasUnattributedSharing은 boolean이라, 댓글을 10개 쓰든 1개 쓰든
        // 보너스는 항상 최대 1권 분으로만 반영된다(raw count 무관).
        assertThat(calculator.thoughtSharingScore(0, 5, true)).isEqualTo(20.0);
    }
}
