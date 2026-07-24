package com.victory.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.victory.dto.StudentStatsResponse;
import com.victory.entity.User;
import com.victory.repository.StudentStatsRepository;
import com.victory.repository.UserRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudentStatsService {

    private final StudentStatsRepository studentStatsRepository;
    private final UserRepository userRepository;

    public StudentStatsResponse getStats(Long studentId) {
        User student = userRepository.findById(studentId)
            .orElseThrow(() -> new EntityNotFoundException(
                "학생을 찾을 수 없습니다. studentId=" + studentId));

        if (!"student".equalsIgnoreCase(student.getRole())) {
            throw new IllegalArgumentException("학생 계정만 능력치를 조회할 수 있습니다.");
        }

        return studentStatsRepository.findByStudent_Id(studentId)
            .map(StudentStatsResponse::from)
            .orElseGet(StudentStatsResponse::zero);
    }
}
