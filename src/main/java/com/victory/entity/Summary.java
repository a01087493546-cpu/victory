package com.victory.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/*
 * 간추리기(요약) 결과 저장.
 */
@Entity
@Table(
    name = "summaries",
    indexes = {
        @Index(name = "idx_summaries_student_id", columnList = "student_id"),
        @Index(name = "idx_summaries_status", columnList = "status")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Summary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id")
    private Book book;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reading_record_id")
    private ReadingRecord readingRecord;

    /*
     * 요약 당시 책 유형 스냅샷(책 유형이 나중에 바뀌어도 이 기록은 그대로 유지).
     */
    @Column(name = "book_type", length = 30)
    private String bookType;

    @Lob
    @Column(name = "summary_text", nullable = false, columnDefinition = "TEXT")
    private String summaryText;

    @Column(name = "is_shared", nullable = false)
    private Boolean isShared = false;

    /*
     * 교사 검수 상태: pending / approved / rejected
     */
    @Column(name = "status", nullable = false, length = 20)
    private String status = "approved";

    /*
     * FeedbackAiController(SYSTEM_PROMPT_SUMMARY)의 good/need 판정 결과.
     * good이면 true, need면 false, 아직 판정 전이면 NULL.
     */
    @Column(name = "ai_passed")
    private Boolean aiPassed;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
