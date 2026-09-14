package com.victory.dto;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ReadingCompetencyScoreTest {

    @Test
    void of_clampsAboveHundred() {
        ReadingCompetencyScore score = ReadingCompetencyScore.of(150.0);
        assertThat(score.score()).isEqualTo(100);
        assertThat(score.level()).isEqualTo("매우 우수");
    }

    @Test
    void of_clampsBelowZero() {
        ReadingCompetencyScore score = ReadingCompetencyScore.of(-30.0);
        assertThat(score.score()).isZero();
        assertThat(score.level()).isEqualTo("노력 필요");
    }

    @Test
    void of_roundsToNearestInt() {
        assertThat(ReadingCompetencyScore.of(72.4).score()).isEqualTo(72);
        assertThat(ReadingCompetencyScore.of(72.5).score()).isEqualTo(73);
    }

    @Test
    void of_attachesMatchingLevelLabel() {
        assertThat(ReadingCompetencyScore.of(72).level()).isEqualTo("우수");
        assertThat(ReadingCompetencyScore.of(48).level()).isEqualTo("보통");
    }
}
