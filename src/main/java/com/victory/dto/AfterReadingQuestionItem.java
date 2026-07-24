package com.victory.dto;

import com.victory.entity.Response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AfterReadingQuestionItem {

    private Long id;
    private Integer index;
    private String question;
    private String answer;
    private Boolean aiPassed;

    public static AfterReadingQuestionItem from(Response response) {
        Integer index = null;
        String question = null;

        if (response.getExtraData() != null) {
            Object rawIndex = response.getExtraData().get("questionIndex");
            if (rawIndex instanceof Number number) {
                index = number.intValue();
            } else if (rawIndex != null) {
                try {
                    index = Integer.valueOf(rawIndex.toString());
                } catch (NumberFormatException ignored) {
                    index = null;
                }
            }

            Object rawQuestion = response.getExtraData().get("question");
            if (rawQuestion != null) {
                question = rawQuestion.toString();
            }
        }

        return new AfterReadingQuestionItem(
            response.getId(),
            index,
            question,
            response.getContent(),
            response.getPassed()
        );
    }
}
