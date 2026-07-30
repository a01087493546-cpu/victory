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
 * 개별읽기 책수다방(글쓰기/댓글) 일일 보상: 용기 +1. readingRecordId와 무관하게
 * "학생당 하루 1회"이므로 instance_id는 날짜 하나만 쓴다(studentId는
 * student_stat_reward_log의 student_id 컬럼으로 이미 구분됨) - 같은 날
 * 여러 번 글을 쓰거나 댓글을 남겨도 이 조합(student_id+reward_type+instance_id)
 * UNIQUE 제약 덕분에 하루 한 번만 지급된다.
 *
 * 글쓰기와 댓글은 reward_type이 서로 다르므로(REWARD_TYPE_POST /
 * REWARD_TYPE_COMMENT) 같은 날 글도 쓰고 댓글도 남기면 각각 +1씩,
 * 최대 용기 +2까지 지급될 수 있다 - 이는 의도된 동작이다.
 */
@Service
@RequiredArgsConstructor
public class IndividualBookChatRewardService {

    public static final String REWARD_TYPE = "INDIVIDUAL_BOOK_CHAT_POST_DAILY";
    public static final String REWARD_TYPE_COMMENT = "INDIVIDUAL_BOOK_CHAT_COMMENT_DAILY";
    private static final int COURAGE_REWARD = 1;

    /*
     * student_stats 행이 아직 없는 학생에게 이 보상으로 처음 행을 만들 때만
     * 쓰는 기본값이다. 이미 행이 있는 학생은 절대 이 값으로 덮어쓰지 않는다.
     */
    private static final int NEW_STUDENT_BASE_STAT = 8;

    private final StudentStatsRepository studentStatsRepository;
    private final StudentStatRewardLogRepository rewardLogRepository;

    @Transactional
    public RewardResult grantPostDailyRewardOnce(User student, LocalDate activityDate) {
        return grantDailyRewardOnce(student, activityDate, REWARD_TYPE);
    }

    @Transactional
    public RewardResult grantCommentDailyRewardOnce(User student, LocalDate activityDate) {
        return grantDailyRewardOnce(student, activityDate, REWARD_TYPE_COMMENT);
    }

    private RewardResult grantDailyRewardOnce(User student, LocalDate activityDate, String rewardType) {

        String instanceId = activityDate.toString();

        if (rewardLogRepository
                .findByStudent_IdAndRewardTypeAndInstanceId(student.getId(), rewardType, instanceId)
                .isPresent()) {

            StudentStats stats = getOrCreateStats(student);
            return RewardResult.alreadyGranted(stats);
        }

        StudentStats stats = getOrCreateStats(student);
        stats.setCourage(stats.getCourage() + COURAGE_REWARD);
        StudentStats savedStats = studentStatsRepository.save(stats);

        StudentStatRewardLog log = new StudentStatRewardLog();
        log.setStudent(student);
        log.setRewardType(rewardType);
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
