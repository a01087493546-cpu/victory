package com.victory.dto;

import java.time.LocalDateTime;
import java.util.Map;

import com.victory.entity.PracticeProgress;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PracticeProgressResponse {

    private Long id;
    private Long studentId;

    private Boolean bookSelected;
    private Boolean beforeDone;
    private Boolean classReadDone;
    private Boolean afterDone;

    private Map<String, Boolean> duringTypeProgress;

    private Boolean practiceCompleted;
    private Boolean rewardGranted;
    private Boolean rewardAlreadyGranted;
    private StudentStatsResponse stats;

    private LocalDateTime updatedAt;

    public static PracticeProgressResponse from(
            PracticeProgress practiceProgress) {

        return new PracticeProgressResponse(
            practiceProgress.getId(),
            practiceProgress.getStudent().getId(),
            practiceProgress.getBookSelected(),
            practiceProgress.getBeforeDone(),
            practiceProgress.getClassReadDone(),
            practiceProgress.getAfterDone(),
            practiceProgress.getDuringTypeProgress(),
            isPracticeCompleted(practiceProgress),
            false,
            false,
            null,
            practiceProgress.getUpdatedAt()
        );
    }

    public static PracticeProgressResponse from(
            PracticeProgress practiceProgress,
            Boolean rewardGranted,
            Boolean rewardAlreadyGranted,
            StudentStatsResponse stats) {

        return new PracticeProgressResponse(
            practiceProgress.getId(),
            practiceProgress.getStudent().getId(),
            practiceProgress.getBookSelected(),
            practiceProgress.getBeforeDone(),
            practiceProgress.getClassReadDone(),
            practiceProgress.getAfterDone(),
            practiceProgress.getDuringTypeProgress(),
            isPracticeCompleted(practiceProgress),
            rewardGranted,
            rewardAlreadyGranted,
            stats,
            practiceProgress.getUpdatedAt()
        );
    }

    private static boolean isPracticeCompleted(PracticeProgress practiceProgress) {
        Map<String, Boolean> during = practiceProgress.getDuringTypeProgress();

        return Boolean.TRUE.equals(practiceProgress.getBookSelected())
            && Boolean.TRUE.equals(practiceProgress.getBeforeDone())
            && Boolean.TRUE.equals(practiceProgress.getClassReadDone())
            && Boolean.TRUE.equals(practiceProgress.getAfterDone())
            && during != null
            && Boolean.TRUE.equals(during.get("direct"))
            && Boolean.TRUE.equals(during.get("infer"))
            && Boolean.TRUE.equals(during.get("opinion"))
            && Boolean.TRUE.equals(during.get("connect"))
            && Boolean.TRUE.equals(during.get("review"));
    }
}
