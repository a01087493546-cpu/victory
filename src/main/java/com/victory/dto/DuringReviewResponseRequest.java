package com.victory.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

/*
 * 읽기 중(during-reading-practice.html) 총복습 질문·답 저장 요청.
 * AI가 good으로 판정한 직후에만 프론트에서 이 요청을 보낸다.
 */
@Getter
@NoArgsConstructor
public class DuringReviewResponseRequest {

    @NotBlank
    private String questionType;

    @NotBlank
    private String question;

    @NotBlank
    private String answer;

    @NotNull
    private Long classReadingBookId;
}
