package com.victory.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.victory.dto.PracticeProgressRequest;
import com.victory.dto.PracticeProgressResponse;
import com.victory.entity.ClassReadingBook;
import com.victory.entity.ClassStudent;
import com.victory.entity.PracticeProgress;
import com.victory.entity.SchoolClass;
import com.victory.entity.StudentStats;
import com.victory.entity.User;
import com.victory.repository.ClassReadingBookRepository;
import com.victory.repository.ClassStudentRepository;
import com.victory.repository.PracticeProgressRepository;
import com.victory.repository.UserRepository;
import com.victory.service.PracticeReadingRewardService.RewardResult;

class PracticeProgressServiceTest {

    private static final Long STUDENT_ID = 10L;
    private static final Long CLASS_ID = 20L;
    private static final Long BOOK_ID = 30L;

    private final PracticeProgressRepository progressRepository = mock(PracticeProgressRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final ClassStudentRepository classStudentRepository = mock(ClassStudentRepository.class);
    private final ClassReadingBookRepository classReadingBookRepository = mock(ClassReadingBookRepository.class);
    private final PracticeReadingRewardService rewardService = mock(PracticeReadingRewardService.class);
    private final PracticeProgressService service = new PracticeProgressService(
        progressRepository, userRepository, classStudentRepository, classReadingBookRepository, rewardService);

    @Test
    void saveProgress_afterDoneCompletesPracticeAndGrantsRewardWithoutLegacyDuringFlags() {
        User student = student();
        PracticeProgress progress = progress(student, false);
        StudentStats stats = new StudentStats();
        stats.setStudent(student);
        stats.setMagic(8);
        stats.setStamina(8);
        stats.setWisdom(8);
        stats.setCourage(8);

        when(userRepository.findById(STUDENT_ID)).thenReturn(Optional.of(student));
        when(progressRepository.findByStudent_Id(STUDENT_ID)).thenReturn(Optional.of(progress));
        when(progressRepository.save(progress)).thenReturn(progress);
        stubCurrentBook();
        when(rewardService.grantPracticeCompleteRewardOnce(student, BOOK_ID))
            .thenReturn(new RewardResult(true, false, stats));

        PracticeProgressRequest request = new PracticeProgressRequest();
        request.setAfterDone(true);

        PracticeProgressResponse response = service.saveProgress(STUDENT_ID, request);

        assertThat(response.getAfterDone()).isTrue();
        assertThat(response.getPracticeCompleted()).isTrue();
        assertThat(response.getRewardGranted()).isTrue();
        assertThat(response.getStats().getMagic()).isEqualTo(8);
        assertThat(response.getStats().getStamina()).isEqualTo(8);
        assertThat(response.getStats().getWisdom()).isEqualTo(8);
        assertThat(response.getStats().getCourage()).isEqualTo(8);
        verify(rewardService).grantPracticeCompleteRewardOnce(student, BOOK_ID);
    }

    @Test
    void saveProgress_beforeFinalCompletionDoesNotGrantOrUnlock() {
        User student = student();
        PracticeProgress progress = progress(student, false);
        when(userRepository.findById(STUDENT_ID)).thenReturn(Optional.of(student));
        when(progressRepository.findByStudent_Id(STUDENT_ID)).thenReturn(Optional.of(progress));
        when(progressRepository.save(progress)).thenReturn(progress);

        PracticeProgressRequest request = new PracticeProgressRequest();
        request.setBeforeDone(true);

        PracticeProgressResponse response = service.saveProgress(STUDENT_ID, request);

        assertThat(response.getAfterDone()).isFalse();
        assertThat(response.getPracticeCompleted()).isFalse();
        assertThat(response.getRewardGranted()).isFalse();
        verify(rewardService, never()).grantPracticeCompleteRewardOnce(any(), any());
    }

    @Test
    void getProgress_afterDoneReportsUnlockedWithoutGrantingAgain() {
        User student = student();
        PracticeProgress progress = progress(student, true);
        StudentStats stats = new StudentStats();
        stats.setStudent(student);

        when(userRepository.findById(STUDENT_ID)).thenReturn(Optional.of(student));
        when(progressRepository.findByStudent_Id(STUDENT_ID)).thenReturn(Optional.of(progress));
        stubCurrentBook();
        when(rewardService.getRewardState(student, BOOK_ID))
            .thenReturn(new RewardResult(false, true, stats));

        PracticeProgressResponse response = service.getProgress(STUDENT_ID);

        assertThat(response.getPracticeCompleted()).isTrue();
        assertThat(response.getRewardGranted()).isFalse();
        assertThat(response.getRewardAlreadyGranted()).isTrue();
        verify(rewardService, never()).grantPracticeCompleteRewardOnce(any(), any());
    }

    private User student() {
        User student = new User();
        student.setId(STUDENT_ID);
        student.setRole("student");
        return student;
    }

    private PracticeProgress progress(User student, boolean afterDone) {
        PracticeProgress progress = new PracticeProgress();
        progress.setStudent(student);
        progress.setBookSelected(false);
        progress.setBeforeDone(false);
        progress.setClassReadDone(false);
        progress.setAfterDone(afterDone);
        return progress;
    }

    private void stubCurrentBook() {
        SchoolClass schoolClass = new SchoolClass();
        schoolClass.setId(CLASS_ID);
        ClassStudent membership = new ClassStudent();
        membership.setSchoolClass(schoolClass);
        ClassReadingBook book = new ClassReadingBook();
        book.setId(BOOK_ID);
        when(classStudentRepository.findByStudentId(STUDENT_ID)).thenReturn(Optional.of(membership));
        when(classReadingBookRepository.findBySchoolClassId(CLASS_ID)).thenReturn(Optional.of(book));
    }
}
