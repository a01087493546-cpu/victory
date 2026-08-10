package com.victory.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.victory.entity.User;
import com.victory.repository.UserRepository;

import lombok.RequiredArgsConstructor;

/*
 * 학생 최초 로그인 인트로(시작 스토리 / 나의 힘) 완료 상태 저장.
 * 일반 학생 계정 전용이다 - 심사계정은 여러 심사위원이 같은 계정을
 * 공유하므로 이 API를 부르지 않고 프론트 localStorage로만 관리한다
 * (auth.js/story-intro.js/ability-intro.html의 isDemoAccount() 분기 참고).
 */
@RestController
@RequestMapping("/api/students/me")
@RequiredArgsConstructor
public class StudentIntroController {

    private final UserRepository userRepository;

    @GetMapping("/intro-status")
    public Map<String, Boolean> getIntroStatus(Authentication authentication) {
        User student = requireStudent(authentication);
        return Map.of(
            "hasSeenStoryIntro", Boolean.TRUE.equals(student.getHasSeenStoryIntro()),
            "hasSeenPowerIntro", Boolean.TRUE.equals(student.getHasSeenPowerIntro()));
    }

    @PatchMapping("/story-intro-seen")
    public ResponseEntity<Void> markStoryIntroSeen(Authentication authentication) {
        User student = requireStudent(authentication);
        if (!Boolean.TRUE.equals(student.getHasSeenStoryIntro())) {
            student.setHasSeenStoryIntro(true);
            userRepository.save(student);
        }
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/power-intro-seen")
    public ResponseEntity<Void> markPowerIntroSeen(Authentication authentication) {
        User student = requireStudent(authentication);
        if (!Boolean.TRUE.equals(student.getHasSeenPowerIntro())) {
            student.setHasSeenPowerIntro(true);
            userRepository.save(student);
        }
        return ResponseEntity.noContent().build();
    }

    private User requireStudent(Authentication authentication) {
        Object principal = authentication == null ? null : authentication.getPrincipal();

        if (!(principal instanceof Long studentId)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "학생 로그인이 필요합니다.");
        }

        return userRepository.findById(studentId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "학생 로그인이 필요합니다."));
    }
}
