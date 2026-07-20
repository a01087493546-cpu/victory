package com.victory.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.victory.entity.ClassReadingBook;

public interface ClassReadingBookRepository extends JpaRepository<ClassReadingBook, Long> {

    Optional<ClassReadingBook> findBySchoolClassId(Long classId);
}
