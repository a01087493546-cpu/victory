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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/*
 * 학생의 책 단위 읽기 진행 기록(개별읽기 기준). 학생-책 조합마다 한 행.
 *
 * 주의(순환 참조): represent_response_id -> responses.id 이고
 * responses.reading_record_id -> reading_records.id 라서 두 테이블이 서로를 참조한다.
 * 두 컬럼 모두 NULL 허용이라 생성 자체엔 문제없지만, 실제 DDL 적용 순서(또는
 * ALTER TABLE로 나중에 FK 추가)를 고려해야 한다.
 */
@Entity
@Table(
    name = "reading_records",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_reading_records_student_book",
        columnNames = {"student_id", "book_id"}
    ),
    indexes = {
        @Index(name = "idx_reading_records_book_id", columnList = "book_id"),
        @Index(name = "idx_reading_records_finished_at", columnList = "finished_at")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReadingRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    /*
     * before / during / after
     */
    @Column(name = "current_stage", nullable = false, length = 20)
    private String currentStage = "before";

    @Column(name = "before_done", nullable = false)
    private Boolean beforeDone = false;

    @Column(name = "during_done", nullable = false)
    private Boolean duringDone = false;

    @Column(name = "after_done", nullable = false)
    private Boolean afterDone = false;

    /*
     * "지금까지 읽은 곳"의 최신 스냅샷. 날짜별 이력은 reading_progress_logs가 따로 관리하고,
     * 이 컬럼은 빠른 조회용 최신값이다.
     */
    @Column(name = "current_page")
    private Integer currentPage;

    @Column(name = "total_pages")
    private Integer totalPages;

    /*
     * 완독 후 별점(나의 독서 보관함 화면).
     */
    @Column(name = "rating")
    private Integer rating;

    /*
     * 완독 팝업에서 고른 대표 질문.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "represent_response_id")
    private Response representResponse;

    /*
     * 완독 시점에 확정 저장하는 스냅샷 점수(dashboard-metrics-audit.md §7 참고).
     * student_daily_metrics는 날짜마다 값이 바뀌는 진행 중 지표이고,
     * 이 두 컬럼은 "이 책을 완독했을 때의 최종 점수"를 영구히 남기는 기록용이라 서로 다른 목적이다.
     */
    @Column(name = "final_reading_practice_score")
    private Integer finalReadingPracticeScore;

    @Column(name = "final_record_completion_score")
    private Integer finalRecordCompletionScore;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

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
