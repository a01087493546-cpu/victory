package com.victory.service;

import com.victory.dto.LoginRequest;
import com.victory.dto.LoginResponse;
import com.victory.dto.RegisterRequest;
import com.victory.dto.RegisterResponse;
import com.victory.entity.User;
import com.victory.exception.DuplicateLoginIdException;
import com.victory.exception.InvalidCredentialsException;
import com.victory.repository.UserRepository;
import com.victory.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public RegisterResponse register(RegisterRequest request) {
        if (userRepository.findByLoginId(request.getLoginId()).isPresent()) {
            throw new DuplicateLoginIdException("이미 사용 중인 아이디입니다.");
        }

        User user = new User();
        user.setLoginId(request.getLoginId());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setName(request.getName());
        user.setRole(request.getRole());

        User savedUser = userRepository.save(user);

        return new RegisterResponse(
                savedUser.getId(),
                savedUser.getLoginId(),
                savedUser.getName(),
                savedUser.getRole(),
                savedUser.getCreatedAt()
        );
    }

    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByLoginId(request.getLoginId())
                .orElseThrow(() -> new InvalidCredentialsException("아이디 또는 비밀번호가 올바르지 않습니다."));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new InvalidCredentialsException("아이디 또는 비밀번호가 올바르지 않습니다.");
        }

        String token = jwtUtil.generateToken(user.getId(), user.getLoginId(), user.getRole());

        return new LoginResponse(
                token,
                user.getId(),
                user.getLoginId(),
                user.getName(),
                user.getRole(),
                Boolean.TRUE.equals(user.getDemoAccount()),
                Boolean.TRUE.equals(user.getHasSeenStoryIntro()),
                Boolean.TRUE.equals(user.getHasSeenPowerIntro()));
    }
}
