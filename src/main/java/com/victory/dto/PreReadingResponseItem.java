package com.victory.dto;

import java.time.LocalDateTime;

import com.victory.entity.Response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PreReadingResponseItem {

    private Long id;
    private String stepType;
    private String question;
    private String answer;
    private Boolean passed;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static PreReadingResponseItem from(Response response) {

        Object stepType = response.getExtraData() == null
            ? null
            : response.getExtraData().get("stepType");

        Object question = response.getExtraData() == null
            ? null
            : response.getExtraData().get("question");

        return new PreReadingResponseItem(
            response.getId(),
            stepType == null ? null : stepType.toString(),
            question == null ? null : question.toString(),
            response.getContent(),
            response.getPassed(),
            response.getCreatedAt(),
            response.getUpdatedAt()
        );
    }
}
