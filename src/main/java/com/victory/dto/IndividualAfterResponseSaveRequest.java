package com.victory.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

/*
 * 개별읽기 읽기 후 간추리기 질문·답 한 세트(1/2/3) 저장 요청.
 * aiPassed/aiFeedback은 AI 확인받기(선택 도움 기능) 결과를 참고용으로만
 * 함께 저장하기 위한 값이고, 저장 자체를 막는 조건으로는 쓰이지 않는다.
 */
@Getter
@NoArgsConstructor
public class IndividualAfterResponseSaveRequest {

    @NotBlank
    private String question;

    @NotBlank
    private String answer;

    private Boolean aiPassed;

    private String aiFeedback;
}
