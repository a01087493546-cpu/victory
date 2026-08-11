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
    private Map<String, Boolean> afterTypeProgress;

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
            resolveAfterTypeProgress(practiceProgress),
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
            resolveAfterTypeProgress(practiceProgress),
            isPracticeCompleted(practiceProgress),
            rewardGranted,
            rewardAlreadyGranted,
            stats,
            practiceProgress.getUpdatedAt()
        );
    }

    /*
     * after_type_progress 컬럼은 기존 행에는(추가 직후) NULL이므로
     * 기본값(전부 false)으로 대체해서 내려준다.
     */
    private static Map<String, Boolean> resolveAfterTypeProgress(PracticeProgress practiceProgress) {
        Map<String, Boolean> progress = practiceProgress.getAfterTypeProgress();
        return progress != null ? progress : PracticeProgress.createDefaultAfterProgress();
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
