package com.victory.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AiBookRecommendationItem {

    private Long bookId;
    private String title;
    private String author;
    private String description;
    private String coverImage;
    private int matchScore;
    private String recommendationReason;
}
