package com.victory.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

/*
 * 개별읽기 읽기 후 간추리기 질문·답 한 세트(1/2/3) 저장 요청.
 * IndividualBeforeResponseSaveRequest와 달리 aiPassed/aiFeedback을 함께 받는다 -
 * 읽기 후 완료 조건이 "3세트 모두 AI 통과"라서 서버가 재검증할 근거가 필요하기 때문이다.
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
