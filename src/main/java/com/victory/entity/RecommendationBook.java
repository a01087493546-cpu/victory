package com.victory.entity;

import java.time.LocalDateTime;
import java.util.List;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/*
 * AI 맞춤 추천 후보용 마스터 도서. 학생 개인 독서 기록용 books와 분리해서
 * 운영자가 검증한 실제 도서 풀만 추천 후보로 쓰기 위한 테이블이다.
 */
@Entity
@Table(
    name = "recommendation_books",
    indexes = {
        @Index(name = "idx_recommendation_books_active_title", columnList = "is_active, title, id"),
        @Index(name = "idx_recommendation_books_grade", columnList = "recommended_grade_min, recommended_grade_max")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RecommendationBook {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "author", nullable = false, length = 100)
    private String author;

    @Lob
    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Lob
    @Column(name = "cover_image", columnDefinition = "LONGTEXT")
    private String coverImage;

    @Column(name = "thickness", nullable = false, length = 30)
    private String thickness;

    @Column(name = "mood", nullable = false, length = 50)
    private String mood;

    @Column(name = "genre", nullable = false, length = 50)
    private String genre;

    @Column(name = "illustration_level", nullable = false, length = 30)
    private String illustrationLevel;

    @Column(name = "difficulty", nullable = false, length = 30)
    private String difficulty;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "purpose_tags", nullable = false, columnDefinition = "json")
    private List<String> purposeTags;

    @Column(name = "recommended_grade_min")
    private Integer recommendedGradeMin;

    @Column(name = "recommended_grade_max")
    private Integer recommendedGradeMax;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

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
