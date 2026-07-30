package com.victory.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.victory.entity.Book;

public interface BookRepository extends JpaRepository<Book, Long> {

    /*
     * 개별읽기 책 등록 시 같은 제목·지은이 책이 이미 있으면 재사용한다.
     * source로 범위를 좁혀 교사가 등록한 온책읽기 도서(source='class')가
     * 실수로 재사용되지 않게 한다.
     */
    Optional<Book> findFirstBySourceAndTitleAndAuthor(String source, String title, String author);
}
