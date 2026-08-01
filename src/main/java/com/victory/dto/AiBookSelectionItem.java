package com.victory.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/*
 * OpenAI 응답을 그대로 담는 내부 DTO. bookId 외의 title/author/description
 * 등은 AI가 되돌려주더라도 절대 신뢰하지 않고 무시한다 - 최종 응답은 항상
 * DB 후보 데이터(RecommendationCandidateItem)로만 다시 채운다.
 */
@Getter
@Setter
@NoArgsConstructor
public class AiBookSelectionItem {

    private Long bookId;
    private String recommendationReason;
}
