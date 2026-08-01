package com.victory.dto;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

/*
 * AI 책 추천 전용 요청 DTO. 기존 태그 점수 후보 API(/recommendation-books/candidates,
 * BookPreferenceRequest)와는 별도로 분리한다 - excludedBookIds(이전에 이미 보여준
 * 책 제외 목록)는 AI 추천에서만 필요한 개념이라 기존 후보 API DTO에는 넣지 않는다.
 * excludedBookIds는 선택 사항이며, 서비스 계층에서 null/음수/중복/최대 개수를
 * 안전하게 정리한다(여기서는 형식 검증만 한다).
 */
@Getter
@NoArgsConstructor
public class AiBookRecommendationRequest {

    @NotBlank
    private String thickness;

    @NotBlank
    private String mood;

    @NotBlank
    private String genre;

    @NotBlank
    private String illustrationLevel;

    @NotBlank
    private String difficulty;

    @NotBlank
    private String purpose;

    private List<Long> excludedBookIds;
}
