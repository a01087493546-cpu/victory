package com.victory.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.victory.entity.RecommendationBook;

public interface RecommendationBookRepository extends JpaRepository<RecommendationBook, Long> {

    List<RecommendationBook> findByActiveTrueOrderByTitleAscIdAsc();

    @Query("""
        SELECT book
        FROM RecommendationBook book
        WHERE book.active = true
        AND (:grade IS NULL
            OR (book.recommendedGradeMin IS NULL OR book.recommendedGradeMin <= :grade)
            AND (book.recommendedGradeMax IS NULL OR book.recommendedGradeMax >= :grade))
        ORDER BY book.title ASC, book.id ASC
        """)
    List<RecommendationBook> findActiveBooksForGradeOrderByTitleAscIdAsc(@Param("grade") Integer grade);

    List<RecommendationBook> findByIdIn(Collection<Long> ids);

    boolean existsByTitle(String title);

    boolean existsByTitleAndAuthor(String title, String author);
}
