package com.victory.dto;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ReadingCompetencyLevelTest {

    @Test
    void boundaries_matchSpecifiedRanges() {
        assertThat(ReadingCompetencyLevel.fromScore(0).getLabel()).isEqualTo("노력 필요");
        assertThat(ReadingCompetencyLevel.fromScore(39).getLabel()).isEqualTo("노력 필요");
        assertThat(ReadingCompetencyLevel.fromScore(40).getLabel()).isEqualTo("보통");
        assertThat(ReadingCompetencyLevel.fromScore(59).getLabel()).isEqualTo("보통");
        assertThat(ReadingCompetencyLevel.fromScore(60).getLabel()).isEqualTo("우수");
        assertThat(ReadingCompetencyLevel.fromScore(79).getLabel()).isEqualTo("우수");
        assertThat(ReadingCompetencyLevel.fromScore(80).getLabel()).isEqualTo("매우 우수");
        assertThat(ReadingCompetencyLevel.fromScore(100).getLabel()).isEqualTo("매우 우수");
    }
}
