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
 * 요약(summaries)/책수다글(responses)/추천도서(book_recommendations) 좋아요.
 *
 * content_id는 content_type에 따라 참조 대상이 달라지는 다형성 참조라
 * DB 레벨 FK 제약을 걸 수 없다 — 애플리케이션에서 무결성을 관리해야 한다.
 * 좋아요 수는 캐시 컬럼 없이 이 테이블을 매번 COUNT해서 구하기로 확정했다
 * (dashboard-metrics-audit.md에서 논의된 캐시 vs 실시간 집계 중 실시간 집계로 결론).
 */
@Entity
@Table(
    name = "content_likes",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_content_likes_student_content",
        columnNames = {"student_id", "content_type", "content_id"}
    )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ContentLike {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    /*
     * summary / response / book_recommendation
     */
    @Column(name = "content_type", nullable = false, length = 30)
    private String contentType;

    /*
     * content_type에 따라 summaries.id / responses.id / book_recommendations.id를 가리킨다.
     */
    @Column(name = "content_id", nullable = false)
    private Long contentId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
