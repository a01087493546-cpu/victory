package com.victory.service;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.victory.dto.PortfolioAiAnalysisResponse;
import com.victory.dto.PracticeStageDetail;

import lombok.RequiredArgsConstructor;

/*
 * 심사 온책읽기 포트폴리오의 읽기 전/중/후 완료 상태를 학생별로 항상
 * "논리적인 진행 순서"만 나오게 고정한다(랜덤 없음). 실제 심사 계정
 * seed 데이터(DemoClassActivityInitializer)는 읽기 전(before) 단계
 * Response가 전혀 없이 읽기 중/후만 채워져 있어, 실제 쿼리를 그대로
 * 쓰면 "읽기 전 미완료인데 읽기 중은 완료"처럼 앞뒤가 안 맞는 조합이
 * 나온다. 그래서 학생마다 4가지 허용 패턴(전부 미완료 / 전만 완료 /
 * 전+중 완료 / 전부 완료) 중 하나만 골라 결정적으로 돌려준다.
 * completed 단계의 growthNote는 DemoPracticePortfolioAiProvider의
 * 해당 단계 strengthText를 그대로 재사용해, AI 호출 전(aggregate 응답)
 * 부터 이미 실제 성장 문장이 보이게 한다.
 */
@Component
@RequiredArgsConstructor
public class DemoPracticeStageProvider {
    private static final String NOT_PARTICIPATED_NOTE = "현재 기록에서는 이 단계 활동 기록이 아직 없어요.";

    private final DemoPracticePortfolioAiProvider demoPracticePortfolioAiProvider;

    private enum Pattern {
        NONE(false, false, false),
        BEFORE_ONLY(true, false, false),
        BEFORE_DURING(true, true, false),
        ALL(true, true, true);

        final boolean before;
        final boolean during;
        final boolean after;

        Pattern(boolean before, boolean during, boolean after) {
            this.before = before;
            this.during = during;
            this.after = after;
        }
    }

    private static final Map<String, Pattern> PATTERN_BY_LOGIN_ID = Map.ofEntries(
        Map.entry("ss01", Pattern.ALL),                                                     // 김초롱
        Map.entry("ss02", Pattern.BEFORE_DURING), Map.entry("demo_student_02", Pattern.BEFORE_DURING), // 송민정
        Map.entry("ss03", Pattern.BEFORE_DURING), Map.entry("demo_student_03", Pattern.BEFORE_DURING), // 박하민
        Map.entry("ss04", Pattern.BEFORE_DURING), Map.entry("demo_student_04", Pattern.BEFORE_DURING), // 이진우
        Map.entry("ss05", Pattern.NONE), Map.entry("demo_student_05", Pattern.NONE),         // 김민지
        Map.entry("ss06", Pattern.ALL), Map.entry("demo_student_06", Pattern.ALL),           // 서희원
        Map.entry("ss07", Pattern.ALL), Map.entry("demo_student_07", Pattern.ALL),           // 김수진
        Map.entry("ss08", Pattern.BEFORE_ONLY), Map.entry("demo_student_08", Pattern.BEFORE_ONLY) // 이혜원
    );

    public record StageBundle(PracticeStageDetail before, PracticeStageDetail during, PracticeStageDetail after) {
    }

    public StageBundle forLoginId(String loginId) {
        Pattern pattern = PATTERN_BY_LOGIN_ID.getOrDefault(loginId, Pattern.NONE);
        PortfolioAiAnalysisResponse aiText = demoPracticePortfolioAiProvider.forLoginId(loginId);
        return new StageBundle(
            stage(pattern.before, "질문 만들기에 참여함", aiText.stageAnalysis().before().strengthText()),
            stage(pattern.during, "근거를 들어 답함", aiText.stageAnalysis().during().strengthText()),
            stage(pattern.after, "간추리기 활동 참여", aiText.stageAnalysis().after().strengthText()));
    }

    private PracticeStageDetail stage(boolean completed, String shortTitle, String growthNote) {
        if (!completed) {
            return new PracticeStageDetail(false, false, "아직 참여 기록 없음", NOT_PARTICIPATED_NOTE, null);
        }
        return new PracticeStageDetail(true, true, shortTitle, growthNote, null);
    }
}
