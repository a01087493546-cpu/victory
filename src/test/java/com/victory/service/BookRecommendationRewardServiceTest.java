package com.victory.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import com.victory.entity.StudentStatRewardLog;
import com.victory.entity.StudentStats;
import com.victory.entity.User;
import com.victory.repository.StudentStatRewardLogRepository;
import com.victory.repository.StudentStatsRepository;

class BookRecommendationRewardServiceTest {

    private final StudentStatsRepository studentStatsRepository = Mockito.mock(StudentStatsRepository.class);
    private final StudentStatRewardLogRepository rewardLogRepository =
        Mockito.mock(StudentStatRewardLogRepository.class);
    private final StudentEndingService studentEndingService = Mockito.mock(StudentEndingService.class);
    private final BookRecommendationRewardService service =
        new BookRecommendationRewardService(studentStatsRepository, rewardLogRepository, studentEndingService);

    private User buildStudent(Long id) {
        User student = new User();
        student.setId(id);
        student.setName("학생" + id);
        student.setRole("student");
        return student;
    }

    private StudentStats buildStats(User student, int courage) {
        StudentStats stats = new StudentStats();
        stats.setStudent(student);
        stats.setMagic(8);
        stats.setStamina(8);
        stats.setWisdom(8);
        stats.setCourage(courage);
        return stats;
    }

    @Test
    void grantRecommendationRewardOnce_addsCourageOneOnFirstRecommendation() {
        User student = buildStudent(1L);
        StudentStats stats = buildStats(student, 8);

        when(rewardLogRepository.findByStudent_IdAndRewardTypeAndInstanceId(
                1L, BookRecommendationRewardService.REWARD_TYPE, "10"))
            .thenReturn(Optional.empty());
        when(studentStatsRepository.findByStudent_Id(1L)).thenReturn(Optional.of(stats));
        when(studentStatsRepository.save(any(StudentStats.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BookRecommendationRewardService.RewardResult result =
            service.grantRecommendationRewardOnce(student, 10L);

        assertThat(result.isRewardGranted()).isTrue();
        assertThat(result.getStats().getCourage()).isEqualTo(9);

        ArgumentCaptor<StudentStatRewardLog> logCaptor = ArgumentCaptor.forClass(StudentStatRewardLog.class);
        verify(rewardLogRepository).save(logCaptor.capture());
        assertThat(logCaptor.getValue().getRewardType())
            .isEqualTo(BookRecommendationRewardService.REWARD_TYPE);
        assertThat(logCaptor.getValue().getStatType()).isEqualTo("courage");
        assertThat(logCaptor.getValue().getAmount()).isEqualTo(1);
        assertThat(logCaptor.getValue().getInstanceId()).isEqualTo("10");
    }

    @Test
    void grantRecommendationRewardOnce_doesNotGrantAgainForSameReadingRecordId() {
        User student = buildStudent(1L);
        StudentStats stats = buildStats(student, 9);

        when(rewardLogRepository.findByStudent_IdAndRewardTypeAndInstanceId(
                1L, BookRecommendationRewardService.REWARD_TYPE, "10"))
            .thenReturn(Optional.of(new StudentStatRewardLog()));
        when(studentStatsRepository.findByStudent_Id(1L)).thenReturn(Optional.of(stats));

        BookRecommendationRewardService.RewardResult result =
            service.grantRecommendationRewardOnce(student, 10L);

        assertThat(result.isRewardGranted()).isFalse();
        assertThat(result.isRewardAlreadyGranted()).isTrue();
        assertThat(result.getStats().getCourage()).isEqualTo(9);
        verify(studentStatsRepository, never()).save(any(StudentStats.class));
        verify(rewardLogRepository, never()).save(any(StudentStatRewardLog.class));
    }

    @Test
    void grantRecommendationRewardOnce_grantsSeparatelyForDifferentReadingRecordIdsOnSameDay() {
        User student = buildStudent(1L);
        StudentStats stats = buildStats(student, 8);

        when(studentStatsRepository.findByStudent_Id(1L)).thenReturn(Optional.of(stats));
        when(studentStatsRepository.save(any(StudentStats.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(rewardLogRepository.findByStudent_IdAndRewardTypeAndInstanceId(
                1L, BookRecommendationRewardService.REWARD_TYPE, "10"))
            .thenReturn(Optional.empty());
        when(rewardLogRepository.findByStudent_IdAndRewardTypeAndInstanceId(
                1L, BookRecommendationRewardService.REWARD_TYPE, "11"))
            .thenReturn(Optional.empty());

        BookRecommendationRewardService.RewardResult first =
            service.grantRecommendationRewardOnce(student, 10L);
        BookRecommendationRewardService.RewardResult second =
            service.grantRecommendationRewardOnce(student, 11L);

        assertThat(first.isRewardGranted()).isTrue();
        assertThat(second.isRewardGranted()).isTrue();
        assertThat(second.getStats().getCourage()).isEqualTo(10);
    }

    @Test
    void grantRecommendationRewardOnce_isIsolatedByStudentId() {
        User studentA = buildStudent(1L);
        User studentB = buildStudent(2L);

        when(rewardLogRepository.findByStudent_IdAndRewardTypeAndInstanceId(
                1L, BookRecommendationRewardService.REWARD_TYPE, "10"))
            .thenReturn(Optional.of(new StudentStatRewardLog()));
        when(rewardLogRepository.findByStudent_IdAndRewardTypeAndInstanceId(
                2L, BookRecommendationRewardService.REWARD_TYPE, "10"))
            .thenReturn(Optional.empty());
        when(studentStatsRepository.findByStudent_Id(1L)).thenReturn(Optional.of(buildStats(studentA, 9)));
        when(studentStatsRepository.findByStudent_Id(2L)).thenReturn(Optional.of(buildStats(studentB, 8)));
        when(studentStatsRepository.save(any(StudentStats.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BookRecommendationRewardService.RewardResult first =
            service.grantRecommendationRewardOnce(studentA, 10L);
        BookRecommendationRewardService.RewardResult second =
            service.grantRecommendationRewardOnce(studentB, 10L);

        assertThat(first.isRewardGranted()).isFalse();
        assertThat(second.isRewardGranted()).isTrue();
        assertThat(second.getStats().getCourage()).isEqualTo(9);
    }

    @Test
    void grantRecommendationRewardOnce_propagatesExceptionWhenRewardLogSaveFails() {
        User student = buildStudent(1L);
        StudentStats stats = buildStats(student, 8);

        when(rewardLogRepository.findByStudent_IdAndRewardTypeAndInstanceId(
                1L, BookRecommendationRewardService.REWARD_TYPE, "10"))
            .thenReturn(Optional.empty());
        when(studentStatsRepository.findByStudent_Id(1L)).thenReturn(Optional.of(stats));
        when(studentStatsRepository.save(any(StudentStats.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(rewardLogRepository.save(any(StudentStatRewardLog.class)))
            .thenThrow(new IllegalStateException("duplicate reward"));

        assertThatThrownBy(() -> service.grantRecommendationRewardOnce(student, 10L))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("duplicate reward");
    }

    /* 엔딩을 이미 본 학생은 용기가 더 이상 오르지 않고 보상 로그도 남지 않는다 */
    @Test
    void grantRecommendationRewardOnce_doesNotGrantAfterStudentHasSeenEnding() {
        User student = buildStudent(1L);
        StudentStats existing = buildStats(student, 0);

        when(studentEndingService.hasEnded(1L)).thenReturn(true);
        when(studentStatsRepository.findByStudent_Id(1L)).thenReturn(Optional.of(existing));

        BookRecommendationRewardService.RewardResult result =
            service.grantRecommendationRewardOnce(student, 10L);

        assertThat(result.isRewardGranted()).isFalse();
        assertThat(result.getStats().getCourage()).isEqualTo(0);
        verify(studentStatsRepository, never()).save(any(StudentStats.class));
        verify(rewardLogRepository, never()).save(any(StudentStatRewardLog.class));
    }
}
