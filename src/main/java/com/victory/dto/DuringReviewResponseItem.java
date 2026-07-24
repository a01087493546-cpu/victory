package com.victory.dto;

import java.time.LocalDateTime;

import com.victory.entity.Response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class DuringReviewResponseItem {

    private Long id;
    private String questionType;
    private String question;
    private String answer;
    private Boolean passed;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static DuringReviewResponseItem from(Response response) {

        Object questionType = response.getExtraData() == null
            ? null
            : response.getExtraData().get("questionType");

        Object question = response.getExtraData() == null
            ? null
            : response.getExtraData().get("question");

        return new DuringReviewResponseItem(
            response.getId(),
            questionType == null ? null : questionType.toString(),
            question == null ? null : question.toString(),
            response.getContent(),
            response.getPassed(),
            response.getCreatedAt(),
            response.getUpdatedAt()
        );
    }
}
