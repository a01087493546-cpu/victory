package com.victory.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.victory.dto.ClassStudentResponse;
import com.victory.service.ClassStudentService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/classes")
@RequiredArgsConstructor
public class ClassStudentController {

    private final ClassStudentService classStudentService;

    @GetMapping("/{classId}/students")
    public ResponseEntity<List<ClassStudentResponse>> getClassStudents(
            @PathVariable("classId") Long classId
    ) {
        List<ClassStudentResponse> students =
                classStudentService.getStudentsByClassId(classId);

        return ResponseEntity.ok(students);
    }
}