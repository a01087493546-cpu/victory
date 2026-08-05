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

    /*
     * 차례가 없는 책이라 학생이 "차례 없음"을 선택해 이 단계를 통과했는지
     * 여부. true면 question/answer는 실제 학생 작성값이 아니라 빈 값이다.
     */
    private boolean skipped;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static PreReadingResponseItem from(Response response) {

        Object stepType = response.getExtraData() == null
            ? null
            : response.getExtraData().get("stepType");

        Object question = response.getExtraData() == null
            ? null
            : response.getExtraData().get("question");

        Object skipped = response.getExtraData() == null
            ? null
            : response.getExtraData().get("skipped");

        return new PreReadingResponseItem(
            response.getId(),
            stepType == null ? null : stepType.toString(),
            question == null ? null : question.toString(),
            response.getContent(),
            response.getPassed(),
            Boolean.TRUE.equals(skipped),
            response.getCreatedAt(),
            response.getUpdatedAt()
        );
    }
}
