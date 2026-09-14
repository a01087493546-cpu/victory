package com.victory.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

class PracticePortfolioResponseTest {
    private final PracticeStageDetail emptyStage =
        new PracticeStageDetail(false, false, "아직 참여 기록 없음", "현재 기록에서는 이 단계 활동 기록이 아직 없어요.", null);

    private PracticePortfolioResponse withRates(Double participationRate, Double comprehensionRate) {
        return new PracticePortfolioResponse(1L, "김학생", 4, 2,
            LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31),
            participationRate, comprehensionRate,
            new PortfolioActivityCount(0, false), new PortfolioActivityCount(0, false), new PortfolioActivityCount(0, false),
            "책 제목", 0, emptyStage, emptyStage, emptyStage);
    }

    @Test
    void participationLevel_92_isExcellent() {
        assertThat(withRates(92d, 0d).participationLevel()).isEqualTo("매우 우수");
    }

    @Test
    void comprehensionLevel_68_isProficient() {
        assertThat(withRates(0d, 68d).comprehensionLevel()).isEqualTo("우수");
    }

    @Test
    void level_40to59_isAverage() {
        assertThat(withRates(58d, 40d).participationLevel()).isEqualTo("보통");
        assertThat(withRates(58d, 40d).comprehensionLevel()).isEqualTo("보통");
    }

    @Test
    void level_below40_needsEffort() {
        assertThat(withRates(35d, 0d).participationLevel()).isEqualTo("노력 필요");
    }

    @Test
    void level_nullRate_isNull() {
        PracticePortfolioResponse result = withRates(null, null);
        assertThat(result.participationLevel()).isNull();
        assertThat(result.comprehensionLevel()).isNull();
    }
}
