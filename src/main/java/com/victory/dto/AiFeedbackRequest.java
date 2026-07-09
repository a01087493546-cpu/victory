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

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QAItem {
        private String question;
        private String answer;
    }
}
