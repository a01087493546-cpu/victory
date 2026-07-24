package com.victory.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.victory.entity.PracticeAchievementSnapshot;

public interface PracticeAchievementSnapshotRepository
        extends JpaRepository<PracticeAchievementSnapshot, Long> {

    Optional<PracticeAchievementSnapshot> findByStudentIdAndClassReadingBookIdAndSnapshotDate(
            Long studentId, Long classReadingBookId, LocalDate snapshotDate);

    Optional<PracticeAchievementSnapshot> findTopByStudentIdAndClassReadingBookIdOrderBySnapshotDateDesc(
            Long studentId, Long classReadingBookId);

    List<PracticeAchievementSnapshot> findByStudentIdAndClassReadingBookIdAndSnapshotDateBetweenOrderBySnapshotDateAsc(
            Long studentId, Long classReadingBookId, LocalDate from, LocalDate to);
}
