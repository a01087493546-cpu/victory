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
import com.victory.service.StudentStatsService;

import lombok.RequiredArgsConstructor;

/*
 * 학생 최초 로그인 인트로(시작 스토리 / 나의 힘) 완료 상태 저장.
 * 일반 학생 계정 전용이다 - 심사계정은 여러 심사위원이 같은 계정을
 * 공유하므로 이 API를 부르지 않고 프론트 localStorage로만 관리한다
 * (auth.js/story-intro.js/ability-intro.html의 isDemoAccount() 분기 참고).
 *
 * 엔딩(고급 던전 클리어 후 ending-intro.html 마지막 장면까지 완료) 완료
 * 처리도 같은 "학생 마일스톤 완료 상태" 성격이라 이 컨트롤러에 함께 둔다.
 */
@RestController
@RequestMapping("/api/students/me")
@RequiredArgsConstructor
public class StudentIntroController {

    private final UserRepository userRepository;
    private final StudentStatsService studentStatsService;

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
            userRepository.saveAndFlush(student);
        }
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/power-intro-seen")
    public ResponseEntity<Void> markPowerIntroSeen(Authentication authentication) {
        User student = requireStudent(authentication);
        if (!Boolean.TRUE.equals(student.getHasSeenPowerIntro())) {
            student.setHasSeenPowerIntro(true);
            userRepository.saveAndFlush(student);
        }
        return ResponseEntity.noContent().build();
    }

    /*
     * 고급 던전 최초 클리어 후 엔딩(ending-intro.html)을 마지막 장면까지
     * 다 본 시점에만 호출된다(전투 승리 시점이 아님). 이 시점부터
     * 능력치 시스템을 영구 종료한다 - has_seen_ending을 true로 남기고
     * (이후 모든 보상 서비스/DungeonService가 이 값으로 지급을 건너뛴다),
     * 능력치 4종을 정확히 0으로 SET한다(기존 값에서 빼는 게 아니라
     * StudentStatsService.applyReward(id, 0) 그대로 재사용).
     * 이미 완료 처리된 학생이 다시 호출해도(예: 재도전 후 재방문) 멱등하게
     * 아무 부작용이 없어야 하므로 매번 그대로 0으로 SET한다.
     */
    @PatchMapping("/ending-seen")
    public ResponseEntity<Void> markEndingSeen(Authentication authentication) {
        User student = requireStudent(authentication);
        student.setHasSeenEnding(true);
        userRepository.save(student);
        studentStatsService.applyReward(student.getId(), 0);
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
