package com.victory.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.victory.dto.StudentClassResponse;
import com.victory.service.ClassStudentService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/students")
@RequiredArgsConstructor
public class StudentClassController {

    private final ClassStudentService classStudentService;

    @GetMapping("/{studentId}/class")
    public ResponseEntity<StudentClassResponse> getStudentClass(
            @PathVariable("studentId") Long studentId
    ) {
        StudentClassResponse studentClass =
                classStudentService.getClassByStudentId(studentId);

        return ResponseEntity.ok(studentClass);
    }
}