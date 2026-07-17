package com.victory.entity;

import java.time.LocalDateTime;
import java.util.List;

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
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/*
 * 친구에게 추천하는 책(우리 반 추천 책장). book_votes를 대체하는 신규 테이블.
 * 좋아요 수 캐시 컬럼(like_count)은 두지 않는다 — content_likes를 실시간 COUNT해서 구한다.
 */
@Entity
@Table(name = "book_recommendations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BookRecommendation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "author", length = 100)
    private String author;

    @Lob
    @Column(name = "reason", nullable = false, columnDefinition = "TEXT")
    private String reason;

    /*
     * "이 책이 궁금해지는 질문" 최대 3개. responses.id 배열(텍스트 복사본이 아니라 실제 참조로 확정).
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "teaser_response_ids", columnDefinition = "json")
    private List<Long> teaserResponseIds;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
