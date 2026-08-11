package com.victory.service;

import org.springframework.stereotype.Service;

import com.victory.repository.UserRepository;

import lombok.RequiredArgsConstructor;

/*
 * 고급(최종) 던전 클리어 → 엔딩을 끝까지 본 학생인지 판별하는 공통 판별기.
 * DemoAccountService와 같은 모양 - 이 학생이 "능력치 시스템 종료" 상태인지
 * 여러 보상 서비스/DungeonService가 공통으로 확인할 때 쓴다.
 */
@Service
@RequiredArgsConstructor
public class StudentEndingService {

    private final UserRepository userRepository;

    public boolean hasEnded(Long userId) {
        return userId != null && userRepository.findById(userId)
            .map(user -> Boolean.TRUE.equals(user.getHasSeenEnding()))
            .orElse(false);
    }
}
