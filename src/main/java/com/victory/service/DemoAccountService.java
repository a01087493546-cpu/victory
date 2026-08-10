package com.victory.service;

import org.springframework.stereotype.Service;
import com.victory.repository.UserRepository;
import lombok.RequiredArgsConstructor;

/** 아이디 문자열 비교를 여러 화면과 서비스에 흩뜨리지 않는 공통 판별기. */
@Service
@RequiredArgsConstructor
public class DemoAccountService {
    private final UserRepository userRepository;

    public boolean isDemoAccount(Long userId) {
        return userId != null && userRepository.findById(userId)
            .map(user -> Boolean.TRUE.equals(user.getDemoAccount()))
            .orElse(false);
    }
}
