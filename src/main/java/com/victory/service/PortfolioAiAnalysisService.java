package com.victory.service;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.victory.dto.IndividualPortfolioResponse;
import com.victory.dto.PortfolioAiAnalysisResponse;
import com.victory.dto.PracticePortfolioResponse;

import lombok.RequiredArgsConstructor;

/** 집계 원본에서 AI에 허용된 필드만 전달하는 포트폴리오 분석 오케스트레이터. */
@Service
@RequiredArgsConstructor
public class PortfolioAiAnalysisService {
    static final String TYPE_PRACTICE = "practice";
    static final String TYPE_INDIVIDUAL = "individual";

    private final StudentPortfolioService portfolioService;
    private final FeedbackAiService feedbackAiService;

    public PortfolioAiAnalysisResponse analyzePractice(
            Long teacherId, Long classId, Long studentId, LocalDate from, LocalDate to) {
        PracticePortfolioResponse data = portfolioService
            .getPracticePortfolio(teacherId, classId, studentId, from, to);
        return feedbackAiService.generatePortfolioAnalysis(TYPE_PRACTICE, practiceInput(data));
    }

    public PortfolioAiAnalysisResponse analyzeIndividual(
            Long teacherId, Long classId, Long studentId, LocalDate from, LocalDate to) {
        IndividualPortfolioResponse data = portfolioService
            .getIndividualPortfolio(teacherId, classId, studentId, from, to);
        return feedbackAiService.generatePortfolioAnalysis(TYPE_INDIVIDUAL, individualInput(data));
    }

    Map<String, Object> practiceInput(PracticePortfolioResponse data) {
        Map<String, Object> input = new LinkedHashMap<>();
        input.put("studentName", data.studentName());
        input.put("periodStart", data.periodStart());
        input.put("periodEnd", data.periodEnd());
        input.put("participationRate", data.participationRate());
        input.put("comprehensionRate", data.comprehensionRate());
        input.put("beforeParticipation", data.beforeParticipation());
        input.put("duringParticipation", data.duringParticipation());
        input.put("afterParticipation", data.afterParticipation());
        input.put("currentBookTitle", data.currentBookTitle());
        input.put("activityCount", data.activityCount());
        return input;
    }

    Map<String, Object> individualInput(IndividualPortfolioResponse data) {
        Map<String, Object> input = new LinkedHashMap<>();
        input.put("studentName", data.studentName());
        input.put("periodStart", data.periodStart());
        input.put("periodEnd", data.periodEnd());
        input.put("completedBookCount", data.completedBookCount());
        input.put("averageReadingPracticeScore", data.averageReadingPracticeScore());
        input.put("averageRecordCompletionScore", data.averageRecordCompletionScore());
        input.put("activitySummary", data.activitySummary());
        input.put("monthlyCompletionStats", data.monthlyCompletionStats());
        input.put("competencies", data.competencies());
        return input;
    }
}
