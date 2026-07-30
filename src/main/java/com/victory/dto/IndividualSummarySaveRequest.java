package com.victory.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

/*
 * 개별읽기 읽기 후 최종 간추리기 저장 요청(summaries 테이블 재사용,
 * reading_record_id로 연결). bookType은 학생이 읽기 후 화면에서 고른 책
 * 종류 스냅샷이다(온책읽기 AfterReadingSaveRequest와 같은 관례).
 */
@Getter
@NoArgsConstructor
public class IndividualSummarySaveRequest {

    @NotBlank
    private String bookType;

    @NotBlank
    private String summaryText;

    private Boolean aiPassed;
}
