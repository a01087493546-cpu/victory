package com.victory.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.victory.dto.ClassStudentResponse;
import com.victory.dto.StudentClassResponse;
import com.victory.entity.ClassStudent;
import com.victory.entity.SchoolClass;
import com.victory.repository.ClassStudentRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ClassStudentService {

    private final ClassStudentRepository classStudentRepository;

    public List<ClassStudentResponse> getStudentsByClassId(Long classId) {
        return classStudentRepository.findBySchoolClassId(classId)
                .stream()
                .map(classStudent -> new ClassStudentResponse(
                        classStudent.getStudent().getId(),
                        classStudent.getStudent().getLoginId(),
                        classStudent.getStudent().getName(),
                        classStudent.getStudentNumber()
                ))
                .toList();
    }

    public StudentClassResponse getClassByStudentId(Long studentId) {
        ClassStudent classStudent = classStudentRepository
                .findByStudentId(studentId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "학생이 소속된 학급을 찾을 수 없습니다. studentId=" + studentId
                ));

        SchoolClass schoolClass = classStudent.getSchoolClass();

        return new StudentClassResponse(
                schoolClass.getId(),
                schoolClass.getClassName(),
                schoolClass.getGrade(),
                schoolClass.getClassNumber()
        );
    }
}