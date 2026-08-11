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

/*
 * 완독한 책을 친구에게 추천하는 최초 등록 보상: 용기 +1.
 * 중복 기준은 student_id + reward_type + readingRecordId(instance_id)라서
 * 같은 완독 기록은 한 번만, 다른 완독 기록은 같은 날에도 각각 지급된다.
 */
@Service
@RequiredArgsConstructor
public class BookRecommendationRewardService {

    public static final String REWARD_TYPE = "INDIVIDUAL_BOOK_RECOMMENDATION";
    private static final int COURAGE_REWARD = 1;
    private static final int NEW_STUDENT_BASE_STAT = 8;

    private final StudentStatsRepository studentStatsRepository;
    private final StudentStatRewardLogRepository rewardLogRepository;
    private final StudentEndingService studentEndingService;

    @Transactional
    public RewardResult grantRecommendationRewardOnce(User student, Long readingRecordId) {
        if (studentEndingService.hasEnded(student.getId())) {
            return RewardResult.alreadyGranted(getOrCreateStats(student));
        }

        String instanceId = buildInstanceId(readingRecordId);

        if (rewardLogRepository
                .findByStudent_IdAndRewardTypeAndInstanceId(student.getId(), REWARD_TYPE, instanceId)
                .isPresent()) {

            StudentStats stats = getOrCreateStats(student);
            return RewardResult.alreadyGranted(stats);
        }

        StudentStats stats = getOrCreateStats(student);
        stats.setCourage(stats.getCourage() + COURAGE_REWARD);
        StudentStats savedStats = studentStatsRepository.save(stats);

        StudentStatRewardLog log = new StudentStatRewardLog();
        log.setStudent(student);
        log.setRewardType(REWARD_TYPE);
        log.setStatType("courage");
        log.setAmount(COURAGE_REWARD);
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

    private String buildInstanceId(Long readingRecordId) {
        return readingRecordId == null ? "unknown" : readingRecordId.toString();
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
