package com.victory.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

/*
 * 연습읽기 읽기 전(제목/차례/그림/글) 질문·답 저장 요청.
 * AI가 good으로 판정한 직후에만 프론트에서 이 요청을 보낸다.
 */
@Getter
@NoArgsConstructor
public class PreReadingResponseRequest {

    @NotBlank
    private String stepType;

    @NotBlank
    private String question;

    @NotBlank
    private String answer;

    @NotNull
    private Long classReadingBookId;
}
