package com.victory.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.victory.entity.Summary;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AfterReadingDataResponse {

    private Long classReadingBookId;
    private String bookType;
    private List<AfterReadingQuestionItem> questions;
    private Long summaryId;
    private String summary;
    private Boolean summaryAiPassed;
    private Boolean afterDone;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Boolean practiceCompleted;
    private Boolean rewardGranted;
    private Boolean rewardAlreadyGranted;
    private StudentStatsResponse stats;

    public static AfterReadingDataResponse of(
            Long classReadingBookId,
            Summary summary,
            String savedBookType,
            List<AfterReadingQuestionItem> questions,
            Boolean afterDone) {

        return new AfterReadingDataResponse(
            classReadingBookId,
            savedBookType,
            questions,
            summary == null ? null : summary.getId(),
            summary == null ? null : summary.getSummaryText(),
            summary == null ? null : summary.getAiPassed(),
            afterDone,
            summary == null ? null : summary.getCreatedAt(),
            summary == null ? null : summary.getUpdatedAt(),
            false,
            false,
            false,
            null
        );
    }

    public AfterReadingDataResponse withPracticeReward(
            PracticeProgressResponse progressResponse) {

        if (progressResponse == null) {
            return this;
        }

        return new AfterReadingDataResponse(
            classReadingBookId,
            bookType,
            questions,
            summaryId,
            summary,
            summaryAiPassed,
            afterDone,
            createdAt,
            updatedAt,
            progressResponse.getPracticeCompleted(),
            progressResponse.getRewardGranted(),
            progressResponse.getRewardAlreadyGranted(),
            progressResponse.getStats()
        );
    }
}
