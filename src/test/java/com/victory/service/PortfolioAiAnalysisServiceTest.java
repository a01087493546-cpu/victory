package com.victory.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.victory.dto.IndividualPortfolioResponse;
import com.victory.dto.MonthlyCompletionStatsResponse;
import com.victory.dto.PortfolioActivityCount;
import com.victory.dto.PortfolioAiAnalysisResponse;
import com.victory.dto.PracticePortfolioResponse;

class PortfolioAiAnalysisServiceTest {
    private final StudentPortfolioService portfolioService = mock(StudentPortfolioService.class);
    private final FeedbackAiService feedbackAiService = mock(FeedbackAiService.class);
    private final PortfolioAiAnalysisService service =
        new PortfolioAiAnalysisService(portfolioService, feedbackAiService);
    private final LocalDate from = LocalDate.of(2026, 8, 1);
    private final LocalDate to = LocalDate.of(2026, 8, 31);

    @Test
    @SuppressWarnings("unchecked")
    void practiceAnalysis_sendsOnlyPracticeAggregateFields() {
        PracticePortfolioResponse aggregate = new PracticePortfolioResponse(2L, "김학생", 4, 2, from, to,
            82d, 91d, new PortfolioActivityCount(1, true), new PortfolioActivityCount(2, true),
            new PortfolioActivityCount(0, false), "마당을 나온 암탉", 3);
        when(portfolioService.getPracticePortfolio(1L, 10L, 2L, from, to)).thenReturn(aggregate);
        when(feedbackAiService.generatePortfolioAnalysis(eq("practice"), org.mockito.ArgumentMatchers.anyMap()))
            .thenReturn(new PortfolioAiAnalysisResponse("꾸준히 참여했어요.", "읽기 후 활동을 이어가면 좋아요."));

        PortfolioAiAnalysisResponse result = service.analyzePractice(1L, 10L, 2L, from, to);

        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(feedbackAiService).generatePortfolioAnalysis(eq("practice"), captor.capture());
        assertThat(captor.getValue()).containsKeys("participationRate", "comprehensionRate", "beforeParticipation",
            "duringParticipation", "afterParticipation", "currentBookTitle", "activityCount");
        assertThat(captor.getValue()).doesNotContainKeys("completedBookCount", "competencies", "monthlyCompletionStats");
        assertThat(result.strengthText()).isEqualTo("꾸준히 참여했어요.");
    }

    @Test
    @SuppressWarnings("unchecked")
    void individualAnalysis_sendsOnlyIndividualAggregateFieldsIncludingDemoDerivedValues() {
        IndividualPortfolioResponse aggregate = new IndividualPortfolioResponse(2L, "심사학생", 4, 2, from, to,
            2, new BigDecimal("88.00"), new BigDecimal("92.00"),
            new IndividualPortfolioResponse.ActivitySummary(5, 4, 2, 1),
            new MonthlyCompletionStatsResponse(2026, List.of(0,0,0,0,0,0,0,2,0,0,0,0)),
            new IndividualPortfolioResponse.Competencies(3, 6, 3, 1), true);
        when(portfolioService.getIndividualPortfolio(1L, 10L, 2L, from, to)).thenReturn(aggregate);
        when(feedbackAiService.generatePortfolioAnalysis(eq("individual"), org.mockito.ArgumentMatchers.anyMap()))
            .thenReturn(new PortfolioAiAnalysisResponse("기록을 잘 이어가고 있어요.", "생각을 더 나눠 보면 좋아요."));

        service.analyzeIndividual(1L, 10L, 2L, from, to);

        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(feedbackAiService).generatePortfolioAnalysis(eq("individual"), captor.capture());
        assertThat(captor.getValue()).containsKeys("completedBookCount", "averageReadingPracticeScore",
            "averageRecordCompletionScore", "activitySummary", "monthlyCompletionStats", "competencies");
        assertThat(captor.getValue()).doesNotContainKeys("participationRate", "comprehensionRate", "currentBookTitle");
    }

    @Test
    void aggregateAuthorizationFailure_isPropagatedBeforeAiCall() {
        ResponseStatusException forbidden = new ResponseStatusException(HttpStatus.FORBIDDEN, "담당 학급 학생만 조회");
        when(portfolioService.getPracticePortfolio(1L, 10L, 99L, from, to)).thenThrow(forbidden);
        assertThatThrownBy(() -> service.analyzePractice(1L, 10L, 99L, from, to)).isSameAs(forbidden);
        verify(feedbackAiService, never()).generatePortfolioAnalysis(eq("practice"), org.mockito.ArgumentMatchers.anyMap());
    }
}
