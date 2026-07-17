package com.victory.entity;

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
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/*
 * 능력치 보상 중복 지급 방지 이력.
 *
 * instance_id: 반복 지급 가능한 보상(책 완독마다/추천마다/게시글마다 등)의 인스턴스 식별자.
 * "학생당 평생 1회"인 보상(practice_all_complete 등)은 빈 문자열('')을 쓴다.
 * game-system-audit.md §10-5에서 확정: reward_type 하나만으로는 individual_after_complete /
 * individual_friend_book_recommend / individual_book_chat_post 등 반복 보상을
 * 정확히 구분할 수 없어서 이 컬럼을 추가했다. NULL 대신 빈 문자열 기본값을 쓰는 이유는
 * MySQL에서 UNIQUE 제약이 NULL을 여러 개 허용하기 때문이다.
 */
@Entity
@Table(
    name = "student_stat_reward_log",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_reward_log_student_type_instance",
        columnNames = {"student_id", "reward_type", "instance_id"}
    )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StudentStatRewardLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    /*
     * REWARD_PRESETS의 프리셋 키. 예: individual_before_complete
     */
    @Column(name = "reward_type", nullable = false, length = 60)
    private String rewardType;

    /*
     * magic / stamina / wisdom / courage
     */
    @Column(name = "stat_type", nullable = false, length = 20)
    private String statType;

    @Column(name = "amount", nullable = false)
    private Integer amount;

    @Column(name = "instance_id", nullable = false, length = 100)
    private String instanceId = "";

    @Column(name = "granted_at", nullable = false, updatable = false)
    private LocalDateTime grantedAt;

    @PrePersist
    protected void onCreate() {
        this.grantedAt = LocalDateTime.now();

        if (this.instanceId == null) {
            this.instanceId = "";
        }
    }
}
