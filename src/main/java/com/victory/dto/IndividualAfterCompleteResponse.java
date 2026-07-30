package com.victory.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class IndividualAfterCompleteResponse {

    private Long readingRecordId;
    private Boolean afterDone;
    private String currentStage;
    private boolean rewardGranted;
    private StudentStatsResponse stats;
}
