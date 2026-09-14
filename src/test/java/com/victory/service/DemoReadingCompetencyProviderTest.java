package com.victory.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class DemoReadingCompetencyProviderTest {
    private final DemoReadingCompetencyProvider provider = new DemoReadingCompetencyProvider();

    @Test
    void representativeStudentUsesDocumentedFixedScoresAndLevels() {
        var result = provider.forLoginId("ss01");

        assertThat(result.questionGeneration().score()).isEqualTo(82);
        assertThat(result.questionGeneration().level()).isEqualTo("매우 우수");
        assertThat(result.readingPersistence().score()).isEqualTo(76);
        assertThat(result.readingPersistence().level()).isEqualTo("우수");
        assertThat(result.thoughtRefinement().score()).isEqualTo(88);
        assertThat(result.thoughtSharing().score()).isEqualTo(64);
    }

    @Test
    void sameStudentIsStableAndDifferentStudentsHaveDifferentPatterns() {
        var first = provider.forLoginId("demo_student_02");
        var repeated = provider.forLoginId("demo_student_02");
        var another = provider.forLoginId("demo_student_03");

        assertThat(first).isEqualTo(repeated);
        assertThat(first).isNotEqualTo(another);
    }

    @Test
    void fixedDemoTableIncludesAllFourGrowthLevels() {
        var needsEffort = provider.forLoginId("demo_student_04").questionGeneration();
        var average = provider.forLoginId("demo_student_03").questionGeneration();
        var good = provider.forLoginId("demo_student_02").questionGeneration();
        var excellent = provider.forLoginId("ss01").questionGeneration();

        assertThat(needsEffort.level()).isEqualTo("노력 필요");
        assertThat(average.level()).isEqualTo("보통");
        assertThat(good.level()).isEqualTo("우수");
        assertThat(excellent.level()).isEqualTo("매우 우수");
    }

    @Test
    void legacyAndCanonicalLoginIdsForSameDemoStudentMatch() {
        assertThat(provider.forLoginId("ss04"))
            .isEqualTo(provider.forLoginId("demo_student_04"));
        assertThat(provider.forLoginId("ss07"))
            .isEqualTo(provider.forLoginId("demo_student_07"));
    }
}
