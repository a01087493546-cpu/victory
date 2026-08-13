package com.victory.dto;

import java.time.LocalDateTime;
import java.util.Map;

import com.victory.entity.Response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class BookChatThoughtItem {

    private Long id;
    private Long questionResponseId;
    private Long studentId;
    private String studentName;
    private boolean mine;
    private String main;
    private String reason;
    private LocalDateTime createdAt;
    // 작성자가 교사면 "teacher", 학생이면 "student" - 프론트가 이 값으로만
    // "선생님" 배지를 붙인다(이름만으로 구분하지 않는다).
    private String authorRole;

    public static BookChatThoughtItem from(Response response, Long viewerStudentId) {

        Map<String, Object> extraData = response.getExtraData();

        return new BookChatThoughtItem(
            response.getId(),
            toLong(extraData == null ? null : extraData.get("questionResponseId")),
            response.getStudent() == null ? null : response.getStudent().getId(),
            response.getStudent() == null ? null : response.getStudent().getName(),
            response.getStudent() != null
                && response.getStudent().getId().equals(viewerStudentId),
            response.getContent(),
            extraData == null || extraData.get("reason") == null
                ? ""
                : extraData.get("reason").toString(),
            response.getCreatedAt(),
            response.getStudent() == null ? null : response.getStudent().getRole()
        );
    }

    public static Long toLong(Object value) {

        if (value == null) {
            return null;
        }

        if (value instanceof Number number) {
            return number.longValue();
        }

        try {
            return Long.valueOf(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
