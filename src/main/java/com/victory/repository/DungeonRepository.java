package com.victory.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.victory.entity.Dungeon;

public interface DungeonRepository extends JpaRepository<Dungeon, Long> {
}
