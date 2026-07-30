package com.victory.dto;

import java.util.List;

import com.victory.entity.RecommendationBook;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RecommendationBookItem {

    private Long id;
    private String title;
    private String author;
    private String description;
    private String coverImage;
    private String thickness;
    private String mood;
    private String genre;
    private String illustrationLevel;
    private String difficulty;
    private List<String> purposeTags;
    private Integer recommendedGradeMin;
    private Integer recommendedGradeMax;

    public static RecommendationBookItem from(RecommendationBook book) {
        return new RecommendationBookItem(
            book.getId(),
            book.getTitle(),
            book.getAuthor(),
            book.getDescription(),
            book.getCoverImage(),
            book.getThickness(),
            book.getMood(),
            book.getGenre(),
            book.getIllustrationLevel(),
            book.getDifficulty(),
            book.getPurposeTags() == null ? List.of() : book.getPurposeTags(),
            book.getRecommendedGradeMin(),
            book.getRecommendedGradeMax()
        );
    }
}
