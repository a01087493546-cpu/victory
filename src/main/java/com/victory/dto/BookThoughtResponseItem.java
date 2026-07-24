package com.victory.dto;

import java.time.LocalDateTime;

import com.victory.entity.Response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class BookThoughtResponseItem {

    private Long id;
    private String questionType;
    private String question;
    private String answer;
    private String approvalStatus;
    private String rejectionReason;
    private Long studentId;
    private String studentName;
    private Long classReadingBookId;
    private LocalDateTime createdAt;

    public static BookThoughtResponseItem from(Response response) {

        java.util.Map<String, Object> extraData = response.getExtraData();

        Object questionType = extraData == null ? null : extraData.get("questionType");
        Object question = extraData == null ? null : extraData.get("question");
        Object approvalStatus = extraData == null ? null : extraData.get("approvalStatus");
        Object rejectionReason = extraData == null ? null : extraData.get("rejectionReason");
        Object classReadingBookId = extraData == null ? null : extraData.get("classReadingBookId");

        return new BookThoughtResponseItem(
            response.getId(),
            questionType == null ? null : questionType.toString(),
            question == null ? null : question.toString(),
            response.getContent(),
            approvalStatus == null ? "PENDING" : approvalStatus.toString(),
            rejectionReason == null ? null : rejectionReason.toString(),
            response.getStudent() == null ? null : response.getStudent().getId(),
            response.getStudent() == null ? null : response.getStudent().getName(),
            toLong(classReadingBookId),
            response.getCreatedAt()
        );
    }

    private static Long toLong(Object value) {

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
