package com.victory.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.victory.entity.BookRecommendation;

public interface BookRecommendationRepository extends JpaRepository<BookRecommendation, Long> {

    List<BookRecommendation> findByStudent_IdInOrderByCreatedAtDescIdDesc(Collection<Long> studentIds);

    List<BookRecommendation> findByStudent_IdOrderByCreatedAtDescIdDesc(Long studentId);
}
