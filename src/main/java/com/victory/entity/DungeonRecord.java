package com.victory.entity;

import java.time.LocalDateTime;
import java.time.ZoneId;

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
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/*
 * 학생별 던전 플레이 기록. 학생-던전 조합에 UNIQUE 제약을 일부러 걸지 않는다 —
 * 같은 던전을 여러 번 재도전한 기록이 여러 행으로 쌓여야 재도전/반복 순환 구조가 된다
 * (game-system-audit.md §10-4에서 "기존 구조 그대로 재사용 가능"으로 확정).
 */
@Entity
@Table(
    name = "dungeon_records",
    indexes = {
        @Index(name = "idx_dungeon_records_student_dungeon", columnList = "student_id, dungeon_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DungeonRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dungeon_id", nullable = false)
    private Dungeon dungeon;

    /*
     * victory / defeat / timeout — endBattle(result) 3분기와 대응.
     */
    @Column(name = "result", nullable = false, length = 20)
    private String result;

    @Column(name = "played_at", nullable = false, updatable = false)
    private LocalDateTime playedAt;

    @PrePersist
    protected void onCreate() {
        this.playedAt = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
    }
}
