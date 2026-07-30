package com.victory.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

/*
 * 개별읽기 "이 책 마무리하기" 요청. representativeResponseId는 실제
 * responses.id를 가리키고, 질문·답 텍스트 자체는 서버가 그 응답을
 * 조회해서 채운다(프론트가 텍스트를 임의로 보내지 않는다).
 */
@Getter
@NoArgsConstructor
public class IndividualReadingFinishRequest {

    @NotNull
    @Min(1)
    @Max(5)
    private Integer rating;

    @NotNull
    private Long representativeResponseId;
}
