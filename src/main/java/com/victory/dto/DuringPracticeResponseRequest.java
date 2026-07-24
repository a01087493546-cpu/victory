package com.victory.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

/*
 * 읽기 중(during-reading-practice.html) 유형별 심화 연습 질문 저장 요청.
 * 이 화면은 학생이 질문만 만들고 답은 입력하지 않으므로 answer 필드가 없다.
 * AI가 good으로 판정한 직후에만 프론트에서 이 요청을 보낸다.
 */
@Getter
@NoArgsConstructor
public class DuringPracticeResponseRequest {

    @NotBlank
    private String questionType;

    @NotBlank
    private String question;

    @NotNull
    private Long classReadingBookId;
}
