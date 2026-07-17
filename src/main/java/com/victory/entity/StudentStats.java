package com.victory.entity;

import java.time.LocalDateTime;

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

/*
 * 학생 능력치(마법력/체력/지혜/용기) 현재값. 학생당 한 행.
 * frontend/js/individual-power.js의 individualPower_v2_<studentId> 대응.
 */
@Entity
@Table(name = "student_stats")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StudentStats {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false, unique = true)
    private User student;

    @Column(name = "magic", nullable = false)
    private Integer magic = 8;

    @Column(name = "stamina", nullable = false)
    private Integer stamina = 8;

    @Column(name = "wisdom", nullable = false)
    private Integer wisdom = 8;

    @Column(name = "courage", nullable = false)
    private Integer courage = 8;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
