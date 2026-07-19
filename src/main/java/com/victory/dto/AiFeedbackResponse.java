package com.victory.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiFeedbackResponse {

    private String status;
    private String message;

    /*
     * pre_reading_question 전용 필드.
     * 다른 활동 유형의 응답에는 항상 null이다(기존 status/message만 사용).
     */
    private String result;
    private String failedRule;
}
