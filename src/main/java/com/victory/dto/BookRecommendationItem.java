package com.victory.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.victory.entity.BookRecommendation;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

/*
 * 우리 반 추천 책장 카드 하나. likeCount/likedByMe/isMine은 조회하는
 * 사용자 기준으로 매번 새로 계산한다(정적 데이터 아님). rank는 BEST
 * 목록에서만 채워지고 recent 목록에서는 null(응답에서 생략)이다.
 *
 * isMine 필드는 @Getter(AccessLevel.NONE) + 수동 getIsMine()으로 처리한다.
 * Lombok이 boolean 필드 isMine에 대해 isMine() 게터를 만들면, Jackson이
 * 필드 자체("isMine")와 getter 기반 프로퍼티("mine", "is" 접두사 제거)를
 * 서로 다른 프로퍼티로 인식해 응답에 isMine/mine이 중복으로 나온다
 * (필드에 @JsonProperty만 붙이는 방식으로는 이 중복을 막지 못했다 -
 * IndividualSummaryShareItem에서 같은 방식을 썼을 때도 실제로는 중복이
 * 발생했을 가능성이 있다). getIsMine() 하나만 노출해 이름 충돌을 없앤다.
 */
@Getter
@AllArgsConstructor
public class BookRecommendationItem {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Integer rank;

    private Long recommendationId;
    private Long studentId;
    private String studentName;
    private String title;
    private String author;
    private String reason;
    private List<String> teaserQuestions;
    private LocalDateTime createdAt;
    private long likeCount;
    private boolean likedByMe;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Boolean rewardGranted;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Integer courage;

    @Getter(AccessLevel.NONE)
    private boolean isMine;

    @JsonProperty("isMine")
    public boolean getIsMine() {
        return isMine;
    }

    public static BookRecommendationItem of(
            BookRecommendation recommendation,
            long likeCount,
            boolean likedByMe,
            boolean isMine) {
        return of(recommendation, List.of(), likeCount, likedByMe, isMine);
    }

    public static BookRecommendationItem of(
            BookRecommendation recommendation,
            List<String> teaserQuestions,
            long likeCount,
            boolean likedByMe,
            boolean isMine) {

        return new BookRecommendationItem(
            null,
            recommendation.getId(),
            recommendation.getStudent().getId(),
            recommendation.getStudent().getName(),
            recommendation.getTitle(),
            recommendation.getAuthor(),
            recommendation.getReason(),
            teaserQuestions == null ? List.of() : teaserQuestions,
            recommendation.getCreatedAt(),
            likeCount,
            likedByMe,
            null,
            null,
            isMine
        );
    }

    public BookRecommendationItem withReward(boolean rewardGranted, Integer courage) {
        return new BookRecommendationItem(
            rank,
            recommendationId,
            studentId,
            studentName,
            title,
            author,
            reason,
            teaserQuestions,
            createdAt,
            likeCount,
            likedByMe,
            rewardGranted,
            courage,
            isMine
        );
    }

    public BookRecommendationItem withRank(int rank) {
        return new BookRecommendationItem(
            rank,
            recommendationId,
            studentId,
            studentName,
            title,
            author,
            reason,
            teaserQuestions,
            createdAt,
            likeCount,
            likedByMe,
            rewardGranted,
            courage,
            isMine
        );
    }
}
