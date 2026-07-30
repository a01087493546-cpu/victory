package com.victory.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

/*
 * 개별읽기 읽기 전 한 단계(title/contents/picture/skim)의 질문·답 저장
 * 요청. AI가 good으로 판정한 직후에만 프론트에서 이 요청을 보낸다.
 * AI 판정 결과(good/need)·피드백 문구·evaluationKey는 이 DTO에도,
 * DB에도 저장하지 않는다.
 */
@Getter
@NoArgsConstructor
public class IndividualBeforeResponseSaveRequest {

    @NotBlank
    private String question;

    @NotBlank
    private String answer;
}
