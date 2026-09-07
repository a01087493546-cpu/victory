package com.victory.dto;

import java.time.LocalDate;

public record PortfolioAiAnalysisRequest(LocalDate from, LocalDate to) {
}
