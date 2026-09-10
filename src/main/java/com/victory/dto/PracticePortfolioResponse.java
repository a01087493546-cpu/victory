package com.victory.dto;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonProperty;

public record PracticePortfolioResponse(
    Long studentId, String studentName, Integer grade, Integer classNumber,
    LocalDate periodStart, LocalDate periodEnd,
    Double participationRate, Double comprehensionRate,
    PortfolioActivityCount beforeParticipation,
    PortfolioActivityCount duringParticipation,
    PortfolioActivityCount afterParticipation,
    String currentBookTitle, long activityCount,
    PracticeStageDetail beforeStage,
    PracticeStageDetail duringStage,
    PracticeStageDetail afterStage) {

    @JsonProperty("beforeStatus")
    public String beforeStatus() { return beforeParticipation.participated() ? "완료" : "미완료"; }

    @JsonProperty("duringStatus")
    public String duringStatus() { return duringParticipation.participated() ? "완료" : "미완료"; }

    @JsonProperty("afterStatus")
    public String afterStatus() { return afterParticipation.participated() ? "완료" : "미완료"; }

    @JsonProperty("bookTitle")
    public String bookTitle() { return currentBookTitle; }

    /*
     * 온책 핵심지표(참여도/질문 이해도) 단계 배지 - 0~39 노력 필요,
     * 40~59 보통, 60~79 우수, 80~100 매우 우수(ReadingCompetencyLevel과
     * 동일 기준 공유). 프론트가 별도 계산 없이 바로 쓸 수 있게 라벨
     * 문자열로 내려준다. rate가 없으면(null) 배지도 null.
     */
    @JsonProperty("participationLevel")
    public String participationLevel() { return levelLabel(participationRate); }

    @JsonProperty("comprehensionLevel")
    public String comprehensionLevel() { return levelLabel(comprehensionRate); }

    private static String levelLabel(Double rate) {
        if (rate == null) {
            return null;
        }
        int clamped = (int) Math.round(Math.max(0, Math.min(100, rate)));
        return ReadingCompetencyLevel.fromScore(clamped).getLabel();
    }
}
