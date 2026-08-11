package com.victory.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.victory.dto.StudentStatsResponse;
import com.victory.entity.StudentStats;
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
    private final DemoAccountService demoAccountService;

    public StudentStatsResponse getStats(Long studentId) {
        User student = userRepository.findById(studentId)
            .orElseThrow(() -> new EntityNotFoundException(
                "학생을 찾을 수 없습니다. studentId=" + studentId));

        if (!"student".equalsIgnoreCase(student.getRole())) {
            throw new IllegalArgumentException("학생 계정만 능력치를 조회할 수 있습니다.");
        }

        boolean hasSeenEnding = Boolean.TRUE.equals(student.getHasSeenEnding());

        return studentStatsRepository.findByStudent_Id(studentId)
            .map(stats -> StudentStatsResponse.from(stats, hasSeenEnding))
            .orElseGet(() -> StudentStatsResponse.from(null, hasSeenEnding));
    }

    /*
     * 던전 승리 보상: magic/stamina/wisdom/courage 4개를 모두 value로 맞춘다.
     * 호출 측인 DungeonService가 dungeon.rewardStatResetValue를 그대로 넘긴다.
     */
    @Transactional
    public void applyReward(Long studentId, int value) {
        if (demoAccountService.isDemoAccount(studentId)) {
            return;
        }
        StudentStats stats = studentStatsRepository.findByStudent_Id(studentId)
            .orElseGet(() -> {
                User student = userRepository.findById(studentId)
                    .orElseThrow(() -> new EntityNotFoundException(
                        "학생을 찾을 수 없습니다. studentId=" + studentId));

                StudentStats created = new StudentStats();
                created.setStudent(student);
                return created;
            });

        stats.setMagic(value);
        stats.setStamina(value);
        stats.setWisdom(value);
        stats.setCourage(value);

        studentStatsRepository.save(stats);
    }

    public double getStatAverage(Long studentId) {
        return studentStatsRepository.findByStudent_Id(studentId)
            .map(stats -> (stats.getMagic() + stats.getStamina() + stats.getWisdom() + stats.getCourage()) / 4.0)
            .orElse(0.0);
    }
}
