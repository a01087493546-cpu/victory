package com.victory.dto;

import java.time.LocalDateTime;
import java.util.Map;

import com.victory.entity.Response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class BookChatReplyItem {

    private Long id;
    private Long questionResponseId;
    private Long parentThoughtId;
    private Long studentId;
    private String studentName;
    private boolean mine;
    private String replyType;
    private String text;
    private LocalDateTime createdAt;

    public static BookChatReplyItem from(Response response, Long viewerStudentId) {

        Map<String, Object> extraData = response.getExtraData();

        return new BookChatReplyItem(
            response.getId(),
            BookChatThoughtItem.toLong(
                extraData == null ? null : extraData.get("questionResponseId")
            ),
            response.getParent() == null ? null : response.getParent().getId(),
            response.getStudent() == null ? null : response.getStudent().getId(),
            response.getStudent() == null ? null : response.getStudent().getName(),
            response.getStudent() != null
                && response.getStudent().getId().equals(viewerStudentId),
            extraData == null || extraData.get("replyType") == null
                ? ""
                : extraData.get("replyType").toString(),
            response.getContent(),
            response.getCreatedAt()
        );
    }
}
