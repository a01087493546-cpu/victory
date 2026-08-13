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
    private static final String COMPLETE_INSTANCE_ID = SOURCE_TYPE + ":complete";
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

        if (hasPracticeCompletionReward(student.getId())) {

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
        StudentStats stats = studentStatsRepository
            .findByStudent_Id(student.getId())
            .orElse(null);

        boolean alreadyGranted = hasPracticeCompletionReward(student.getId());

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
        return COMPLETE_INSTANCE_ID;
    }

    private boolean hasPracticeCompletionReward(Long studentId) {
        /*
         * 현재 정책은 책/학급과 무관하게 학생별 최초 1회다. 예전 버전이
         * practice_reading:{classReadingBookId}로 남긴 로그도 같은 보상으로
         * 인정해야 배포 후 기존 학생에게 +8이 다시 지급되지 않는다.
         */
        return rewardLogRepository.findByStudent_Id(studentId).stream()
            .anyMatch(log -> REWARD_TYPE.equals(log.getRewardType()));
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
