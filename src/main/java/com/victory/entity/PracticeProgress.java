package com.victory.entity;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "practice_progress")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PracticeProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /*
     * 학생 한 명당 연습읽기 진행 상태가 한 줄만 존재하므로
     * User와 1:1 관계로 연결한다.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "student_id",
        nullable = false,
        unique = true
    )
    private User student;

    @Column(name = "book_selected", nullable = false)
    private Boolean bookSelected = false;

    @Column(name = "before_done", nullable = false)
    private Boolean beforeDone = false;

    @Column(name = "class_read_done", nullable = false)
    private Boolean classReadDone = false;

    @Column(name = "after_done", nullable = false)
    private Boolean afterDone = false;

    /*
     * 질문 유형별 완료 상태를 MySQL JSON 컬럼에 저장한다.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "during_type_progress", columnDefinition = "json")
    private Map<String, Boolean> duringTypeProgress
        = createDefaultDuringProgress();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /*
     * @PrePersist:
     * 데이터가 처음 저장되기 직전에 실행된다.
     */
    @PrePersist
    protected void onCreate() {
        this.updatedAt = LocalDateTime.now();

        if (this.duringTypeProgress == null) {
            this.duringTypeProgress = createDefaultDuringProgress();
        }
    }

    /*
     * @PreUpdate:
     * 기존 데이터가 수정되기 직전에 실행된다.
     */
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public static Map<String, Boolean> createDefaultDuringProgress() {
        Map<String, Boolean> progress = new HashMap<>();

        progress.put("direct", false);
        progress.put("infer", false);
        progress.put("opinion", false);
        progress.put("connect", false);

        /*
         * "총 복습" 화면(during-reading-practice.html의 finishReview()) 완료 여부.
         * 4개 질문 유형을 다 풀어야 열리는 마지막 단계이며,
         * 다른 화면의 잠금 해제 조건에는 쓰이지 않는 완료 기록용 플래그다.
         */
        progress.put("review", false);

        return progress;
    }
}