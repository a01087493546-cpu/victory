package com.victory.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class BookRecommendationLikeResponse {

    private Long recommendationId;
    private boolean liked;
    private long likeCount;
}
