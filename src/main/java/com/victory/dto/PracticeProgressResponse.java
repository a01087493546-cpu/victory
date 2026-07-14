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
            practiceProgress.getUpdatedAt()
        );
    }
}