package com.victory.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class BookRecommendationClassWallResponse {

    private List<BookRecommendationItem> best;
    private List<BookRecommendationItem> recent;
}
