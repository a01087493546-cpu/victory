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
 * 개별읽기 읽기 후(간추리기) 최초 완료 보상: 체력 +3, 마법력 +1, 지혜 +1.
 * IndividualBeforeReadingRewardService와 같은 구조(reward_type + instance_id로
 * student_stat_reward_log에 1행만 남겨 중복 지급을 막는다)를 그대로 따른다.
 * instance_id는 readingRecordId 그대로 사용한다 - 같은 책을 재독해 새
 * readingRecordId가 생기면 다시 지급 가능해야 한다(책 재독 = 새 독서 세션).
 *
 * 책 완독 처리(IndividualReadingService.completeReadingRecord)는 이 보상과
 * 완전히 별개다 - 완독 시에는 이 서비스를 절대 호출하지 않는다(추가 보상 없음).
 */
@Service
@RequiredArgsConstructor
public class IndividualAfterReadingRewardService {

    public static final String REWARD_TYPE = "INDIVIDUAL_AFTER_COMPLETE";
    private static final int STAMINA_REWARD = 3;
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
    public RewardResult grantAfterCompleteRewardOnce(User student, Long readingRecordId) {

        if (studentEndingService.hasEnded(student.getId())) {
            return RewardResult.alreadyGranted(getOrCreateStats(student));
        }

        String instanceId = buildInstanceId(readingRecordId);

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
        stats.setStamina(stats.getStamina() + STAMINA_REWARD);
        stats.setMagic(stats.getMagic() + MAGIC_REWARD);
        stats.setWisdom(stats.getWisdom() + WISDOM_REWARD);
        StudentStats savedStats = studentStatsRepository.save(stats);

        StudentStatRewardLog log = new StudentStatRewardLog();
        log.setStudent(student);
        log.setRewardType(REWARD_TYPE);
        log.setStatType("stamina_magic_wisdom");
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
