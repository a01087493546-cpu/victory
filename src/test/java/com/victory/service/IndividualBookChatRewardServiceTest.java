package com.victory.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.victory.entity.StudentStatRewardLog;
import com.victory.entity.StudentStats;
import com.victory.entity.User;
import com.victory.repository.StudentStatRewardLogRepository;
import com.victory.repository.StudentStatsRepository;

class IndividualBookChatRewardServiceTest {

    private final StudentStatsRepository studentStatsRepository = mock(StudentStatsRepository.class);
    private final StudentStatRewardLogRepository rewardLogRepository = mock(StudentStatRewardLogRepository.class);
    private final IndividualBookChatRewardService service =
        new IndividualBookChatRewardService(studentStatsRepository, rewardLogRepository);

    private static final LocalDate DAY1 = LocalDate.of(2026, 7, 27);
    private static final LocalDate DAY2 = LocalDate.of(2026, 7, 28);

    private User buildStudent(Long id) {
        User student = new User();
        student.setId(id);
        student.setRole("student");
        return student;
    }

    private StudentStats buildStats(User student, int magic, int stamina, int wisdom, int courage) {
        StudentStats stats = new StudentStats();
        stats.setStudent(student);
        stats.setMagic(magic);
        stats.setStamina(stamina);
        stats.setWisdom(wisdom);
        stats.setCourage(courage);
        return stats;
    }

    /* 최초 지급 시 용기 +1만 오르고 나머지 능력치는 그대로 */
    @Test
    void grantPostDailyRewardOnce_addsOneToCourageOnly() {
        User student = buildStudent(1L);
        StudentStats existing = buildStats(student, 8, 8, 8, 8);

        when(rewardLogRepository.findByStudent_IdAndRewardTypeAndInstanceId(1L, "INDIVIDUAL_BOOK_CHAT_POST_DAILY", "2026-07-27"))
            .thenReturn(Optional.empty());
        when(studentStatsRepository.findByStudent_Id(1L)).thenReturn(Optional.of(existing));
        when(studentStatsRepository.save(any(StudentStats.class))).thenAnswer(inv -> inv.getArgument(0));

        IndividualBookChatRewardService.RewardResult result = service.grantPostDailyRewardOnce(student, DAY1);

        assertThat(result.isRewardGranted()).isTrue();
        assertThat(result.getStats().getCourage()).isEqualTo(9);
        assertThat(result.getStats().getMagic()).isEqualTo(8);
        assertThat(result.getStats().getStamina()).isEqualTo(8);
        assertThat(result.getStats().getWisdom()).isEqualTo(8);
    }

    /* 기존 능력치가 8이 아니어도 그 값에 그대로 누적되고 8로 초기화되지 않는다 */
    @Test
    void grantPostDailyRewardOnce_accumulatesOnExistingValueWithoutResettingToEight() {
        User student = buildStudent(1L);
        StudentStats existing = buildStats(student, 15, 23, 20, 16);

        when(rewardLogRepository.findByStudent_IdAndRewardTypeAndInstanceId(1L, "INDIVIDUAL_BOOK_CHAT_POST_DAILY", "2026-07-27"))
            .thenReturn(Optional.empty());
        when(studentStatsRepository.findByStudent_Id(1L)).thenReturn(Optional.of(existing));
        when(studentStatsRepository.save(any(StudentStats.class))).thenAnswer(inv -> inv.getArgument(0));

        IndividualBookChatRewardService.RewardResult result = service.grantPostDailyRewardOnce(student, DAY1);

        assertThat(result.getStats().getCourage()).isEqualTo(17);
    }

    /* student_stats 행이 없는 학생은 8/8/8/8을 기본값으로 새로 만든 뒤 용기 +1을 얹는다 */
    @Test
    void grantPostDailyRewardOnce_createsRowWithBaseEightWhenNoneExists() {
        User student = buildStudent(1L);

        when(rewardLogRepository.findByStudent_IdAndRewardTypeAndInstanceId(1L, "INDIVIDUAL_BOOK_CHAT_POST_DAILY", "2026-07-27"))
            .thenReturn(Optional.empty());
        when(studentStatsRepository.findByStudent_Id(1L)).thenReturn(Optional.empty());
        when(studentStatsRepository.save(any(StudentStats.class))).thenAnswer(inv -> inv.getArgument(0));

        IndividualBookChatRewardService.RewardResult result = service.grantPostDailyRewardOnce(student, DAY1);

        assertThat(result.getStats().getCourage()).isEqualTo(9);
        assertThat(result.getStats().getMagic()).isEqualTo(8);
    }

    /* 같은 날짜로 다시 호출하면(같은 날 글 재작성/재호출) 추가 지급 없음 */
    @Test
    void grantPostDailyRewardOnce_doesNotGrantAgainForSameDate() {
        User student = buildStudent(1L);
        StudentStats existing = buildStats(student, 8, 8, 8, 9);

        when(rewardLogRepository.findByStudent_IdAndRewardTypeAndInstanceId(1L, "INDIVIDUAL_BOOK_CHAT_POST_DAILY", "2026-07-27"))
            .thenReturn(Optional.of(new StudentStatRewardLog()));
        when(studentStatsRepository.findByStudent_Id(1L)).thenReturn(Optional.of(existing));

        IndividualBookChatRewardService.RewardResult result = service.grantPostDailyRewardOnce(student, DAY1);

        assertThat(result.isRewardGranted()).isFalse();
        assertThat(result.isRewardAlreadyGranted()).isTrue();
        assertThat(result.getStats().getCourage()).isEqualTo(9);
        verify(studentStatsRepository, never()).save(any(StudentStats.class));
        verify(rewardLogRepository, never()).save(any(StudentStatRewardLog.class));
    }

    /* 다음 날 다시 글을 쓰면 다시 지급된다 */
    @Test
    void grantPostDailyRewardOnce_grantsAgainOnNextDay() {
        User student = buildStudent(1L);
        StudentStats existing = buildStats(student, 8, 8, 8, 9);

        when(rewardLogRepository.findByStudent_IdAndRewardTypeAndInstanceId(1L, "INDIVIDUAL_BOOK_CHAT_POST_DAILY", "2026-07-28"))
            .thenReturn(Optional.empty());
        when(studentStatsRepository.findByStudent_Id(1L)).thenReturn(Optional.of(existing));
        when(studentStatsRepository.save(any(StudentStats.class))).thenAnswer(inv -> inv.getArgument(0));

        IndividualBookChatRewardService.RewardResult result = service.grantPostDailyRewardOnce(student, DAY2);

        assertThat(result.isRewardGranted()).isTrue();
        assertThat(result.getStats().getCourage()).isEqualTo(10);
    }

    /* 학생 간 격리: 다른 학생의 동일 날짜 보상 여부와 서로 영향 없음 */
    @Test
    void grantPostDailyRewardOnce_isolatesByStudentId() {
        User studentA = buildStudent(1L);
        User studentB = buildStudent(2L);

        when(rewardLogRepository.findByStudent_IdAndRewardTypeAndInstanceId(1L, "INDIVIDUAL_BOOK_CHAT_POST_DAILY", "2026-07-27"))
            .thenReturn(Optional.of(new StudentStatRewardLog()));
        when(rewardLogRepository.findByStudent_IdAndRewardTypeAndInstanceId(2L, "INDIVIDUAL_BOOK_CHAT_POST_DAILY", "2026-07-27"))
            .thenReturn(Optional.empty());
        when(studentStatsRepository.findByStudent_Id(1L)).thenReturn(Optional.of(buildStats(studentA, 8, 8, 8, 9)));
        when(studentStatsRepository.findByStudent_Id(2L)).thenReturn(Optional.of(buildStats(studentB, 8, 8, 8, 8)));
        when(studentStatsRepository.save(any(StudentStats.class))).thenAnswer(inv -> inv.getArgument(0));

        IndividualBookChatRewardService.RewardResult resultA = service.grantPostDailyRewardOnce(studentA, DAY1);
        IndividualBookChatRewardService.RewardResult resultB = service.grantPostDailyRewardOnce(studentB, DAY1);

        assertThat(resultA.isRewardAlreadyGranted()).isTrue();
        assertThat(resultB.isRewardGranted()).isTrue();
        assertThat(resultB.getStats().getCourage()).isEqualTo(9);
    }

    /* 보상 로그 저장이 실패하면 예외가 그대로 전파된다(@Transactional이 롤백을 유발) */
    @Test
    void grantPostDailyRewardOnce_propagatesExceptionWhenRewardLogSaveFails() {
        User student = buildStudent(1L);
        StudentStats existing = buildStats(student, 8, 8, 8, 8);

        when(rewardLogRepository.findByStudent_IdAndRewardTypeAndInstanceId(1L, "INDIVIDUAL_BOOK_CHAT_POST_DAILY", "2026-07-27"))
            .thenReturn(Optional.empty());
        when(studentStatsRepository.findByStudent_Id(1L)).thenReturn(Optional.of(existing));
        when(studentStatsRepository.save(any(StudentStats.class))).thenAnswer(inv -> inv.getArgument(0));
        when(rewardLogRepository.save(any(StudentStatRewardLog.class)))
            .thenThrow(new RuntimeException("DB 저장 실패(unique 제약 위반 등)"));

        assertThatThrownBy(() -> service.grantPostDailyRewardOnce(student, DAY1))
            .isInstanceOf(RuntimeException.class);
    }

    /* 다른 개별읽기 보상과 reward_type이 겹치지 않는다 */
    @Test
    void rewardType_doesNotConflictWithOtherRewards() {
        assertThat(IndividualBookChatRewardService.REWARD_TYPE)
            .isNotEqualTo(PracticeReadingRewardService.REWARD_TYPE)
            .isNotEqualTo(IndividualBeforeReadingRewardService.REWARD_TYPE)
            .isNotEqualTo(IndividualDuringReadingRewardService.REWARD_TYPE);
    }

    /* 글쓰기 보상과 댓글 보상은 reward_type이 서로 다르다 */
    @Test
    void commentRewardType_differsFromPostRewardType() {
        assertThat(IndividualBookChatRewardService.REWARD_TYPE_COMMENT)
            .isNotEqualTo(IndividualBookChatRewardService.REWARD_TYPE);
    }

    /* 검증 2/3: 댓글을 남기면 하루 첫 댓글에서 용기 +1이 지급된다(Asia/Seoul 날짜 기준 instanceId) */
    @Test
    void grantCommentDailyRewardOnce_addsOneToCourageOnFirstCommentOfDay() {
        User student = buildStudent(1L);
        StudentStats existing = buildStats(student, 8, 8, 8, 8);

        when(rewardLogRepository.findByStudent_IdAndRewardTypeAndInstanceId(
                1L, "INDIVIDUAL_BOOK_CHAT_COMMENT_DAILY", "2026-07-27"))
            .thenReturn(Optional.empty());
        when(studentStatsRepository.findByStudent_Id(1L)).thenReturn(Optional.of(existing));
        when(studentStatsRepository.save(any(StudentStats.class))).thenAnswer(inv -> inv.getArgument(0));

        IndividualBookChatRewardService.RewardResult result = service.grantCommentDailyRewardOnce(student, DAY1);

        assertThat(result.isRewardGranted()).isTrue();
        assertThat(result.getStats().getCourage()).isEqualTo(9);
    }

    /* 검증 4/5: 같은 날 댓글을 여러 번 남겨도(예: 다른 글에) 용기 보상은 하루 1회만 지급된다 */
    @Test
    void grantCommentDailyRewardOnce_doesNotGrantAgainForSameDate() {
        User student = buildStudent(1L);
        StudentStats existing = buildStats(student, 8, 8, 8, 9);

        when(rewardLogRepository.findByStudent_IdAndRewardTypeAndInstanceId(
                1L, "INDIVIDUAL_BOOK_CHAT_COMMENT_DAILY", "2026-07-27"))
            .thenReturn(Optional.of(new StudentStatRewardLog()));
        when(studentStatsRepository.findByStudent_Id(1L)).thenReturn(Optional.of(existing));

        IndividualBookChatRewardService.RewardResult result = service.grantCommentDailyRewardOnce(student, DAY1);

        assertThat(result.isRewardGranted()).isFalse();
        assertThat(result.isRewardAlreadyGranted()).isTrue();
        assertThat(result.getStats().getCourage()).isEqualTo(9);
        verify(studentStatsRepository, never()).save(any(StudentStats.class));
        verify(rewardLogRepository, never()).save(any(StudentStatRewardLog.class));
    }

    /* 글쓰기 보상과 댓글 보상은 reward_type이 달라 서로 간섭하지 않는다 -
       같은 날 글도 쓰고 댓글도 남기면 용기가 각각 +1씩, 총 +2까지 오를 수 있다 */
    @Test
    void postRewardAndCommentReward_areIndependentOnSameDay() {
        User student = buildStudent(1L);
        StudentStats afterPostReward = buildStats(student, 8, 8, 8, 9);

        when(rewardLogRepository.findByStudent_IdAndRewardTypeAndInstanceId(
                1L, "INDIVIDUAL_BOOK_CHAT_POST_DAILY", "2026-07-27"))
            .thenReturn(Optional.empty());
        when(rewardLogRepository.findByStudent_IdAndRewardTypeAndInstanceId(
                1L, "INDIVIDUAL_BOOK_CHAT_COMMENT_DAILY", "2026-07-27"))
            .thenReturn(Optional.empty());

        when(studentStatsRepository.findByStudent_Id(1L))
            .thenReturn(Optional.of(buildStats(student, 8, 8, 8, 8)))
            .thenReturn(Optional.of(afterPostReward));
        when(studentStatsRepository.save(any(StudentStats.class))).thenAnswer(inv -> inv.getArgument(0));

        IndividualBookChatRewardService.RewardResult postResult = service.grantPostDailyRewardOnce(student, DAY1);
        IndividualBookChatRewardService.RewardResult commentResult = service.grantCommentDailyRewardOnce(student, DAY1);

        assertThat(postResult.isRewardGranted()).isTrue();
        assertThat(postResult.getStats().getCourage()).isEqualTo(9);
        assertThat(commentResult.isRewardGranted()).isTrue();
        assertThat(commentResult.getStats().getCourage()).isEqualTo(10);
    }
}
