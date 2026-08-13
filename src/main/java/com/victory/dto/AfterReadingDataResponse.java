package com.victory.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

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
    // 책 유형별(story/info/opinion) 연습 질문/답 최신 통과본 - bookType을 key로 가진다.
    private Map<String, AfterReadingTypePracticeItem> typePracticeAnswers;
    private Map<String, List<AfterReadingQuestionItem>> questionsByBookType;

    public static AfterReadingDataResponse of(
            Long classReadingBookId,
            Summary summary,
            String savedBookType,
            List<AfterReadingQuestionItem> questions,
            Boolean afterDone,
            Map<String, AfterReadingTypePracticeItem> typePracticeAnswers,
            Map<String, List<AfterReadingQuestionItem>> questionsByBookType) {

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
            null,
            typePracticeAnswers,
            questionsByBookType
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
            progressResponse.getStats(),
            typePracticeAnswers,
            questionsByBookType
        );
    }
}
