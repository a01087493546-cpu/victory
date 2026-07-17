package com.victory.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "login_id", nullable = false, unique = true, length = 50)
    private String loginId;

    @Column(name = "password", nullable = false, length = 255)
    private String password;

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Column(name = "role", nullable = false, length = 20)
    private String role;

    @Column(name = "has_seen_story_intro", nullable = false)
    private Boolean hasSeenStoryIntro = false;

    /*
     * 던전(고급) 클리어 후 엔딩을 봤는지 여부.
     * 학생 전용 플래그이며 교사 계정은 항상 false로 둔다.
     */
    @Column(name = "has_seen_ending", nullable = false)
    private Boolean hasSeenEnding = false;

    /*
     * 교사 소속 학교명. teacher-register.html의 teacherRegisterData.teacher.school 대응.
     * 학생 계정은 값이 없다(NULL).
     */
    @Column(name = "school", length = 100)
    private String school;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
