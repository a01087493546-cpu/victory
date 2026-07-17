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
 * 학급-학생 소속 관계.
 * 같은 학생이 같은 학급에 두 번 등록되지 않도록 (class_id, student_id) 조합을 유일하게 강제한다.
 */
@Entity
@Table(
    name = "class_students",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_class_students_class_student",
        columnNames = {"class_id", "student_id"}
    )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ClassStudent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_id", nullable = false)
    private SchoolClass schoolClass;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    /*
     * 학급 내 출석번호. classes.class_number(반 번호)와는 다른 값이니 혼동하지 않는다.
     * teacher-register.html의 students[].number 대응.
     */
    @Column(name = "student_number")
    private Integer studentNumber;

    @Column(name = "joined_at", nullable = false, updatable = false)
    private LocalDateTime joinedAt;

    @PrePersist
    protected void onCreate() {
        this.joinedAt = LocalDateTime.now();
    }
}
