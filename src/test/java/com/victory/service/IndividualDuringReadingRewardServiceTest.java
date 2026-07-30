package com.victory.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.victory.entity.StudentStatRewardLog;
import com.victory.entity.StudentStats;
import com.victory.entity.User;
import com.victory.repository.StudentStatRewardLogRepository;
import com.victory.repository.StudentStatsRepository;

class IndividualDuringReadingRewardServiceTest {

    private final StudentStatsRepository studentStatsRepository = mock(StudentStatsRepository.class);
    private final StudentStatRewardLogRepository rewardLogRepository = mock(StudentStatRewardLogRepository.class);
    private final IndividualDuringReadingRewardService service =
        new IndividualDuringReadingRewardService(studentStatsRepository, rewardLogRepository);

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

    /* 최초 지급 시 마법력 +1, 지혜 +1만 오르고 체력/용기는 그대로 */
    @Test
    void grantDuringDailyRewardOnce_addsOneToMagicAndWisdomOnly() {
        User student = buildStudent(1L);
        StudentStats existing = buildStats(student, 8, 8, 8, 8);

        when(rewardLogRepository.findByStudent_IdAndRewardTypeAndInstanceId(1L, "INDIVIDUAL_DURING_DAILY_COMPLETE", "10:2026-07-27"))
            .thenReturn(Optional.empty());
        when(studentStatsRepository.findByStudent_Id(1L)).thenReturn(Optional.of(existing));
        when(studentStatsRepository.save(any(StudentStats.class))).thenAnswer(inv -> inv.getArgument(0));

        IndividualDuringReadingRewardService.RewardResult result =
            service.grantDuringDailyRewardOnce(student, 10L, DAY1);

        assertThat(result.isRewardGranted()).isTrue();
        assertThat(result.getStats().getMagic()).isEqualTo(9);
        assertThat(result.getStats().getWisdom()).isEqualTo(9);
        assertThat(result.getStats().getStamina()).isEqualTo(8);
        assertThat(result.getStats().getCourage()).isEqualTo(8);
    }

    /* 기존 능력치가 8이 아니어도 그 값에 그대로 누적되고 8로 초기화되지 않는다 */
    @Test
    void grantDuringDailyRewardOnce_accumulatesOnExistingValueWithoutResettingToEight() {
        User student = buildStudent(1L);
        StudentStats existing = buildStats(student, 15, 23, 20, 16);

        when(rewardLogRepository.findByStudent_IdAndRewardTypeAndInstanceId(1L, "INDIVIDUAL_DURING_DAILY_COMPLETE", "10:2026-07-27"))
            .thenReturn(Optional.empty());
        when(studentStatsRepository.findByStudent_Id(1L)).thenReturn(Optional.of(existing));
        when(studentStatsRepository.save(any(StudentStats.class))).thenAnswer(inv -> inv.getArgument(0));

        IndividualDuringReadingRewardService.RewardResult result =
            service.grantDuringDailyRewardOnce(student, 10L, DAY1);

        assertThat(result.getStats().getMagic()).isEqualTo(16);
        assertThat(result.getStats().getWisdom()).isEqualTo(21);
    }

    /* student_stats 행이 없는 학생은 8/8/8/8을 기본값으로 새로 만든 뒤 +1/+1을 얹는다 */
    @Test
    void grantDuringDailyRewardOnce_createsRowWithBaseEightWhenNoneExists() {
        User student = buildStudent(1L);

        when(rewardLogRepository.findByStudent_IdAndRewardTypeAndInstanceId(1L, "INDIVIDUAL_DURING_DAILY_COMPLETE", "10:2026-07-27"))
            .thenReturn(Optional.empty());
        when(studentStatsRepository.findByStudent_Id(1L)).thenReturn(Optional.empty());
        when(studentStatsRepository.save(any(StudentStats.class))).thenAnswer(inv -> inv.getArgument(0));

        IndividualDuringReadingRewardService.RewardResult result =
            service.grantDuringDailyRewardOnce(student, 10L, DAY1);

        assertThat(result.getStats().getMagic()).isEqualTo(9);
        assertThat(result.getStats().getStamina()).isEqualTo(8);
    }

    /* 같은 readingRecordId + 같은 날짜로 다시 호출하면(재저장/새로고침/API 재호출) 추가 지급 없음 */
    @Test
    void grantDuringDailyRewardOnce_doesNotGrantAgainForSameRecordAndDate() {
        User student = buildStudent(1L);
        StudentStats existing = buildStats(student, 9, 8, 9, 8);

        when(rewardLogRepository.findByStudent_IdAndRewardTypeAndInstanceId(1L, "INDIVIDUAL_DURING_DAILY_COMPLETE", "10:2026-07-27"))
            .thenReturn(Optional.of(new StudentStatRewardLog()));
        when(studentStatsRepository.findByStudent_Id(1L)).thenReturn(Optional.of(existing));

        IndividualDuringReadingRewardService.RewardResult result =
            service.grantDuringDailyRewardOnce(student, 10L, DAY1);

        assertThat(result.isRewardGranted()).isFalse();
        assertThat(result.isRewardAlreadyGranted()).isTrue();
        assertThat(result.getStats().getMagic()).isEqualTo(9);
        Mockito.verify(studentStatsRepository, never()).save(any(StudentStats.class));
        Mockito.verify(rewardLogRepository, never()).save(any(StudentStatRewardLog.class));
    }

    /* 같은 readingRecordId라도 날짜가 다르면(다음 날) 다시 지급된다 */
    @Test
    void grantDuringDailyRewardOnce_grantsAgainForSameRecordDifferentDate() {
        User student = buildStudent(1L);
        StudentStats existing = buildStats(student, 9, 8, 9, 8);

        when(rewardLogRepository.findByStudent_IdAndRewardTypeAndInstanceId(1L, "INDIVIDUAL_DURING_DAILY_COMPLETE", "10:2026-07-28"))
            .thenReturn(Optional.empty());
        when(studentStatsRepository.findByStudent_Id(1L)).thenReturn(Optional.of(existing));
        when(studentStatsRepository.save(any(StudentStats.class))).thenAnswer(inv -> inv.getArgument(0));

        IndividualDuringReadingRewardService.RewardResult result =
            service.grantDuringDailyRewardOnce(student, 10L, DAY2);

        assertThat(result.isRewardGranted()).isTrue();
        assertThat(result.getStats().getMagic()).isEqualTo(10);
        assertThat(result.getStats().getWisdom()).isEqualTo(10);
    }

    /* 같은 날짜라도 readingRecordId가 다르면(다른 책/재독) 독립적으로 지급된다 */
    @Test
    void grantDuringDailyRewardOnce_grantsIndependentlyForDifferentReadingRecordId() {
        User student = buildStudent(1L);
        StudentStats existing = buildStats(student, 9, 8, 9, 8);

        when(rewardLogRepository.findByStudent_IdAndRewardTypeAndInstanceId(1L, "INDIVIDUAL_DURING_DAILY_COMPLETE", "20:2026-07-27"))
            .thenReturn(Optional.empty());
        when(studentStatsRepository.findByStudent_Id(1L)).thenReturn(Optional.of(existing));
        when(studentStatsRepository.save(any(StudentStats.class))).thenAnswer(inv -> inv.getArgument(0));

        IndividualDuringReadingRewardService.RewardResult result =
            service.grantDuringDailyRewardOnce(student, 20L, DAY1);

        assertThat(result.isRewardGranted()).isTrue();
    }

    /* 학생 간 격리: 다른 학생의 동일 readingRecordId+날짜와 서로 영향을 주지 않는다 */
    @Test
    void grantDuringDailyRewardOnce_isolatesRewardLogByStudentId() {
        User studentA = buildStudent(1L);
        User studentB = buildStudent(2L);

        when(rewardLogRepository.findByStudent_IdAndRewardTypeAndInstanceId(1L, "INDIVIDUAL_DURING_DAILY_COMPLETE", "10:2026-07-27"))
            .thenReturn(Optional.of(new StudentStatRewardLog()));
        when(rewardLogRepository.findByStudent_IdAndRewardTypeAndInstanceId(2L, "INDIVIDUAL_DURING_DAILY_COMPLETE", "10:2026-07-27"))
            .thenReturn(Optional.empty());
        when(studentStatsRepository.findByStudent_Id(1L)).thenReturn(Optional.of(buildStats(studentA, 9, 8, 9, 8)));
        when(studentStatsRepository.findByStudent_Id(2L)).thenReturn(Optional.of(buildStats(studentB, 8, 8, 8, 8)));
        when(studentStatsRepository.save(any(StudentStats.class))).thenAnswer(inv -> inv.getArgument(0));

        IndividualDuringReadingRewardService.RewardResult resultA =
            service.grantDuringDailyRewardOnce(studentA, 10L, DAY1);
        IndividualDuringReadingRewardService.RewardResult resultB =
            service.grantDuringDailyRewardOnce(studentB, 10L, DAY1);

        assertThat(resultA.isRewardAlreadyGranted()).isTrue();
        assertThat(resultB.isRewardGranted()).isTrue();
        assertThat(resultB.getStats().getMagic()).isEqualTo(9);
    }

    /*
     * 보상 로그 저장이 실패하면 예외가 그대로 밖으로 전파된다(@Transactional이
     * 실제 운영 환경에서 이 예외 전파로 롤백을 유발해 능력치 증가분도 함께 취소한다).
     */
    @Test
    void grantDuringDailyRewardOnce_propagatesExceptionWhenRewardLogSaveFails() {
        User student = buildStudent(1L);
        StudentStats existing = buildStats(student, 8, 8, 8, 8);

        when(rewardLogRepository.findByStudent_IdAndRewardTypeAndInstanceId(1L, "INDIVIDUAL_DURING_DAILY_COMPLETE", "10:2026-07-27"))
            .thenReturn(Optional.empty());
        when(studentStatsRepository.findByStudent_Id(1L)).thenReturn(Optional.of(existing));
        when(studentStatsRepository.save(any(StudentStats.class))).thenAnswer(inv -> inv.getArgument(0));
        when(rewardLogRepository.save(any(StudentStatRewardLog.class)))
            .thenThrow(new RuntimeException("DB 저장 실패(unique 제약 위반 등)"));

        assertThatThrownBy(() -> service.grantDuringDailyRewardOnce(student, 10L, DAY1))
            .isInstanceOf(RuntimeException.class);
    }

    /* 다른 개별읽기/연습읽기 보상과 reward_type이 겹치지 않는다 */
    @Test
    void rewardType_doesNotConflictWithOtherRewards() {
        assertThat(IndividualDuringReadingRewardService.REWARD_TYPE)
            .isNotEqualTo(PracticeReadingRewardService.REWARD_TYPE)
            .isNotEqualTo(IndividualBeforeReadingRewardService.REWARD_TYPE);
    }
}
