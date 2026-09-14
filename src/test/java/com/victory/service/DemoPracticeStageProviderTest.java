package com.victory.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.victory.dto.PracticeStageDetail;

class DemoPracticeStageProviderTest {
    private final DemoPracticeStageProvider provider =
        new DemoPracticeStageProvider(new DemoPracticePortfolioAiProvider());

    private static final List<String> LOGIN_IDS = List.of(
        "ss01", "demo_student_02", "demo_student_03", "demo_student_04",
        "demo_student_05", "demo_student_06", "demo_student_07", "demo_student_08");

    /* 진행 순서상 뒤 단계가 완료면 반드시 앞 단계도 완료여야 한다(비논리적 조합 금지). */
    @Test
    void everyStudentFollowsALogicalProgressionPattern() {
        for (String loginId : LOGIN_IDS) {
            DemoPracticeStageProvider.StageBundle bundle = provider.forLoginId(loginId);
            boolean before = bundle.before().completed();
            boolean during = bundle.during().completed();
            boolean after = bundle.after().completed();

            if (during) {
                assertThat(before).as("%s: during completed but before not", loginId).isTrue();
            }
            if (after) {
                assertThat(during).as("%s: after completed but during not", loginId).isTrue();
            }
        }
    }

    @Test
    void allFourPatternsAppearAcrossTheEightStudents() {
        List<String> patterns = LOGIN_IDS.stream().map(id -> {
            DemoPracticeStageProvider.StageBundle bundle = provider.forLoginId(id);
            return "" + bundle.before().completed() + bundle.during().completed() + bundle.after().completed();
        }).distinct().toList();

        assertThat(patterns).containsExactlyInAnyOrder("falsefalsefalse", "truefalsefalse", "truetruefalse", "truetruetrue");
    }

    @Test
    void completedStagesHaveNonBlankGrowthNoteAndIncompleteStagesUseTheStandardNote() {
        for (String loginId : LOGIN_IDS) {
            DemoPracticeStageProvider.StageBundle bundle = provider.forLoginId(loginId);
            for (PracticeStageDetail stage : List.of(bundle.before(), bundle.during(), bundle.after())) {
                if (stage.completed()) {
                    assertThat(stage.growthNote()).isNotBlank();
                } else {
                    assertThat(stage.growthNote()).isEqualTo("현재 기록에서는 이 단계 활동 기록이 아직 없어요.");
                }
            }
        }
    }

    @Test
    void sameLoginIdIsDeterministicAcrossCalls() {
        assertThat(provider.forLoginId("ss01")).isEqualTo(provider.forLoginId("ss01"));
    }

    @Test
    void legacyAndCanonicalLoginIdsForSameDemoStudentMatch() {
        assertThat(provider.forLoginId("ss04")).isEqualTo(provider.forLoginId("demo_student_04"));
    }

    @Test
    void unknownLoginIdFallsBackToAllIncomplete() {
        DemoPracticeStageProvider.StageBundle bundle = provider.forLoginId("unknown-login-id");
        assertThat(bundle.before().completed()).isFalse();
        assertThat(bundle.during().completed()).isFalse();
        assertThat(bundle.after().completed()).isFalse();
    }
}
