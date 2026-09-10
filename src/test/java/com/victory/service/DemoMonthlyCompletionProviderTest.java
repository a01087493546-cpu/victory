package com.victory.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

class DemoMonthlyCompletionProviderTest {
    private final DemoMonthlyCompletionProvider provider = new DemoMonthlyCompletionProvider();

    @Test
    void representativeStudentUsesDocumentedFixedTwelveMonthCounts() {
        assertThat(provider.forLoginId("ss01"))
            .isEqualTo(List.of(1, 2, 1, 2, 3, 2, 3, 2, 4, 2, 3, 2));
    }

    @Test
    void everyEntryHasExactlyTwelveMonthsWithinNaturalElementaryRange() {
        List<String> loginIds = List.of(
            "ss01", "demo_student_02", "demo_student_03", "demo_student_04",
            "demo_student_05", "demo_student_06", "demo_student_07", "demo_student_08");

        for (String loginId : loginIds) {
            List<Integer> counts = provider.forLoginId(loginId);
            assertThat(counts).hasSize(12);
            assertThat(counts).allMatch(count -> count >= 0 && count <= 4);
        }
    }

    @Test
    void noEntryIsMonotonousAcrossAllTwelveMonths() {
        List<String> loginIds = List.of(
            "ss01", "demo_student_02", "demo_student_03", "demo_student_04",
            "demo_student_05", "demo_student_06", "demo_student_07", "demo_student_08");

        for (String loginId : loginIds) {
            List<Integer> counts = provider.forLoginId(loginId);
            assertThat(counts.stream().distinct().count())
                .as("loginId=%s should not be a single repeated value across all 12 months", loginId)
                .isGreaterThan(1);
        }
    }

    @Test
    void sameStudentIsStableAndDifferentStudentsHaveDifferentShapes() {
        var first = provider.forLoginId("demo_student_04");
        var repeated = provider.forLoginId("demo_student_04");
        var another = provider.forLoginId("demo_student_07");

        assertThat(first).isEqualTo(repeated);
        assertThat(first).isNotEqualTo(another);
    }

    @Test
    void allEightDemoStudentsHaveUniqueMonthlyShapes() {
        List<String> loginIds = List.of(
            "ss01", "demo_student_02", "demo_student_03", "demo_student_04",
            "demo_student_05", "demo_student_06", "demo_student_07", "demo_student_08");

        assertThat(loginIds.stream().map(provider::forLoginId).distinct()).hasSize(8);
    }

    @Test
    void legacyAndCanonicalLoginIdsForSameDemoStudentMatch() {
        assertThat(provider.forLoginId("ss04"))
            .isEqualTo(provider.forLoginId("demo_student_04"));
        assertThat(provider.forLoginId("ss07"))
            .isEqualTo(provider.forLoginId("demo_student_07"));
    }

    @Test
    void unknownLoginIdFallsBackToDefaultTwelveMonthCounts() {
        assertThat(provider.forLoginId("unknown-login-id")).hasSize(12);
    }
}
