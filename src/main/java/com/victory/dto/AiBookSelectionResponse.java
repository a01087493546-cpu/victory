package com.victory.dto;

import java.util.List;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/*
 * OpenAI 응답 JSON({"selections": [...]}) 파싱 전용 내부 DTO.
 * 프론트로 그대로 나가지 않고, AiBookRecommendationService의 검증을
 * 거친 뒤 AiBookRecommendationItem으로 다시 조립된다.
 */
@Getter
@Setter
@NoArgsConstructor
public class AiBookSelectionResponse {

    private List<AiBookSelectionItem> selections;
}
