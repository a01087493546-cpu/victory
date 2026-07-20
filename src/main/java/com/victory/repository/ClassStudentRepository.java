package com.victory.repository;

import com.victory.entity.ClassStudent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ClassStudentRepository extends JpaRepository<ClassStudent, Long> {

    List<ClassStudent> findBySchoolClassId(Long classId);

    Optional<ClassStudent> findByStudentId(Long studentId);
}