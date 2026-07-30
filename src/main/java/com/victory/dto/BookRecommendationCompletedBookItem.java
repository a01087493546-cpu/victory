package com.victory.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class BookRecommendationCompletedBookItem {

    private Long readingRecordId;
    private String bookTitle;
    private String author;
    private LocalDateTime finishedAt;
}
