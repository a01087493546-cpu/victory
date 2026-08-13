package com.victory.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

/*
 * saveMyAfterReadingData(최종 완료)와 달리, 질문 1개가 루미 피드백을
 * 통과한 시점에 그 질문 하나만 자동저장하기 위한 요청이다 - 나머지 두
 * 질문이나 간추리기가 아직 없어도 저장할 수 있다.
 */
@Getter
@NoArgsConstructor
public class AfterReadingQuestionSaveRequest {

    @NotNull
    private Long classReadingBookId;

    @NotBlank
    private String bookType;

    @NotNull
    @Min(1)
    @Max(3)
    private Integer index;

    @NotBlank
    private String question;

    @NotBlank
    private String answer;
}
