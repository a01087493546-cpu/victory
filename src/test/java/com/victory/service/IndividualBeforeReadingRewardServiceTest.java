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

class IndividualBeforeReadingRewardServiceTest {

    private final StudentStatsRepository studentStatsRepository = mock(StudentStatsRepository.class);
    private final StudentStatRewardLogRepository rewardLogRepository = mock(StudentStatRewardLogRepository.class);
    private final IndividualBeforeReadingRewardService service =
        new IndividualBeforeReadingRewardService(studentStatsRepository, rewardLogRepository);

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

    /* 검증 2: 최초 지급 시 마법력 +1, 지혜 +1만 오르고 체력/용기는 그대로 */
    @Test
    void grantBeforeCompleteRewardOnce_addsOneToMagicAndWisdomOnly() {
        User student = buildStudent(1L);
        StudentStats existing = buildStats(student, 8, 8, 8, 8);

        when(rewardLogRepository.findByStudent_IdAndRewardTypeAndInstanceId(1L, "INDIVIDUAL_BEFORE_COMPLETE", "10"))
            .thenReturn(Optional.empty());
        when(studentStatsRepository.findByStudent_Id(1L)).thenReturn(Optional.of(existing));
        when(studentStatsRepository.save(any(StudentStats.class))).thenAnswer(inv -> inv.getArgument(0));

        IndividualBeforeReadingRewardService.RewardResult result =
            service.grantBeforeCompleteRewardOnce(student, 10L);

        assertThat(result.isRewardGranted()).isTrue();
        assertThat(result.isRewardAlreadyGranted()).isFalse();
        assertThat(result.getStats().getMagic()).isEqualTo(9);
        assertThat(result.getStats().getWisdom()).isEqualTo(9);
        assertThat(result.getStats().getStamina()).isEqualTo(8);
        assertThat(result.getStats().getCourage()).isEqualTo(8);
    }

    /* 검증 9: 기존 능력치가 8이 아니어도(예: 여러 보상을 이미 받은 상태) 그 값에 그대로 누적되고 8로 초기화되지 않는다 */
    @Test
    void grantBeforeCompleteRewardOnce_accumulatesOnExistingValueWithoutResettingToEight() {
        User student = buildStudent(1L);
        StudentStats existing = buildStats(student, 15, 23, 20, 16);

        when(rewardLogRepository.findByStudent_IdAndRewardTypeAndInstanceId(1L, "INDIVIDUAL_BEFORE_COMPLETE", "10"))
            .thenReturn(Optional.empty());
        when(studentStatsRepository.findByStudent_Id(1L)).thenReturn(Optional.of(existing));
        when(studentStatsRepository.save(any(StudentStats.class))).thenAnswer(inv -> inv.getArgument(0));

        IndividualBeforeReadingRewardService.RewardResult result =
            service.grantBeforeCompleteRewardOnce(student, 10L);

        assertThat(result.getStats().getMagic()).isEqualTo(16);
        assertThat(result.getStats().getWisdom()).isEqualTo(21);
        assertThat(result.getStats().getStamina()).isEqualTo(23);
        assertThat(result.getStats().getCourage()).isEqualTo(16);
    }

    /* 검증: student_stats 행 자체가 아직 없는 학생(가입 직후 등)은 8/8/8/8을 기본값으로 새로 만든 뒤 +1/+1을 얹는다 */
    @Test
    void grantBeforeCompleteRewardOnce_createsRowWithBaseEightWhenNoneExists() {
        User student = buildStudent(1L);

        when(rewardLogRepository.findByStudent_IdAndRewardTypeAndInstanceId(1L, "INDIVIDUAL_BEFORE_COMPLETE", "10"))
            .thenReturn(Optional.empty());
        when(studentStatsRepository.findByStudent_Id(1L)).thenReturn(Optional.empty());
        when(studentStatsRepository.save(any(StudentStats.class))).thenAnswer(inv -> inv.getArgument(0));

        IndividualBeforeReadingRewardService.RewardResult result =
            service.grantBeforeCompleteRewardOnce(student, 10L);

        assertThat(result.getStats().getMagic()).isEqualTo(9);
        assertThat(result.getStats().getWisdom()).isEqualTo(9);
        assertThat(result.getStats().getStamina()).isEqualTo(8);
        assertThat(result.getStats().getCourage()).isEqualTo(8);
    }

    /* 검증 3·4·5: 같은 readingRecordId로 다시 호출하면(동일 API 재호출/새로고침 후 재실행) 로그가 이미 있으므로 추가 지급 없음 */
    @Test
    void grantBeforeCompleteRewardOnce_doesNotGrantAgainForSameReadingRecordId() {
        User student = buildStudent(1L);
        StudentStats existing = buildStats(student, 9, 8, 9, 8);

        when(rewardLogRepository.findByStudent_IdAndRewardTypeAndInstanceId(1L, "INDIVIDUAL_BEFORE_COMPLETE", "10"))
            .thenReturn(Optional.of(new StudentStatRewardLog()));
        when(studentStatsRepository.findByStudent_Id(1L)).thenReturn(Optional.of(existing));

        IndividualBeforeReadingRewardService.RewardResult result =
            service.grantBeforeCompleteRewardOnce(student, 10L);

        assertThat(result.isRewardGranted()).isFalse();
        assertThat(result.isRewardAlreadyGranted()).isTrue();
        assertThat(result.getStats().getMagic()).isEqualTo(9);
        assertThat(result.getStats().getWisdom()).isEqualTo(9);
        verify(studentStatsRepository, never()).save(any(StudentStats.class));
        verify(rewardLogRepository, never()).save(any(StudentStatRewardLog.class));
    }

    /* 검증 6: 같은 책을 새 readingRecordId로 재독하면 dedup 키(instanceId)가 달라지므로 다시 지급된다 */
    @Test
    void grantBeforeCompleteRewardOnce_grantsAgainForNewReadingRecordIdOfRereading() {
        User student = buildStudent(1L);
        StudentStats existing = buildStats(student, 9, 8, 9, 8);

        when(rewardLogRepository.findByStudent_IdAndRewardTypeAndInstanceId(1L, "INDIVIDUAL_BEFORE_COMPLETE", "20"))
            .thenReturn(Optional.empty());
        when(studentStatsRepository.findByStudent_Id(1L)).thenReturn(Optional.of(existing));
        when(studentStatsRepository.save(any(StudentStats.class))).thenAnswer(inv -> inv.getArgument(0));

        IndividualBeforeReadingRewardService.RewardResult result =
            service.grantBeforeCompleteRewardOnce(student, 20L);

        assertThat(result.isRewardGranted()).isTrue();
        assertThat(result.getStats().getMagic()).isEqualTo(10);
        assertThat(result.getStats().getWisdom()).isEqualTo(10);
    }

    /* 검증 7: 다른 학생의 readingRecordId(우연히 같은 값)와 완전히 분리된다 - dedup 조회 자체가 studentId로 걸린다 */
    @Test
    void grantBeforeCompleteRewardOnce_isolatesRewardLogByStudentId() {
        User studentA = buildStudent(1L);
        User studentB = buildStudent(2L);

        when(rewardLogRepository.findByStudent_IdAndRewardTypeAndInstanceId(1L, "INDIVIDUAL_BEFORE_COMPLETE", "10"))
            .thenReturn(Optional.of(new StudentStatRewardLog()));
        when(rewardLogRepository.findByStudent_IdAndRewardTypeAndInstanceId(2L, "INDIVIDUAL_BEFORE_COMPLETE", "10"))
            .thenReturn(Optional.empty());
        when(studentStatsRepository.findByStudent_Id(1L)).thenReturn(Optional.of(buildStats(studentA, 9, 8, 9, 8)));
        when(studentStatsRepository.findByStudent_Id(2L)).thenReturn(Optional.of(buildStats(studentB, 8, 8, 8, 8)));
        when(studentStatsRepository.save(any(StudentStats.class))).thenAnswer(inv -> inv.getArgument(0));

        IndividualBeforeReadingRewardService.RewardResult resultA =
            service.grantBeforeCompleteRewardOnce(studentA, 10L);
        IndividualBeforeReadingRewardService.RewardResult resultB =
            service.grantBeforeCompleteRewardOnce(studentB, 10L);

        assertThat(resultA.isRewardAlreadyGranted()).isTrue();
        assertThat(resultB.isRewardGranted()).isTrue();
        assertThat(resultB.getStats().getMagic()).isEqualTo(9);
    }

    /*
     * 검증 8: 보상 로그 저장이 실패하면 예외가 그대로 밖으로 전파되어야 한다.
     * 이 메서드에는 @Transactional이 붙어 있으므로, 실제 운영 환경에서는 이
     * 예외 전파 자체가 트랜잭션 롤백을 유발해 방금 save()한 능력치 증가분도
     * 함께 취소된다(순수 Mockito 단위테스트라 실제 롤백은 검증할 수 없고,
     * "예외를 삼키지 않고 그대로 던진다"는 전제 조건만 여기서 확인한다).
     */
    @Test
    void grantBeforeCompleteRewardOnce_propagatesExceptionWhenRewardLogSaveFails() {
        User student = buildStudent(1L);
        StudentStats existing = buildStats(student, 8, 8, 8, 8);

        when(rewardLogRepository.findByStudent_IdAndRewardTypeAndInstanceId(1L, "INDIVIDUAL_BEFORE_COMPLETE", "10"))
            .thenReturn(Optional.empty());
        when(studentStatsRepository.findByStudent_Id(1L)).thenReturn(Optional.of(existing));
        when(studentStatsRepository.save(any(StudentStats.class))).thenAnswer(inv -> inv.getArgument(0));
        when(rewardLogRepository.save(any(StudentStatRewardLog.class)))
            .thenThrow(new RuntimeException("DB 저장 실패(unique 제약 위반 등)"));

        assertThatThrownBy(() -> service.grantBeforeCompleteRewardOnce(student, 10L))
            .isInstanceOf(RuntimeException.class);
    }

    /* 검증 10: 연습읽기 +8 보상(PRACTICE_READING_COMPLETE)과 reward_type이 겹치지 않는다 */
    @Test
    void rewardType_doesNotConflictWithPracticeReadingReward() {
        assertThat(IndividualBeforeReadingRewardService.REWARD_TYPE)
            .isNotEqualTo(PracticeReadingRewardService.REWARD_TYPE);
    }
}
