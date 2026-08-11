package com.victory.service;

import java.time.LocalDate;

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

/*
 * 개별읽기 읽기 중(책속 생각쓰기) 일일 보상: 마법력 +1, 지혜 +1.
 * 완독 전까지 매일 반복하는 활동이라 "readingRecordId당 1회"가 아니라
 * "readingRecordId + 날짜당 1회"로 지급한다 - instance_id에 날짜를 포함시켜
 * IndividualBeforeReadingRewardService/PracticeReadingRewardService와 같은
 * student_stat_reward_log UNIQUE(student_id, reward_type, instance_id) 구조를
 * 그대로 재사용한다.
 */
@Service
@RequiredArgsConstructor
public class IndividualDuringReadingRewardService {

    public static final String REWARD_TYPE = "INDIVIDUAL_DURING_DAILY_COMPLETE";
    private static final int MAGIC_REWARD = 1;
    private static final int WISDOM_REWARD = 1;

    /*
     * student_stats 행이 아직 없는 학생에게 이 보상으로 처음 행을 만들 때만
     * 쓰는 기본값이다. 이미 행이 있는 학생은 절대 이 값으로 덮어쓰지 않는다.
     */
    private static final int NEW_STUDENT_BASE_STAT = 8;

    private final StudentStatsRepository studentStatsRepository;
    private final StudentStatRewardLogRepository rewardLogRepository;
    private final StudentEndingService studentEndingService;

    @Transactional
    public RewardResult grantDuringDailyRewardOnce(User student, Long readingRecordId, LocalDate activityDate) {

        if (studentEndingService.hasEnded(student.getId())) {
            return RewardResult.alreadyGranted(getOrCreateStats(student));
        }

        String instanceId = buildInstanceId(readingRecordId, activityDate);

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
        stats.setMagic(stats.getMagic() + MAGIC_REWARD);
        stats.setWisdom(stats.getWisdom() + WISDOM_REWARD);
        StudentStats savedStats = studentStatsRepository.save(stats);

        StudentStatRewardLog log = new StudentStatRewardLog();
        log.setStudent(student);
        log.setRewardType(REWARD_TYPE);
        log.setStatType("magic_wisdom");
        log.setAmount(1);
        log.setInstanceId(instanceId);
        rewardLogRepository.save(log);

        return RewardResult.granted(savedStats);
    }

    private StudentStats getOrCreateStats(User student) {
        return studentStatsRepository
            .findByStudent_Id(student.getId())
            .orElseGet(() -> {
                StudentStats stats = new StudentStats();
                stats.setStudent(student);
                stats.setMagic(NEW_STUDENT_BASE_STAT);
                stats.setStamina(NEW_STUDENT_BASE_STAT);
                stats.setWisdom(NEW_STUDENT_BASE_STAT);
                stats.setCourage(NEW_STUDENT_BASE_STAT);
                return studentStatsRepository.save(stats);
            });
    }

    private String buildInstanceId(Long readingRecordId, LocalDate activityDate) {
        return (readingRecordId == null ? "unknown" : readingRecordId.toString())
            + ":" + activityDate;
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
