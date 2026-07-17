package com.victory.repository;

import com.victory.entity.ClassStudent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ClassStudentRepository extends JpaRepository<ClassStudent, Long> {

    List<ClassStudent> findBySchoolClassId(Long classId);
}