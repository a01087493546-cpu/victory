package com.victory.dto;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
    ReadingCompetencies readingCompetencies,
    boolean demoDerived) {

    @JsonProperty("booksReadCount")
    public long booksReadCount() { return completedBookCount; }

    @JsonProperty("readingPracticeAvg")
    public BigDecimal readingPracticeAvg() { return averageReadingPracticeScore; }

    @JsonProperty("recordCompletionAvg")
    public BigDecimal recordCompletionAvg() { return averageRecordCompletionScore; }

    /*
     * 개별읽기 핵심지표(독서 실천도 평균/기록 완성도 평균) 단계 배지 -
     * 0~39 노력 필요, 40~59 보통, 60~79 우수, 80~100 매우 우수
     * (ReadingCompetencyLevel과 동일 기준 공유, 새 점수 계산 없음 -
     * 이미 계산된 averageReadingPracticeScore/averageRecordCompletionScore를
     * 반올림·clamp만 해서 등급을 판정한다). score가 없으면(null) 배지도 null.
     */
    @JsonProperty("readingPracticeLevel")
    public String readingPracticeLevel() { return levelLabel(averageReadingPracticeScore); }

    @JsonProperty("recordCompletionLevel")
    public String recordCompletionLevel() { return levelLabel(averageRecordCompletionScore); }

    private static String levelLabel(BigDecimal score) {
        if (score == null) {
            return null;
        }
        int rounded = score.setScale(0, RoundingMode.HALF_UP).intValue();
        int clamped = Math.max(0, Math.min(100, rounded));
        return ReadingCompetencyLevel.fromScore(clamped).getLabel();
    }

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

    /*
     * 포트폴리오 전용 "독서 역량 4종" - 게임 능력치 보상 누적값(위
     * Competencies)과는 완전히 다른, 학생 개인의 평가기간 내 절대적
     * 수행 비율 기반 0~100 점수 + 4단계 성장 수준. 위 Competencies
     * 필드는 다른 기능이 참조할 수 있으므로 그대로 두고, 새 의미는
     * 반드시 이 필드로만 노출한다(ReadingCompetencyCalculator 참고).
     */
    public record ReadingCompetencies(
        ReadingCompetencyScore questionGeneration,
        ReadingCompetencyScore readingPersistence,
        ReadingCompetencyScore thoughtRefinement,
        ReadingCompetencyScore thoughtSharing) {
    }
}
