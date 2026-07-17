package com.victory.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.victory.dto.ClassStudentResponse;
import com.victory.repository.ClassStudentRepository;

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
}