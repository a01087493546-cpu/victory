package com.victory.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
 * 개별읽기 "나의 책 진행 상황"의 일별 진행 기록.
 * reading_records.current_page/total_pages는 "최신 스냅샷"이고, 이 테이블이 날짜별 이력을 맡는다
 * (dashboard-metrics-audit.md §9 참고). 학생이 같은 날 여러 번 저장하면 그날 행을 덮어쓰고(UPSERT),
 * 날짜가 바뀌면 새 행이 생긴다.
 */
@Entity
@Table(
    name = "reading_progress_logs",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_progress_logs_student_record_date",
        columnNames = {"student_id", "reading_record_id", "log_date"}
    )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReadingProgressLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reading_record_id", nullable = false)
    private ReadingRecord readingRecord;

    @Column(name = "log_date", nullable = false)
    private LocalDate logDate;

    /*
     * 그날 기준 누적 읽은 쪽수("오늘 읽은 곳").
     */
    @Column(name = "cumulative_page", nullable = false)
    private Integer cumulativePage;

    /*
     * 그 시점의 전체 쪽수 스냅샷(책마다 다를 수 있어 매 기록에 함께 저장).
     */
    @Column(name = "total_pages", nullable = false)
    private Integer totalPages;

    /*
     * 저장 시점에 계산해서 캐시: ROUND(cumulative_page / total_pages * 100)
     */
    @Column(name = "progress_percent", nullable = false)
    private Integer progressPercent;

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
