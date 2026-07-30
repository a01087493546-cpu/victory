package com.victory.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;

/*
 * 개별읽기 읽기 전/중/후 질문·답 한 건. 대표 질문 후보 조회(finish-candidates)와
 * 나의 독서 보관함 상세 조회에서 함께 재사용한다.
 *
 * stepType(읽기 전)/questionType(읽기 중)/questionIndex(읽기 후)는 해당
 * 단계에서만 값이 채워지고 나머지는 null이다. passed는 읽기 전·중
 * 응답에서는 항상 true로 채워진다 - 저장 자체가 "AI가 이미 통과시킨
 * 답만 프론트에서 저장을 호출한다"는 기존 관례를 전제로 하고, DB에는
 * 판정 결과를 별도로 남기지 않기 때문이다(읽기 후만 실제 passed 컬럼값을
 * 그대로 반영).
 */
@Getter
@AllArgsConstructor
public class IndividualQaRecordItem {

    private Long responseId;
    private String question;
    private String answer;
    private Boolean passed;
    private String aiFeedback;
    private String stepType;
    private String questionType;
    private Integer questionIndex;
    private LocalDateTime createdAt;
}
