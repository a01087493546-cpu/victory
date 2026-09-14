package com.victory.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.victory.dto.PortfolioAiAnalysisResponse;

class DemoIndividualPortfolioAiProviderTest {
    private final DemoIndividualPortfolioAiProvider provider = new DemoIndividualPortfolioAiProvider();

    private static final List<String> LOGIN_IDS = List.of(
        "ss01", "demo_student_02", "demo_student_03", "demo_student_04",
        "demo_student_05", "demo_student_06", "demo_student_07", "demo_student_08");

    @Test
    void everyStudentHasNonBlankOverallTextAndNullStageAnalysis() {
        for (String loginId : LOGIN_IDS) {
            PortfolioAiAnalysisResponse response = provider.forLoginId(loginId);
            assertThat(response.strengthText()).isNotBlank();
            assertThat(response.improvementText()).isNotBlank();
            assertThat(response.stageAnalysis()).isNull();
        }
    }

    @Test
    void sameLoginIdIsDeterministicAcrossCalls() {
        assertThat(provider.forLoginId("demo_student_05")).isEqualTo(provider.forLoginId("demo_student_05"));
    }

    @Test
    void legacyAndCanonicalLoginIdsForSameDemoStudentMatch() {
        assertThat(provider.forLoginId("ss03")).isEqualTo(provider.forLoginId("demo_student_03"));
    }

    @Test
    void allEightStudentsHaveDistinctOverallText() {
        List<String> strengths = LOGIN_IDS.stream().map(id -> provider.forLoginId(id).strengthText()).distinct().toList();
        List<String> improvements = LOGIN_IDS.stream().map(id -> provider.forLoginId(id).improvementText()).distinct().toList();

        assertThat(strengths).hasSize(LOGIN_IDS.size());
        assertThat(improvements).hasSize(LOGIN_IDS.size());
    }

    @Test
    void unknownLoginIdFallsBackToDefaultResponse() {
        PortfolioAiAnalysisResponse response = provider.forLoginId("unknown-login-id");
        assertThat(response.strengthText()).isNotBlank();
        assertThat(response.stageAnalysis()).isNull();
    }
}
