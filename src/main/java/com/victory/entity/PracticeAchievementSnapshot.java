package com.victory.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/*
 * 연습읽기 학급 성취도 그래프의 "그날 자정까지 누적된 값" 스냅샷.
 *
 * student_daily_metrics 테이블도 검토했으나, (1) class_reading_book_id가
 * 없어 "학급×책" 단위 UNIQUE 제약을 만들 수 없고, (2) scoreA/scoreB/
 * totalScore 3개의 정수 점수만 있어 이번에 요구되는 8개 지표(참여율 4종 +
 * 이해도 + 최종 달성도 + 최종 진행률 + AI평가 여부)를 담기에 구조가 전혀
 * 맞지 않으며, (3) 실제로 어떤 Repository/Service/Controller도 이 테이블을
 * 쓰고 있지 않아(완전 미사용) 재사용보다 새 테이블을 최소한으로 추가하는
 * 쪽이 기존 기능을 깨뜨릴 위험이 없다고 판단했다.
 */
@Entity
@Table(
    name = "practice_achievement_snapshots",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_achievement_snapshot_student_book_date",
            columnNames = { "student_id", "class_reading_book_id", "snapshot_date" }
        )
    },
    indexes = {
        @Index(
            name = "idx_achievement_snapshot_book_date",
            columnList = "class_reading_book_id, snapshot_date"
        )
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PracticeAchievementSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "class_reading_book_id", nullable = false)
    private Long classReadingBookId;

    /*
     * Asia/Seoul 기준 날짜(그날 자정 시점까지 누적된 값을 나타낸다).
     */
    @Column(name = "snapshot_date", nullable = false)
    private LocalDate snapshotDate;

    @Column(name = "reading_activity_completion_rate", nullable = false)
    private Double readingActivityCompletionRate;

    @Column(name = "question_participation_rate", nullable = false)
    private Double questionParticipationRate;

    @Column(name = "thought_sharing_participation_rate", nullable = false)
    private Double thoughtSharingParticipationRate;

    @Column(name = "participation_rate", nullable = false)
    private Double participationRate;

    @Column(name = "comprehension_rate", nullable = false)
    private Double comprehensionRate;

    @Column(name = "achievement_rate", nullable = false)
    private Double achievementRate;

    @Column(name = "final_reading_progress", nullable = false)
    private Double finalReadingProgress;

    @Column(name = "has_ai_evaluation", nullable = false)
    private Boolean hasAiEvaluation;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
