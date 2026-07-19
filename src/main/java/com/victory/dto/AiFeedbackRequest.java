package com.victory.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiFeedbackRequest {

    private String type;
    private String bookType;
    private List<QAItem> qaList;
    private String summaryText;

    /*
     * pre_reading_question 전용 필드.
     * 다른 활동 유형은 사용하지 않는다(항상 null).
     */
    private String bookTitle;
    private String stepType;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QAItem {
        private String question;
        private String answer;
    }
}
