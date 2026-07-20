package com.victory.entity;

import java.time.LocalDate;
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
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/*
 * 교사 대시보드 "학생별 달성도"의 일별 스냅샷. 매일 자정 배치로 그날까지의 누적 점수를 기록한다
 * (dashboard-metrics-audit.md §4/§10-4 참고).
 *
 * 온책읽기(참여도/이해도)와 개별읽기(독서실천도/기록완성도)가 같은 구조를 공유하도록
 * score_a/score_b라는 범용 이름을 쓰고, metric_type으로 두 도메인을 구분한다.
 */
@Entity
@Table(
    name = "student_daily_metrics",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_daily_metrics_student_type_date",
        columnNames = {"student_id", "metric_type", "metric_date"}
    ),
    indexes = {
        @Index(name = "idx_daily_metrics_class_id", columnList = "class_id"),
        @Index(name = "idx_daily_metrics_metric_date", columnList = "metric_date")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StudentDailyMetric {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    /*
     * 개별읽기 집계일 때는 NULL(학생마다 책이 달라 학급 단위 의미가 없음).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_id")
    private SchoolClass schoolClass;

    /*
     * class_reading(온책읽기) / individual_reading(개별읽기)
     */
    @Column(name = "metric_type", nullable = false, length = 20)
    private String metricType;

    @Column(name = "metric_date", nullable = false)
    private LocalDate metricDate;

    /*
     * 온책읽기: 참여도 / 개별읽기: 독서실천도
     */
    @Column(name = "score_a", nullable = false)
    private Integer scoreA;

    /*
     * 온책읽기: 이해도 / 개별읽기: 기록완성도
     */
    @Column(name = "score_b", nullable = false)
    private Integer scoreB;

    @Column(name = "total_score", nullable = false)
    private Integer totalScore;

    /*
     * 그날 발생한 활동(응답/요약 등) 건수. "오늘 참여율" 계산에 재사용.
     */
    @Column(name = "activity_count_today", nullable = false)
    private Integer activityCountToday = 0;

    /*
     * 교사가 학생별 상세 화면에서 남기는 참고 코멘트.
     */
    @Lob
    @Column(name = "teacher_comment", columnDefinition = "TEXT")
    private String teacherComment;

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
