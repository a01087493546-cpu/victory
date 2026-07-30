package com.victory.config;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RecommendationBookSchemaInitializer {

    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void ensureRecommendationBooksTable() {
        jdbcTemplate.execute(
            "CREATE TABLE IF NOT EXISTS recommendation_books (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                "title VARCHAR(200) NOT NULL, " +
                "author VARCHAR(100) NOT NULL, " +
                "description TEXT NOT NULL, " +
                "cover_image LONGTEXT NULL, " +
                "thickness VARCHAR(30) NOT NULL, " +
                "mood VARCHAR(50) NOT NULL, " +
                "genre VARCHAR(50) NOT NULL, " +
                "illustration_level VARCHAR(30) NOT NULL, " +
                "difficulty VARCHAR(30) NOT NULL, " +
                "purpose_tags JSON NOT NULL, " +
                "recommended_grade_min INT NULL, " +
                "recommended_grade_max INT NULL, " +
                "is_active TINYINT(1) NOT NULL DEFAULT 1, " +
                "created_at DATETIME NOT NULL, " +
                "updated_at DATETIME NOT NULL, " +
                "UNIQUE KEY uk_recommendation_books_title_author (title, author), " +
                "KEY idx_recommendation_books_active_title (is_active, title, id), " +
                "KEY idx_recommendation_books_grade (recommended_grade_min, recommended_grade_max)" +
            ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci"
        );
    }
}
