package com.victory.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class StudentAchievementItem {

    private Long studentId;
    private Integer studentNumber;
    private String studentName;

    private Double pageProgress;
    private Double reviewScore;
    private Double finalReadingProgress;

    private Double readingActivityCompletionRate;
    private Double questionParticipationRate;
    private Double thoughtSharingParticipationRate;
    private Double participationRate;

    private Boolean hasAiEvaluation;
    private Integer aiEvaluatedQuestionCount;
    private Integer passedWithinThreeAttemptsCount;
    private Double comprehensionRate;

    private Double achievementRate;

    private Boolean needsSupport;
    private List<SupportReasonItem> supportReasons;
}
