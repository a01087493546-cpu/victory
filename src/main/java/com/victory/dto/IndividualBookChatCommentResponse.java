package com.victory.dto;

import java.time.LocalDateTime;

import com.victory.entity.Response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class IndividualBookChatCommentResponse {

    private Long id;
    private String writer;
    private Long postId;
    private String choice;
    private String content;
    private LocalDateTime createdAt;
    private boolean rewardGranted;
    private StudentStatsResponse stats;
    private String authorRole;
    private Long studentId;
    private boolean mine;

    public static IndividualBookChatCommentResponse fromComment(Response response, String choice) {
        return fromComment(response, choice, null);
    }

    public static IndividualBookChatCommentResponse fromComment(Response response, String choice, Long viewerId) {
        return new IndividualBookChatCommentResponse(
            response.getId(),
            response.getStudent().getName(),
            response.getParent() == null ? null : response.getParent().getId(),
            choice,
            response.getContent(),
            response.getCreatedAt(),
            false,
            null,
            response.getStudent().getRole(),
            response.getStudent().getId(),
            viewerId != null && viewerId.equals(response.getStudent().getId())
        );
    }
}
