package com.victory.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.victory.entity.StudentStats;

public interface StudentStatsRepository extends JpaRepository<StudentStats, Long> {

    Optional<StudentStats> findByStudent_Id(Long studentId);
}
