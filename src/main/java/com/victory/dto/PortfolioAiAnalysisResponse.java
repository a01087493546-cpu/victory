package com.victory.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/*
 * stageAnalysis는 온책읽기(practice) 포트폴리오에서만 채워진다 - 개별읽기
 * 호출은 이 필드를 항상 null로 둔다(FeedbackAiService의 시스템 프롬프트가
 * portfolioType에 따라 강제). 기존 2개 인자 생성자는 stageAnalysis 없이
 * 쓰던 기존 테스트/호출부를 그대로 두기 위한 하위 호환 오버로드다.
 * 생성자가 2개라 Jackson이 어떤 것을 역직렬화에 쓸지 스스로 고르게 두면
 * (구현체마다) 더 짧은 2개 인자 생성자를 골라 stageAnalysis를 조용히
 * null로 떨어뜨릴 수 있어 @JsonCreator로 canonical 생성자를 명시한다.
 */
public record PortfolioAiAnalysisResponse(String strengthText, String improvementText, StageAnalysis stageAnalysis) {

    @JsonCreator
    public PortfolioAiAnalysisResponse(
            @JsonProperty("strengthText") String strengthText,
            @JsonProperty("improvementText") String improvementText,
            @JsonProperty("stageAnalysis") StageAnalysis stageAnalysis) {
        this.strengthText = strengthText;
        this.improvementText = improvementText;
        this.stageAnalysis = stageAnalysis;
    }

    public PortfolioAiAnalysisResponse(String strengthText, String improvementText) {
        this(strengthText, improvementText, null);
    }

    public record StageAnalysis(StageAnalysisItem before, StageAnalysisItem during, StageAnalysisItem after) {
    }

    public record StageAnalysisItem(String title, boolean completed, String strengthText, String growthText) {
    }
}
