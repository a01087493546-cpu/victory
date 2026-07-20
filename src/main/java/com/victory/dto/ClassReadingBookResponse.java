package com.victory.dto;

import java.time.LocalDateTime;

import com.victory.entity.ClassReadingBook;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ClassReadingBookResponse {

    private Long id;
    private Long classId;
    private String bookTitle;
    private String author;
    private String coverImage;
    private Integer totalPages;
    private Integer currentPage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ClassReadingBookResponse from(ClassReadingBook book) {
        return new ClassReadingBookResponse(
            book.getId(),
            book.getSchoolClass().getId(),
            book.getBookTitle(),
            book.getAuthor(),
            book.getCoverImage(),
            book.getTotalPages(),
            book.getCurrentPage(),
            book.getCreatedAt(),
            book.getUpdatedAt()
        );
    }
}