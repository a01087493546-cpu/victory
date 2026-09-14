package com.victory.controller;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;

import com.victory.dto.PortfolioAiAnalysisRequest;
import com.victory.dto.PortfolioAiAnalysisResponse;
import com.victory.service.PortfolioAiAnalysisService;
import com.victory.service.StudentPortfolioService;

class TeacherStudentPortfolioControllerTest {
    private final StudentPortfolioService portfolioService = mock(StudentPortfolioService.class);
    private final PortfolioAiAnalysisService aiService = mock(PortfolioAiAnalysisService.class);
    private final TeacherStudentPortfolioController controller =
        new TeacherStudentPortfolioController(portfolioService, aiService);
    private final PortfolioAiAnalysisRequest request = new PortfolioAiAnalysisRequest(
        LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31));

    @Test
    void aiAnalysis_rejectsDifferentTeacherPrincipalBeforeAggregationOrAi() {
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(999L);

        assertThatThrownBy(() -> controller.analyzePractice(1L, 10L, 2L, request, authentication))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("403");
        verify(aiService, never()).analyzePractice(1L, 10L, 2L, request.from(), request.to());
    }

    @Test
    void aiAnalysis_allowsTeacherSelfAndDelegatesClassStudentValidationToAggregateService() {
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(1L);
        when(aiService.analyzeIndividual(1L, 10L, 2L, request.from(), request.to()))
            .thenReturn(new PortfolioAiAnalysisResponse("잘하는 점", "노력할 점"));

        controller.analyzeIndividual(1L, 10L, 2L, request, authentication);

        verify(aiService).analyzeIndividual(1L, 10L, 2L, request.from(), request.to());
    }
}
