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
    String currentBookTitle, long activityCount) {

    @JsonProperty("beforeStatus")
    public String beforeStatus() { return beforeParticipation.participated() ? "완료" : "미완료"; }

    @JsonProperty("duringStatus")
    public String duringStatus() { return duringParticipation.participated() ? "완료" : "미완료"; }

    @JsonProperty("afterStatus")
    public String afterStatus() { return afterParticipation.participated() ? "완료" : "미완료"; }

    @JsonProperty("bookTitle")
    public String bookTitle() { return currentBookTitle; }
}
