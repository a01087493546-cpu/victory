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

    /*
     * extra_practice(읽기 후 "질문으로 간추리기") 전용 필드.
     * 화면에 실제로 제시된 예시 글 전체. 다른 활동 유형은 사용하지 않는다.
     */
    private String passage;

    /*
     * 더 이상 어디서도 읽지 않는 필드다(과거에는 시도 기록에 썼으나, 인증
     * 없는 공개 요청의 studentId는 위조 가능해서 신뢰할 수 없다는 문제가
     * 있었다). AI 평가 시도 기록이 필요한 화면은 대신 인증된 전용 엔드포인트
     * (/api/students/me/feedback/ai-review, StudentAiFeedbackRequest)를
     * 쓰고, 거기서는 JWT의 로그인 사용자 ID만 신뢰한다. 이 필드는 혹시 옛
     * 방식으로 studentId를 보내는 호출자가 있어도 JSON 역직렬화가 깨지지
     * 않도록 남겨두었을 뿐, 값이 와도 완전히 무시된다.
     */
    private Long studentId;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QAItem {
        private String question;
        private String answer;
    }
}
