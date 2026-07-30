package com.victory.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class IndividualReadingFinishResponse {

    private Long readingRecordId;
    private boolean finished;
    private LocalDateTime finishedAt;
    private Integer rating;
    private String representativeQuestion;
    private String representativeAnswer;
    private String representativeCategory;
}
