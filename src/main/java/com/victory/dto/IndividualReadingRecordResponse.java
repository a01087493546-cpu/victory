package com.victory.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;

/*
 * 개별읽기 독서 기록(reading_record) 한 건에 대한 응답. 책 등록/진행 중 책
 * 조회/쪽수 수정/완독 처리 API가 모두 이 형태로 응답해 프론트가 하나의
 * 구조로 일관되게 다룰 수 있게 한다.
 */
@Getter
@AllArgsConstructor
public class IndividualReadingRecordResponse {

    private Long readingRecordId;
    private Long bookId;

    private String title;
    private String author;
    private String bookType;
    private String coverImage;

    private Integer totalPages;
    private Integer currentPage;

    private String currentStage;
    private Boolean beforeDone;
    private Boolean duringDone;
    private Boolean afterDone;

    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;

    private Boolean completed;

    /*
     * 오늘(Asia/Seoul) 읽은 쪽수(당일 증가분) · 진행률 · 이 응답이 기준으로 삼은
     * 날짜. 같은 날 여러 번 저장해도 currentPage - 직전 날짜 누적 쪽수로만
     * 계산해서 중복 합산되지 않는다.
     */
    private Integer todayReadPages;
    private Integer progressPercent;
    private LocalDate readingDate;
}
