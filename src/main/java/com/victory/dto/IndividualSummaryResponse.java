package com.victory.dto;

import java.time.LocalDateTime;

import com.victory.entity.Summary;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class IndividualSummaryResponse {

    private Long summaryId;
    private Long readingRecordId;
    private String bookType;
    private String summaryText;
    private Boolean aiPassed;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static IndividualSummaryResponse from(Summary summary) {
        return new IndividualSummaryResponse(
            summary.getId(),
            summary.getReadingRecord() == null ? null : summary.getReadingRecord().getId(),
            summary.getBookType(),
            summary.getSummaryText(),
            summary.getAiPassed(),
            summary.getCreatedAt(),
            summary.getUpdatedAt()
        );
    }
}
