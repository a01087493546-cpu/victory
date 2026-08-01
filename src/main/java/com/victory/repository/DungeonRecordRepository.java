package com.victory.repository;

import java.time.LocalDateTime;

import org.springframework.data.jpa.repository.JpaRepository;

import com.victory.entity.DungeonRecord;

public interface DungeonRecordRepository extends JpaRepository<DungeonRecord, Long> {

    long countByStudent_IdAndDungeon_IdAndPlayedAtAfter(Long studentId, Long dungeonId, LocalDateTime after);

    boolean existsByStudent_IdAndDungeon_IdAndResult(Long studentId, Long dungeonId, String result);
}
