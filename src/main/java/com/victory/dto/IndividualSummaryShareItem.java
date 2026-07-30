package com.victory.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.victory.entity.Summary;

import lombok.AllArgsConstructor;
import lombok.Getter;

/*
 * 개별읽기 "우리 반 간추리기 모음"(학생) / 간추리기 확인(교사) 화면의
 * 카드 하나. likeCount/likedByMe/isMine은 조회하는 사용자 기준으로
 * 매번 새로 계산해서 채운다(정적 데이터가 아님).
 *
 * isMine 필드는 주의가 필요하다: Lombok이 boolean 필드 isMine에 대해
 * isMine() 게터를 만들면, Jackson은 "is" 접두사를 뗀 "mine"을 JSON
 * 키로 쓴다(likedByMe처럼 "is"로 시작하지 않는 필드는 이 문제가 없다).
 * 프론트가 기대하는 "isMine" 키를 그대로 유지하기 위해 명시적으로
 * @JsonProperty를 지정한다.
 */
@Getter
@AllArgsConstructor
public class IndividualSummaryShareItem {

    private Long summaryId;
    private Long readingRecordId;
    private Long studentId;
    private String studentName;
    private Long bookId;
    private String bookTitle;
    private String bookType;
    private String summaryText;
    private LocalDateTime createdAt;
    private long likeCount;
    private boolean likedByMe;

    @JsonProperty("isMine")
    private boolean isMine;

    public static IndividualSummaryShareItem of(
            Summary summary,
            String studentName,
            long likeCount,
            boolean likedByMe,
            boolean isMine) {

        return new IndividualSummaryShareItem(
            summary.getId(),
            summary.getReadingRecord().getId(),
            summary.getStudent().getId(),
            studentName,
            summary.getReadingRecord().getBook().getId(),
            summary.getReadingRecord().getBook().getTitle(),
            summary.getBookType(),
            summary.getSummaryText(),
            summary.getCreatedAt(),
            likeCount,
            likedByMe,
            isMine
        );
    }
}
