package com.victory.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.victory.entity.Summary;

public interface SummaryRepository extends JpaRepository<Summary, Long> {

    Optional<Summary> findByStudent_IdAndClassReadingBookId(
            Long studentId,
            Long classReadingBookId);

    List<Summary> findByClassReadingBookIdAndIsSharedTrueOrderByUpdatedAtDesc(
            Long classReadingBookId);
}
