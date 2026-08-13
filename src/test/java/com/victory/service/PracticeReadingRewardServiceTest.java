package com.victory.service;

import static org.assertj.core.api.Assertions.assertThat;
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

class PracticeReadingRewardServiceTest {

    private static final Long STUDENT_ID = 10L;
    private static final Long BOOK_ID = 30L;
    private final StudentStatsRepository statsRepository = mock(StudentStatsRepository.class);
    private final StudentStatRewardLogRepository logRepository = mock(StudentStatRewardLogRepository.class);
    private final StudentEndingService endingService = mock(StudentEndingService.class);
    private final PracticeReadingRewardService service = new PracticeReadingRewardService(
        statsRepository, logRepository, endingService);

    @Test
    void firstCompletionAddsEightToEveryStatAndCreatesOneLog() {
        User student = student();
        StudentStats stats = stats(student, 0);
        when(endingService.hasEnded(STUDENT_ID)).thenReturn(false);
        when(logRepository.findByStudent_Id(STUDENT_ID)).thenReturn(java.util.List.of());
        when(statsRepository.findByStudent_Id(STUDENT_ID)).thenReturn(Optional.of(stats));
        when(statsRepository.save(stats)).thenReturn(stats);

        var result = service.grantPracticeCompleteRewardOnce(student, BOOK_ID);

        assertThat(result.isRewardGranted()).isTrue();
        assertThat(result.isRewardAlreadyGranted()).isFalse();
        assertThat(stats.getMagic()).isEqualTo(8);
        assertThat(stats.getStamina()).isEqualTo(8);
        assertThat(stats.getWisdom()).isEqualTo(8);
        assertThat(stats.getCourage()).isEqualTo(8);
        verify(logRepository).save(any(StudentStatRewardLog.class));
    }

    @Test
    void repeatedCompletionDoesNotChangeStatsOrCreateAnotherLog() {
        User student = student();
        StudentStats stats = stats(student, 8);
        when(endingService.hasEnded(STUDENT_ID)).thenReturn(false);
        StudentStatRewardLog existingLog = new StudentStatRewardLog();
        existingLog.setRewardType(PracticeReadingRewardService.REWARD_TYPE);
        existingLog.setInstanceId("practice_reading:999");
        when(logRepository.findByStudent_Id(STUDENT_ID)).thenReturn(java.util.List.of(existingLog));
        when(statsRepository.findByStudent_Id(STUDENT_ID)).thenReturn(Optional.of(stats));

        var result = service.grantPracticeCompleteRewardOnce(student, BOOK_ID);

        assertThat(result.isRewardGranted()).isFalse();
        assertThat(result.isRewardAlreadyGranted()).isTrue();
        assertThat(stats.getMagic()).isEqualTo(8);
        assertThat(stats.getStamina()).isEqualTo(8);
        assertThat(stats.getWisdom()).isEqualTo(8);
        assertThat(stats.getCourage()).isEqualTo(8);
        verify(statsRepository, never()).save(any(StudentStats.class));
        verify(logRepository, never()).save(any(StudentStatRewardLog.class));
    }

    @Test
    void endingCompletedStudentFinishesActivityWithoutRewardOrPopupSignal() {
        User student = student();
        StudentStats stats = stats(student, 0);
        when(endingService.hasEnded(STUDENT_ID)).thenReturn(true);
        when(statsRepository.findByStudent_Id(STUDENT_ID)).thenReturn(Optional.of(stats));

        var result = service.grantPracticeCompleteRewardOnce(student, BOOK_ID);

        assertThat(result.isRewardGranted()).isFalse();
        assertThat(result.isRewardAlreadyGranted()).isTrue();
        assertThat(stats.getMagic()).isZero();
        assertThat(stats.getStamina()).isZero();
        assertThat(stats.getWisdom()).isZero();
        assertThat(stats.getCourage()).isZero();
        verify(logRepository, never()).save(any(StudentStatRewardLog.class));
    }

    private User student() {
        User student = new User();
        student.setId(STUDENT_ID);
        student.setRole("student");
        return student;
    }

    private StudentStats stats(User student, int value) {
        StudentStats stats = new StudentStats();
        stats.setStudent(student);
        stats.setMagic(value);
        stats.setStamina(value);
        stats.setWisdom(value);
        stats.setCourage(value);
        return stats;
    }
}
