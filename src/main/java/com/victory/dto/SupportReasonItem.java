package com.victory.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

/*
 * 확인이 필요한 학생 화면에 표시할 지원 사유 코드 + 화면 문구.
 */
@Getter
@AllArgsConstructor
public class SupportReasonItem {

    private String code;
    private String label;

    public static final SupportReasonItem LOW_READING_PARTICIPATION =
        new SupportReasonItem("LOW_READING_PARTICIPATION", "읽기 활동 참여 부족");

    public static final SupportReasonItem NO_QUESTION_SUBMISSION =
        new SupportReasonItem("NO_QUESTION_SUBMISSION", "질문 미제출");

    public static final SupportReasonItem NO_THOUGHT_SHARING =
        new SupportReasonItem("NO_THOUGHT_SHARING", "생각 나누기 참여 부족");

    public static final SupportReasonItem LOW_COMPREHENSION =
        new SupportReasonItem("LOW_COMPREHENSION", "이해도 낮음");
}
