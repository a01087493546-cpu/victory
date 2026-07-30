package com.victory.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

/*
 * 읽은 책 종류 통계 (GET .../book-type-stats). 완독(finished_at IS NOT NULL)한
 * reading_records만 집계 대상이다. DB의 books.book_type 값(story/info/opinion/etc)을
 * 화면 표시용 4종(story/information/argument/other)으로 매핑해서 응답한다.
 */
@Getter
@AllArgsConstructor
public class BookTypeStatsResponse {

    private int totalCompletedBooks;
    private int story;
    private int information;
    private int argument;
    private int other;
    private Percentages percentages;

    @Getter
    @AllArgsConstructor
    public static class Percentages {
        private double story;
        private double information;
        private double argument;
        private double other;
    }
}
