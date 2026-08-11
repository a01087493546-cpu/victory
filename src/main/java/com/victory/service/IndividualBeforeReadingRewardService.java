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
 * 개별읽기 읽기 전(before) 4단계 최초 완료 보상: 마법력 +1, 지혜 +1.
 * PracticeReadingRewardService와 같은 구조(reward_type + instance_id로
 * student_stat_reward_log에 1행만 남겨 중복 지급을 막는다)를 따르되,
 * reward_type을 다르게 써서("INDIVIDUAL_BEFORE_COMPLETE") 서로 충돌하지 않는다.
 * instance_id는 readingRecordId 그대로 사용한다 - 같은 책을 재독해 새
 * readingRecordId가 생기면 다시 지급 가능해야 하기 때문이다(책 재독 = 새 독서 세션).
 */
@Service
@RequiredArgsConstructor
public class IndividualBeforeReadingRewardService {

    public static final String REWARD_TYPE = "INDIVIDUAL_BEFORE_COMPLETE";
    private static final int MAGIC_REWARD = 1;
    private static final int WISDOM_REWARD = 1;

    /*
     * student_stats 행이 아직 없는 학생에게 이 보상으로 처음 행을 만들 때만
     * 쓰는 기본값이다. 이미 행이 있는 학생은 절대 이 값으로 덮어쓰지 않는다
     * (getOrCreateStats가 기존 행을 찾으면 그대로 반환하고 새로 만들지 않음).
     */
    private static final int NEW_STUDENT_BASE_STAT = 8;

    private final StudentStatsRepository studentStatsRepository;
    private final StudentStatRewardLogRepository rewardLogRepository;
    private final StudentEndingService studentEndingService;

    @Transactional
    public RewardResult grantBeforeCompleteRewardOnce(User student, Long readingRecordId) {

        /*
         * 고급 던전 엔딩을 끝까지 본 학생은 능력치 시스템이 종료됐으므로
         * 이후 독서활동은 정상 저장되지만 능력치는 더 이상 지급하지 않는다
         * (student_stat_reward_log에도 새 행을 남기지 않는다).
         */
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
