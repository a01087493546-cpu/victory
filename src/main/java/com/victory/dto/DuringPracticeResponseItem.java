package com.victory.dto;

import java.time.LocalDateTime;

import com.victory.entity.Response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class DuringPracticeResponseItem {

    private Long id;
    private String questionType;
    private String question;
    private Boolean passed;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static DuringPracticeResponseItem from(Response response) {

        Object questionType = response.getExtraData() == null
            ? null
            : response.getExtraData().get("questionType");

        return new DuringPracticeResponseItem(
            response.getId(),
            questionType == null ? null : questionType.toString(),
            response.getContent(),
            response.getPassed(),
            response.getCreatedAt(),
            response.getUpdatedAt()
        );
    }
}
