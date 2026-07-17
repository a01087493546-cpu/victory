package com.victory.controller;

import com.victory.dto.TeacherRegisterRequest;
import com.victory.dto.TeacherRegisterResponse;
import com.victory.service.TeacherRegistrationService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.victory.dto.TeacherClassResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@RestController
@RequestMapping("/api/teachers")
@RequiredArgsConstructor
public class TeacherRegistrationController {

    private final TeacherRegistrationService teacherRegistrationService;

    @PostMapping("/register")
    public ResponseEntity<TeacherRegisterResponse> registerTeacher(
            @Valid @RequestBody TeacherRegisterRequest request
    ) {
        TeacherRegisterResponse response =
                teacherRegistrationService.registerTeacherWithClass(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }
@GetMapping("/{teacherId}/class")
public ResponseEntity<TeacherClassResponse> getTeacherClass(
        @PathVariable("teacherId") Long teacherId
) {
    TeacherClassResponse response =
            teacherRegistrationService.getTeacherClass(teacherId);

    return ResponseEntity.ok(response);
}
}