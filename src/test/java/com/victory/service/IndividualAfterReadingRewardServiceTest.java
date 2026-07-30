package com.victory.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.victory.entity.StudentStatRewardLog;
import com.victory.entity.StudentStats;
import com.victory.entity.User;
import com.victory.repository.StudentStatRewardLogRepository;
import com.victory.repository.StudentStatsRepository;

class IndividualAfterReadingRewardServiceTest {

    private final StudentStatsRepository studentStatsRepository = mock(StudentStatsRepository.class);
    private final StudentStatRewardLogRepository rewardLogRepository = mock(StudentStatRewardLogRepository.class);
    private final IndividualAfterReadingRewardService service =
        new IndividualAfterReadingRewardService(studentStatsRepository, rewardLogRepository);

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

    /* 검증 11-14: 최초 지급 시 체력 +3, 마법력 +1, 지혜 +1만 오르고 용기는 그대로 */
    @Test
    void grantAfterCompleteRewardOnce_addsStaminaThreeMagicOneWisdomOne() {
        User student = buildStudent(1L);
        StudentStats existing = buildStats(student, 8, 8, 8, 8);

        when(rewardLogRepository.findByStudent_IdAndRewardTypeAndInstanceId(1L, "INDIVIDUAL_AFTER_COMPLETE", "10"))
            .thenReturn(Optional.empty());
        when(studentStatsRepository.findByStudent_Id(1L)).thenReturn(Optional.of(existing));
        when(studentStatsRepository.save(any(StudentStats.class))).thenAnswer(inv -> inv.getArgument(0));

        IndividualAfterReadingRewardService.RewardResult result =
            service.grantAfterCompleteRewardOnce(student, 10L);

        assertThat(result.isRewardGranted()).isTrue();
        assertThat(result.isRewardAlreadyGranted()).isFalse();
        assertThat(result.getStats().getStamina()).isEqualTo(11);
        assertThat(result.getStats().getMagic()).isEqualTo(9);
        assertThat(result.getStats().getWisdom()).isEqualTo(9);
        assertThat(result.getStats().getCourage()).isEqualTo(8);
    }

    /* 검증: student_stats 행이 아직 없는 학생은 8/8/8/8을 기본값으로 새로 만든 뒤 보상을 얹는다 */
    @Test
    void grantAfterCompleteRewardOnce_createsRowWithBaseEightWhenNoneExists() {
        User student = buildStudent(1L);

        when(rewardLogRepository.findByStudent_IdAndRewardTypeAndInstanceId(1L, "INDIVIDUAL_AFTER_COMPLETE", "10"))
            .thenReturn(Optional.empty());
        when(studentStatsRepository.findByStudent_Id(1L)).thenReturn(Optional.empty());
        when(studentStatsRepository.save(any(StudentStats.class))).thenAnswer(inv -> inv.getArgument(0));

        IndividualAfterReadingRewardService.RewardResult result =
            service.grantAfterCompleteRewardOnce(student, 10L);

        assertThat(result.getStats().getStamina()).isEqualTo(11);
        assertThat(result.getStats().getMagic()).isEqualTo(9);
        assertThat(result.getStats().getWisdom()).isEqualTo(9);
        assertThat(result.getStats().getCourage()).isEqualTo(8);
    }

    /* 검증 15: 같은 readingRecordId로 다시 호출하면(완료 버튼 재클릭, 새로고침 후 재요청) 로그가 이미 있으므로 추가 지급 없음 */
    @Test
    void grantAfterCompleteRewardOnce_doesNotGrantAgainForSameReadingRecordId() {
        User student = buildStudent(1L);
        StudentStats existing = buildStats(student, 9, 11, 9, 8);

        when(rewardLogRepository.findByStudent_IdAndRewardTypeAndInstanceId(1L, "INDIVIDUAL_AFTER_COMPLETE", "10"))
            .thenReturn(Optional.of(new StudentStatRewardLog()));
        when(studentStatsRepository.findByStudent_Id(1L)).thenReturn(Optional.of(existing));

        IndividualAfterReadingRewardService.RewardResult result =
            service.grantAfterCompleteRewardOnce(student, 10L);

        assertThat(result.isRewardGranted()).isFalse();
        assertThat(result.isRewardAlreadyGranted()).isTrue();
        assertThat(result.getStats().getStamina()).isEqualTo(11);
        assertThat(result.getStats().getMagic()).isEqualTo(9);
        assertThat(result.getStats().getWisdom()).isEqualTo(9);
        verify(studentStatsRepository, never()).save(any(StudentStats.class));
        verify(rewardLogRepository, never()).save(any(StudentStatRewardLog.class));
    }

    /* 검증 16: 다른 readingRecordId(다른 책)는 각각 별도로 보상 지급 가능하다 */
    @Test
    void grantAfterCompleteRewardOnce_grantsSeparatelyForDifferentReadingRecordId() {
        User student = buildStudent(1L);
        StudentStats existing = buildStats(student, 9, 11, 9, 8);

        when(rewardLogRepository.findByStudent_IdAndRewardTypeAndInstanceId(1L, "INDIVIDUAL_AFTER_COMPLETE", "20"))
            .thenReturn(Optional.empty());
        when(studentStatsRepository.findByStudent_Id(1L)).thenReturn(Optional.of(existing));
        when(studentStatsRepository.save(any(StudentStats.class))).thenAnswer(inv -> inv.getArgument(0));

        IndividualAfterReadingRewardService.RewardResult result =
            service.grantAfterCompleteRewardOnce(student, 20L);

        assertThat(result.isRewardGranted()).isTrue();
        assertThat(result.getStats().getStamina()).isEqualTo(14);
        assertThat(result.getStats().getMagic()).isEqualTo(10);
        assertThat(result.getStats().getWisdom()).isEqualTo(10);
    }

    /* 검증: 다른 학생의 readingRecordId(우연히 같은 값)와 완전히 분리된다 */
    @Test
    void grantAfterCompleteRewardOnce_isolatesRewardLogByStudentId() {
        User studentA = buildStudent(1L);
        User studentB = buildStudent(2L);

        when(rewardLogRepository.findByStudent_IdAndRewardTypeAndInstanceId(1L, "INDIVIDUAL_AFTER_COMPLETE", "10"))
            .thenReturn(Optional.of(new StudentStatRewardLog()));
        when(rewardLogRepository.findByStudent_IdAndRewardTypeAndInstanceId(2L, "INDIVIDUAL_AFTER_COMPLETE", "10"))
            .thenReturn(Optional.empty());
        when(studentStatsRepository.findByStudent_Id(1L)).thenReturn(Optional.of(buildStats(studentA, 9, 11, 9, 8)));
        when(studentStatsRepository.findByStudent_Id(2L)).thenReturn(Optional.of(buildStats(studentB, 8, 8, 8, 8)));
        when(studentStatsRepository.save(any(StudentStats.class))).thenAnswer(inv -> inv.getArgument(0));

        IndividualAfterReadingRewardService.RewardResult resultA =
            service.grantAfterCompleteRewardOnce(studentA, 10L);
        IndividualAfterReadingRewardService.RewardResult resultB =
            service.grantAfterCompleteRewardOnce(studentB, 10L);

        assertThat(resultA.isRewardAlreadyGranted()).isTrue();
        assertThat(resultB.isRewardGranted()).isTrue();
        assertThat(resultB.getStats().getStamina()).isEqualTo(11);
    }

    /*
     * 검증 20: 보상 로그 저장이 실패하면 예외가 그대로 전파된다(트랜잭션 롤백 전제 -
     * afterDone만 반영되고 보상 일부만 남는 상황을 방지하는 근거).
     */
    @Test
    void grantAfterCompleteRewardOnce_propagatesExceptionWhenRewardLogSaveFails() {
        User student = buildStudent(1L);
        StudentStats existing = buildStats(student, 8, 8, 8, 8);

        when(rewardLogRepository.findByStudent_IdAndRewardTypeAndInstanceId(1L, "INDIVIDUAL_AFTER_COMPLETE", "10"))
            .thenReturn(Optional.empty());
        when(studentStatsRepository.findByStudent_Id(1L)).thenReturn(Optional.of(existing));
        when(studentStatsRepository.save(any(StudentStats.class))).thenAnswer(inv -> inv.getArgument(0));
        when(rewardLogRepository.save(any(StudentStatRewardLog.class)))
            .thenThrow(new RuntimeException("DB 저장 실패(unique 제약 위반 등)"));

        assertThatThrownBy(() -> service.grantAfterCompleteRewardOnce(student, 10L))
            .isInstanceOf(RuntimeException.class);
    }

    /* 검증: 읽기 전/읽기 중 보상과 reward_type이 겹치지 않는다(같은 readingRecordId라도 각 단계 보상이 독립적으로 지급됨) */
    @Test
    void rewardType_doesNotConflictWithBeforeOrDuringReward() {
        assertThat(IndividualAfterReadingRewardService.REWARD_TYPE)
            .isNotEqualTo(IndividualBeforeReadingRewardService.REWARD_TYPE);
        assertThat(IndividualAfterReadingRewardService.REWARD_TYPE)
            .isNotEqualTo(IndividualDuringReadingRewardService.REWARD_TYPE);
    }
}
