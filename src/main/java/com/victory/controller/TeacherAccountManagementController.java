package com.victory.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.victory.dto.TeacherManagedAccountsResponse;
import com.victory.dto.TemporaryPasswordResponse;
import com.victory.service.TeacherAccountManagementService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/teachers/me")
@RequiredArgsConstructor
public class TeacherAccountManagementController {

    private final TeacherAccountManagementService accountManagementService;

    @GetMapping("/managed-accounts")
    public ResponseEntity<TeacherManagedAccountsResponse> getManagedAccounts(
            Authentication authentication) {
        return ResponseEntity.ok(
                accountManagementService.getManagedAccounts(requireTeacherId(authentication)));
    }

    @PostMapping("/students/{studentId}/reset-password")
    public ResponseEntity<TemporaryPasswordResponse> resetStudentPassword(
            @PathVariable Long studentId,
            Authentication authentication) {
        return ResponseEntity.ok(accountManagementService.resetStudentPassword(
                requireTeacherId(authentication), studentId));
    }

    private Long requireTeacherId(Authentication authentication) {
        Object principal = authentication == null ? null : authentication.getPrincipal();

        if (!(principal instanceof Long teacherId)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "인증이 필요합니다.");
        }

        return teacherId;
    }
}
