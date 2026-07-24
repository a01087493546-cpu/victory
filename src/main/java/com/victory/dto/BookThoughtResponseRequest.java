package com.victory.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;

/*
 * 읽기 중(during-read.html) "책 속 생각 쓰기" 저장 요청.
 * AI가 good으로 판정한 직후에만 프론트에서 이 요청을 보낸다.
 * 유형별 upsert(during-practice/during-review)와 달리, 이 화면은 학생이
 * 하루에 여러 개를 만들 수 있으므로 매번 새 기록으로 저장한다.
 */
@Getter
@NoArgsConstructor
public class BookThoughtResponseRequest {

    @NotBlank
    @Pattern(
        regexp = "direct|infer|opinion|connect",
        message = "questionType은 direct, infer, opinion, connect 중 하나여야 합니다."
    )
    private String questionType;

    @NotBlank
    private String question;

    @NotBlank
    private String answer;

    @NotNull
    private Long classReadingBookId;
}
