package com.victory.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

class IndividualPortfolioResponseTest {

    private IndividualPortfolioResponse withScores(BigDecimal readingPractice, BigDecimal recordCompletion) {
        return new IndividualPortfolioResponse(1L, "김학생", 4, 2,
            LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31),
            0, readingPractice, recordCompletion,
            new IndividualPortfolioResponse.ActivitySummary(0, 0, 0, 0),
            new MonthlyCompletionStatsResponse(2026, List.of(0,0,0,0,0,0,0,0,0,0,0,0)),
            new IndividualPortfolioResponse.Competencies(0, 0, 0, 0),
            new IndividualPortfolioResponse.ReadingCompetencies(
                ReadingCompetencyScore.of(0), ReadingCompetencyScore.of(0),
                ReadingCompetencyScore.of(0), ReadingCompetencyScore.of(0)),
            false);
    }

    @Test
    void readingPracticeLevel_39_isNeedsEffort() {
        assertThat(withScores(new BigDecimal("39"), BigDecimal.ZERO).readingPracticeLevel()).isEqualTo("노력 필요");
    }

    @Test
    void readingPracticeLevel_40_isAverage() {
        assertThat(withScores(new BigDecimal("40"), BigDecimal.ZERO).readingPracticeLevel()).isEqualTo("보통");
    }

    @Test
    void readingPracticeLevel_59_isAverage() {
        assertThat(withScores(new BigDecimal("59"), BigDecimal.ZERO).readingPracticeLevel()).isEqualTo("보통");
    }

    @Test
    void readingPracticeLevel_60_isProficient() {
        assertThat(withScores(new BigDecimal("60"), BigDecimal.ZERO).readingPracticeLevel()).isEqualTo("우수");
    }

    @Test
    void readingPracticeLevel_79_isProficient() {
        assertThat(withScores(new BigDecimal("79"), BigDecimal.ZERO).readingPracticeLevel()).isEqualTo("우수");
    }

    @Test
    void readingPracticeLevel_80_isExcellent() {
        assertThat(withScores(new BigDecimal("80"), BigDecimal.ZERO).readingPracticeLevel()).isEqualTo("매우 우수");
    }

    @Test
    void readingPracticeLevel_100_isExcellent() {
        assertThat(withScores(new BigDecimal("100"), BigDecimal.ZERO).readingPracticeLevel()).isEqualTo("매우 우수");
    }

    @Test
    void recordCompletionLevel_sameBoundariesAsReadingPractice() {
        assertThat(withScores(BigDecimal.ZERO, new BigDecimal("39")).recordCompletionLevel()).isEqualTo("노력 필요");
        assertThat(withScores(BigDecimal.ZERO, new BigDecimal("40")).recordCompletionLevel()).isEqualTo("보통");
        assertThat(withScores(BigDecimal.ZERO, new BigDecimal("60")).recordCompletionLevel()).isEqualTo("우수");
        assertThat(withScores(BigDecimal.ZERO, new BigDecimal("80")).recordCompletionLevel()).isEqualTo("매우 우수");
    }

    @Test
    void level_nullScore_isNull() {
        IndividualPortfolioResponse result = withScores(null, null);
        assertThat(result.readingPracticeLevel()).isNull();
        assertThat(result.recordCompletionLevel()).isNull();
    }

    @Test
    void level_roundsHalfUpBeforeJudging() {
        // 59.5 -> 반올림 60 -> 우수 (59 그대로였으면 보통)
        assertThat(withScores(new BigDecimal("59.5"), BigDecimal.ZERO).readingPracticeLevel()).isEqualTo("우수");
    }
}
