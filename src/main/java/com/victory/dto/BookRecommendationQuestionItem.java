package com.victory.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class BookRecommendationQuestionItem {

    private Long responseId;
    private String category;
    private String categoryLabel;
    private String detailLabel;
    private String question;
}
