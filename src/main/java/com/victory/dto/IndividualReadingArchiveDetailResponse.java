package com.victory.dto;

import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class IndividualReadingArchiveDetailResponse {

    private Long readingRecordId;
    private Long bookId;
    private String bookTitle;
    private String author;
    private String coverImage;
    private LocalDateTime finishedAt;
    private Integer rating;
    private String representativeQuestion;
    private String representativeAnswer;
    private String representativeCategory;
    private List<IndividualQaRecordItem> beforeResponses;
    private List<IndividualQaRecordItem> duringResponses;
    private List<IndividualQaRecordItem> afterResponses;
    private String summaryText;
    private Boolean summaryAiPassed;
}
