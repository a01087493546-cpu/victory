package com.victory.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonProperty;

public record IndividualPortfolioResponse(
    Long studentId, String studentName, Integer grade, Integer classNumber,
    LocalDate periodStart, LocalDate periodEnd,
    long completedBookCount,
    BigDecimal averageReadingPracticeScore,
    BigDecimal averageRecordCompletionScore,
    ActivitySummary activitySummary,
    MonthlyCompletionStatsResponse monthlyCompletionStats,
    Competencies competencies,
    boolean demoDerived) {

    @JsonProperty("booksReadCount")
    public long booksReadCount() { return completedBookCount; }

    @JsonProperty("readingPracticeAvg")
    public BigDecimal readingPracticeAvg() { return averageReadingPracticeScore; }

    @JsonProperty("recordCompletionAvg")
    public BigDecimal recordCompletionAvg() { return averageRecordCompletionScore; }

    @JsonProperty("activityQuestionCount")
    public long activityQuestionCount() { return activitySummary.questions(); }

    @JsonProperty("activityThoughtCount")
    public long activityThoughtCount() { return activitySummary.thoughtWriting(); }

    @JsonProperty("activitySummaryCount")
    public long activitySummaryCount() { return activitySummary.summaries(); }

    @JsonProperty("activityBookChatCount")
    public long activityBookChatCount() { return activitySummary.bookChatSharing(); }

    @JsonProperty("monthlyCounts")
    public java.util.List<Integer> monthlyCounts() { return monthlyCompletionStats.getMonthlyCounts(); }

    @JsonProperty("competencyQuestion")
    public long competencyQuestion() { return competencies.questionGeneration(); }

    @JsonProperty("competencyPersistence")
    public long competencyPersistence() { return competencies.readingPersistence(); }

    @JsonProperty("competencyRefine")
    public long competencyRefine() { return competencies.thoughtRefinement(); }

    @JsonProperty("competencyShare")
    public long competencyShare() { return competencies.thoughtSharing(); }

    public record ActivitySummary(long questions, long thoughtWriting, long summaries, long bookChatSharing) {
    }

    public record Competencies(long questionGeneration, long readingPersistence,
                               long thoughtRefinement, long thoughtSharing) {
    }
}
