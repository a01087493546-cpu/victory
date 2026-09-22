package com.victory.dto;

import java.time.LocalDate;

public record PortfolioAiAnalysisRequest(LocalDate from, LocalDate to, Long regenerationVersion) {
    public PortfolioAiAnalysisRequest(LocalDate from, LocalDate to) {
        this(from, to, 0L);
    }
}
