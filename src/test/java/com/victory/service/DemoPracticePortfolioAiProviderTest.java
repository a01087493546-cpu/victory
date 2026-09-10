package com.victory.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.victory.dto.PortfolioAiAnalysisResponse;

class DemoPracticePortfolioAiProviderTest {
    private final DemoPracticePortfolioAiProvider provider = new DemoPracticePortfolioAiProvider();

    private static final List<String> LOGIN_IDS = List.of(
        "ss01", "demo_student_02", "demo_student_03", "demo_student_04",
        "demo_student_05", "demo_student_06", "demo_student_07", "demo_student_08");

    @Test
    void everyStudentHasFullStageAnalysisAndOverallText() {
        for (String loginId : LOGIN_IDS) {
            PortfolioAiAnalysisResponse response = provider.forLoginId(loginId);

            assertThat(response.strengthText()).isNotBlank();
            assertThat(response.improvementText()).isNotBlank();
            assertThat(response.stageAnalysis()).isNotNull();
            assertThat(response.stageAnalysis().before().title()).isEqualTo("읽기 전");
            assertThat(response.stageAnalysis().during().title()).isEqualTo("읽기 중");
            assertThat(response.stageAnalysis().after().title()).isEqualTo("읽기 후");
            assertThat(response.stageAnalysis().before().strengthText()).isNotBlank();
            assertThat(response.stageAnalysis().during().strengthText()).isNotBlank();
            assertThat(response.stageAnalysis().after().strengthText()).isNotBlank();
        }
    }

    @Test
    void sameLoginIdIsDeterministicAcrossCalls() {
        assertThat(provider.forLoginId("ss01")).isEqualTo(provider.forLoginId("ss01"));
    }

    @Test
    void legacyAndCanonicalLoginIdsForSameDemoStudentMatch() {
        assertThat(provider.forLoginId("ss04")).isEqualTo(provider.forLoginId("demo_student_04"));
        assertThat(provider.forLoginId("ss07")).isEqualTo(provider.forLoginId("demo_student_07"));
    }

    @Test
    void allEightStudentsHaveDistinctOverallAndStageText() {
        List<String> overallStrengths = LOGIN_IDS.stream()
            .map(id -> provider.forLoginId(id).strengthText()).distinct().toList();
        List<String> beforeTexts = LOGIN_IDS.stream()
            .map(id -> provider.forLoginId(id).stageAnalysis().before().strengthText()).distinct().toList();

        assertThat(overallStrengths).hasSize(LOGIN_IDS.size());
        assertThat(beforeTexts).hasSize(LOGIN_IDS.size());
    }

    /* stageAnalysis 문구와 overall 문구가 같은 학생 안에서 서로 그대로 복제된 문장이 아닌지. */
    @Test
    void overallTextDoesNotDuplicateStageAnalysisTextVerbatim() {
        for (String loginId : LOGIN_IDS) {
            PortfolioAiAnalysisResponse response = provider.forLoginId(loginId);
            assertThat(response.strengthText()).isNotEqualTo(response.stageAnalysis().before().strengthText());
            assertThat(response.strengthText()).isNotEqualTo(response.stageAnalysis().during().strengthText());
            assertThat(response.strengthText()).isNotEqualTo(response.stageAnalysis().after().strengthText());
        }
    }

    @Test
    void unknownLoginIdFallsBackToDefaultResponse() {
        PortfolioAiAnalysisResponse response = provider.forLoginId("unknown-login-id");
        assertThat(response.strengthText()).isNotBlank();
        assertThat(response.stageAnalysis()).isNotNull();
    }
}
