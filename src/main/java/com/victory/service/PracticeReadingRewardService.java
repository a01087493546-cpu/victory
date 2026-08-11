package com.victory.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.victory.entity.StudentStatRewardLog;
import com.victory.entity.StudentStats;
import com.victory.entity.User;
import com.victory.repository.StudentStatRewardLogRepository;
import com.victory.repository.StudentStatsRepository;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PracticeReadingRewardService {

    public static final String REWARD_TYPE = "PRACTICE_READING_COMPLETE";
    public static final String SOURCE_TYPE = "practice_reading";
    private static final int REWARD_AMOUNT = 8;

    private final StudentStatsRepository studentStatsRepository;
    private final StudentStatRewardLogRepository rewardLogRepository;
    private final StudentEndingService studentEndingService;

    @Transactional
    public RewardResult grantPracticeCompleteRewardOnce(
            User student,
            Long classReadingBookId) {

        if (studentEndingService.hasEnded(student.getId())) {
            return RewardResult.alreadyGranted(getOrCreateStats(student));
        }

        String instanceId = buildInstanceId(classReadingBookId);

        if (rewardLogRepository
                .findByStudent_IdAndRewardTypeAndInstanceId(
                    student.getId(),
                    REWARD_TYPE,
                    instanceId)
                .isPresent()) {

            StudentStats stats = getOrCreateStats(student);
            return RewardResult.alreadyGranted(stats);
        }

        StudentStats stats = getOrCreateStats(student);
        stats.setMagic(stats.getMagic() + REWARD_AMOUNT);
        stats.setStamina(stats.getStamina() + REWARD_AMOUNT);
        stats.setWisdom(stats.getWisdom() + REWARD_AMOUNT);
        stats.setCourage(stats.getCourage() + REWARD_AMOUNT);
        StudentStats savedStats = studentStatsRepository.save(stats);

        StudentStatRewardLog log = new StudentStatRewardLog();
        log.setStudent(student);
        log.setRewardType(REWARD_TYPE);
        log.setStatType("all");
        log.setAmount(REWARD_AMOUNT);
        log.setInstanceId(instanceId);
        rewardLogRepository.save(log);

        return RewardResult.granted(savedStats);
    }

    @Transactional(readOnly = true)
    public RewardResult getRewardState(User student, Long classReadingBookId) {
        String instanceId = buildInstanceId(classReadingBookId);
        StudentStats stats = studentStatsRepository
            .findByStudent_Id(student.getId())
            .orElse(null);

        boolean alreadyGranted = rewardLogRepository
            .findByStudent_IdAndRewardTypeAndInstanceId(
                student.getId(),
                REWARD_TYPE,
                instanceId)
            .isPresent();

        return new RewardResult(false, alreadyGranted, stats);
    }

    private StudentStats getOrCreateStats(User student) {
        return studentStatsRepository
            .findByStudent_Id(student.getId())
            .orElseGet(() -> {
                StudentStats stats = new StudentStats();
                stats.setStudent(student);
                return studentStatsRepository.save(stats);
            });
    }

    private String buildInstanceId(Long classReadingBookId) {
        return SOURCE_TYPE + ":" + (classReadingBookId == null ? "unknown" : classReadingBookId);
    }

    @Getter
    @AllArgsConstructor
    public static class RewardResult {
        private boolean rewardGranted;
        private boolean rewardAlreadyGranted;
        private StudentStats stats;

        private static RewardResult granted(StudentStats stats) {
            return new RewardResult(true, false, stats);
        }

        private static RewardResult alreadyGranted(StudentStats stats) {
            return new RewardResult(false, true, stats);
        }
    }
}
