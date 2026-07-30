package com.victory.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;

/*
 * 나의 독서 보관함 목록 카드 한 장. 대표 질문의 답은 카드 앞면에
 * 노출하지 않으므로 여기에는 포함하지 않는다(상세 조회에서만 제공).
 */
@Getter
@AllArgsConstructor
public class IndividualReadingArchiveItem {

    private Long readingRecordId;
    private Long bookId;
    private String bookTitle;
    private String author;
    private String coverImage;
    private LocalDateTime finishedAt;
    private Integer rating;
    private String representativeQuestion;
    private String representativeCategory;
}
