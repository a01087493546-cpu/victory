package com.victory.entity;

import java.time.LocalDateTime;
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
 * 던전(게임 스테이지) 정의. game/js/game-api.js의 DUNGEONS와 game/index.html의
 * DUNGEON_INFO로 중복 하드코딩되어 있던 것을 하나로 통합하는 테이블.
 */
@Entity
@Table(name = "dungeons")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Dungeon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Lob
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /*
     * 표시용 텍스트("쉬움/보통/어려움"). 코드에 비교/정렬 로직 없음을 확인했다.
     */
    @Column(name = "difficulty", length = 20)
    private String difficulty;

    @Column(name = "required_books")
    private Integer requiredBooks;

    @Column(name = "required_stat_avg")
    private Integer requiredStatAvg;

    /*
     * 이 던전에 입장하려면 먼저 클리어해야 하는 던전. 최초 던전(초급)은 NULL.
     * DUNGEON_INFO의 locked/lockedMessage가 배열 순서로만 암묵적으로 표현하던 관계를
     * 명시적 컬럼으로 뽑아낸 것(game-system-audit.md §10-2).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prerequisite_dungeon_id")
    private Dungeon prerequisiteDungeon;

    @Column(name = "time_limit_seconds")
    private Integer timeLimitSeconds;

    /*
     * {maxHp, normalAtk, heavyAtk, normalAtkInterval, heavyAtkInterval}
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "enemy_stats", columnDefinition = "json")
    private Map<String, Object> enemyStats;

    @Column(name = "reward_title", length = 100)
    private String rewardTitle;

    @Lob
    @Column(name = "reward_note", columnDefinition = "TEXT")
    private String rewardNote;

    /*
     * 클리어 후 리셋되는 능력치 평균값(승리 시: 각 능력치 = MIN(현재값, 이 값)).
     * 최종 스테이지는 NULL(더 이상 리셋할 다음 단계가 없음).
     */
    @Column(name = "reward_stat_reset_value")
    private Integer rewardStatResetValue;

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
