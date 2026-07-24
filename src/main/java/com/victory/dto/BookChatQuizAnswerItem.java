package com.victory.dto;

import java.time.LocalDateTime;
import java.util.Map;

import com.victory.entity.Response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class BookChatQuizAnswerItem {

    private Long id;
    private Long questionResponseId;
    private Long studentId;
    private String studentName;
    private boolean mine;
    private String answer;
    private LocalDateTime createdAt;

    public static BookChatQuizAnswerItem from(Response response, Long viewerStudentId) {

        Map<String, Object> extraData = response.getExtraData();

        return new BookChatQuizAnswerItem(
            response.getId(),
            BookChatThoughtItem.toLong(
                extraData == null ? null : extraData.get("questionResponseId")
            ),
            response.getStudent() == null ? null : response.getStudent().getId(),
            response.getStudent() == null ? null : response.getStudent().getName(),
            response.getStudent() != null
                && response.getStudent().getId().equals(viewerStudentId),
            response.getContent(),
            response.getCreatedAt()
        );
    }
}
