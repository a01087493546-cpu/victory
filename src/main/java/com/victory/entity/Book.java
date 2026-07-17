package com.victory.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
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
 * 학생이 읽는 책. 교사가 지정하는 온책읽기(연습읽기) 도서와
 * 학생 개인이 등록하는 개별읽기 자유도서를 하나의 테이블로 통합해서 관리한다(source 컬럼으로 구분).
 */
@Entity
@Table(
    name = "books",
    indexes = {
        @Index(name = "idx_books_class_id", columnList = "class_id"),
        @Index(name = "idx_books_registered_by", columnList = "registered_by")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "author", length = 100)
    private String author;

    /*
     * base64 이미지 데이터라 용량이 클 수 있어 LONGTEXT로 명시한다.
     * (주의: @Lob만 쓰면 MySQL에서 tinytext(255자)로 생성되는 문제가 있어 columnDefinition을 직접 지정함)
     */
    @Lob
    @Column(name = "cover_image", columnDefinition = "LONGTEXT")
    private String coverImage;

    /*
     * 책 종류. 허용값: story(이야기 책) / info(정보를 담은 책) /
     * opinion(주장을 담은 책) / etc(그 밖의 책).
     * individual-before-reading.html의 selectBookType() 4개 버튼과 1:1 대응.
     */
    @Column(name = "book_type", length = 30)
    private String bookType;

    /*
     * 책 소개(줄거리 등). 교사가 등록하면 학생 화면에 그대로 보여주는 용도.
     */
    @Lob
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /*
     * class(온책읽기 지정 도서) / individual(개별읽기 자유도서)
     */
    @Column(name = "source", nullable = false, length = 20)
    private String source = "individual";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_id")
    private SchoolClass schoolClass;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "registered_by")
    private User registeredBy;

    @Column(name = "reading_range", length = 200)
    private String readingRange;

    /*
     * 온책읽기(학급 공통) 진행 쪽수. book-manage.html "이번 차시 읽을 범위" 카드 대응.
     * source='class'인 책에서만 값이 채워진다. 개별읽기 진행 쪽수는 reading_records/
     * reading_progress_logs가 학생별로 따로 관리하므로 이 컬럼과는 별개다.
     */
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
