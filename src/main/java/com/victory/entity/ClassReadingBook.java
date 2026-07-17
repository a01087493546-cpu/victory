package com.victory.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/*
 * 학급별 온책읽기(현재 함께 읽는 책) 정보.
 * 학급 하나당 1행만 존재하며(class_id UNIQUE), 교사가 책을 바꿀 때마다
 * 새 행을 추가하지 않고 기존 행을 덮어쓴다.
 */
@Entity
@Table(name = "class_reading_books")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ClassReadingBook {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_id", nullable = false, unique = true)
    private SchoolClass schoolClass;

    @Column(name = "book_title", nullable = false, length = 200)
    private String bookTitle;

    @Column(name = "author", length = 100)
    private String author;

    /*
     * base64 이미지 데이터일 수 있어 LONGTEXT로 명시한다.
     * (@Lob만 쓰면 MySQL에서 tinytext(255자)로 생성되는 문제가 있어 columnDefinition을 직접 지정)
     */
    @Lob
    @Column(name = "cover_image", columnDefinition = "LONGTEXT")
    private String coverImage;

    @Column(name = "total_pages")
private Integer totalPages;

@Column(name = "current_page")
private Integer currentPage;

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
