package com.victory.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.victory.entity.BookRecommendation;

public interface BookRecommendationRepository extends JpaRepository<BookRecommendation, Long> {

    List<BookRecommendation> findByStudent_IdInOrderByCreatedAtDescIdDesc(Collection<Long> studentIds);

    List<BookRecommendation> findByStudent_IdOrderByCreatedAtDescIdDesc(Long studentId);

    /*
     * 개별읽기 지표 계산(독서일수·활동 종류)용. readingRecordId가 없는
     * 과거 추천 글은 특정 책에 안전하게 귀속할 수 없으므로 이 조회
     * 자체에서 제외된다(컬럼이 NULL이면 결과에 포함되지 않음).
     */
    List<BookRecommendation> findByReadingRecord_Id(Long readingRecordId);
}
