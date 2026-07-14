package com.victory.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.victory.entity.PracticeProgress;

public interface PracticeProgressRepository
        extends JpaRepository<PracticeProgress, Long> {

    Optional<PracticeProgress> findByStudent_Id(Long studentId);
}