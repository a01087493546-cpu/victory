package com.victory.service;

import com.victory.dto.LoginRequest;
import com.victory.dto.LoginResponse;
import com.victory.dto.RegisterRequest;
import com.victory.dto.RegisterResponse;
import com.victory.dto.StudentCreateRequest;
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
        user.setRole("teacher");

        User savedUser = userRepository.save(user);

        return new RegisterResponse(
                savedUser.getId(),
                savedUser.getLoginId(),
                savedUser.getName(),
                savedUser.getRole(),
                savedUser.getCreatedAt()
        );
    }

    public RegisterResponse createStudent(Long teacherId, StudentCreateRequest request) {
        if (userRepository.findByLoginId(request.getLoginId()).isPresent()) {
            throw new DuplicateLoginIdException("이미 사용 중인 아이디입니다.");
        }

        User student = new User();
        student.setLoginId(request.getLoginId());
        student.setPassword(passwordEncoder.encode(request.getPassword()));
        student.setName(request.getName());
        student.setRole("student");

        User savedStudent = userRepository.save(student);

        // TODO: class_students 연결 - Class/ClassStudent 엔티티 도입 후 teacherId 기준으로 배정 처리 필요

        return new RegisterResponse(
                savedStudent.getId(),
                savedStudent.getLoginId(),
                savedStudent.getName(),
                savedStudent.getRole(),
                savedStudent.getCreatedAt()
        );
    }

    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByLoginId(request.getLoginId())
                .orElseThrow(() -> new InvalidCredentialsException("아이디 또는 비밀번호가 올바르지 않습니다."));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new InvalidCredentialsException("아이디 또는 비밀번호가 올바르지 않습니다.");
        }

        String token = jwtUtil.generateToken(user.getId(), user.getLoginId(), user.getRole());

        return new LoginResponse(token, user.getId(), user.getLoginId(), user.getName(), user.getRole());
    }
}
