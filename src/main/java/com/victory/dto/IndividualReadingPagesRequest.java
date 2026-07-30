package com.victory.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;

/*
 * 개별읽기 쪽수 수정 요청
 * (PUT /api/students/me/individual-reading/{readingRecordId}/pages).
 * currentPage <= totalPages 검증은 두 필드를 함께 봐야 하므로 서비스에서
 * 처리한다(Bean Validation 단일 필드 검증만으로는 표현할 수 없음).
 */
@Getter
@NoArgsConstructor
public class IndividualReadingPagesRequest {

    @NotNull
    @Positive
    private Integer totalPages;

    @NotNull
    @Min(0)
    private Integer currentPage;
}
