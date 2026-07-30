package com.victory.dto;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

/*
 * 우리 반 추천 책장 글 작성 요청. title/author는 과거 클라이언트 호환용으로
 * 남겨두지만, 새 저장 흐름에서는 readingRecordId로 서버가 책 정보를 직접 결정한다.
 */
@Getter
@NoArgsConstructor
public class BookRecommendationCreateRequest {

    @NotNull
    private Long readingRecordId;

    private String title;

    private String author;

    @NotBlank
    @Size(max = 2000)
    private String reason;

    private List<Long> teaserResponseIds;
}
