package com.victory.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class IndividualSummaryLikeResponse {

    private Long summaryId;
    private boolean liked;
    private long likeCount;
}
