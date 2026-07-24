package com.victory.dto;

import java.time.LocalDateTime;

import com.victory.entity.ClassStudent;
import com.victory.entity.Summary;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AfterReadingSummaryItem {

    private Long id;
    private Long studentId;
    private String studentName;
    private Integer studentNumber;
    private String bookType;
    private String summary;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private long likeCount;
    private boolean likedByMe;
    private boolean mine;

    public static AfterReadingSummaryItem from(
            Summary summary,
            ClassStudent classStudent,
            long likeCount,
            boolean likedByMe,
            Long viewerStudentId) {

        Long writerId = summary.getStudent().getId();

        return new AfterReadingSummaryItem(
            summary.getId(),
            writerId,
            summary.getStudent().getName(),
            classStudent == null ? null : classStudent.getStudentNumber(),
            summary.getBookType(),
            summary.getSummaryText(),
            summary.getCreatedAt(),
            summary.getUpdatedAt(),
            likeCount,
            likedByMe,
            viewerStudentId != null && viewerStudentId.equals(writerId)
        );
    }
}
