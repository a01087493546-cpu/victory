package com.victory.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RecommendationCandidateItem {

    private RecommendationBookItem book;
    private int matchScore;
}
