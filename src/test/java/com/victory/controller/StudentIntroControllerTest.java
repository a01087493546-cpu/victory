package com.victory.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import com.victory.entity.User;
import com.victory.repository.UserRepository;
import com.victory.service.StudentStatsService;

/*
 * 엔딩(고급 던전 클리어 후 ending-intro.html 완료) 처리 - has_seen_ending을
 * true로 남기고 능력치를 0으로 SET하는지 검증한다. 실제 DB(ss01 등 공유
 * 테스트 계정)를 이 API로 직접 건드리면 hasSeenEnding이 영구히 true로
 * 남아 이후 다른 회귀 테스트를 오염시키므로, 이 검증은 반드시 순수
 * Mockito 단위테스트로만 한다(라이브 curl 호출 금지).
 */
class StudentIntroControllerTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final StudentStatsService studentStatsService = mock(StudentStatsService.class);
    private final StudentIntroController controller =
        new StudentIntroController(userRepository, studentStatsService);

    private Authentication authFor(Long studentId) {
        return new UsernamePasswordAuthenticationToken(
            studentId, null, List.of(new SimpleGrantedAuthority("student")));
    }

    @Test
    void markStoryIntroSeen_setsFlagAndFlushesBeforeResponding() {
        Long studentId = 1L;
        User student = new User();
        student.setId(studentId);
        student.setRole("student");
        student.setHasSeenStoryIntro(false);

        when(userRepository.findById(studentId)).thenReturn(Optional.of(student));

        controller.markStoryIntroSeen(authFor(studentId));

        assertThat(student.getHasSeenStoryIntro()).isTrue();
        verify(userRepository).saveAndFlush(student);
    }

    @Test
    void markPowerIntroSeen_setsFlagAndFlushesBeforeResponding() {
        Long studentId = 1L;
        User student = new User();
        student.setId(studentId);
        student.setRole("student");
        student.setHasSeenPowerIntro(false);

        when(userRepository.findById(studentId)).thenReturn(Optional.of(student));

        controller.markPowerIntroSeen(authFor(studentId));

        assertThat(student.getHasSeenPowerIntro()).isTrue();
        verify(userRepository).saveAndFlush(student);
    }

    @Test
    void getIntroStatus_returnsBothPersistedFlagsIndependently() {
        Long studentId = 1L;
        User student = new User();
        student.setId(studentId);
        student.setRole("student");
        student.setHasSeenStoryIntro(true);
        student.setHasSeenPowerIntro(false);

        when(userRepository.findById(studentId)).thenReturn(Optional.of(student));

        var status = controller.getIntroStatus(authFor(studentId));

        assertThat(status.get("hasSeenStoryIntro")).isTrue();
        assertThat(status.get("hasSeenPowerIntro")).isFalse();
    }

    @Test
    void markEndingSeen_setsHasSeenEndingTrueAndZeroesStats() {
        Long studentId = 1L;
        User student = new User();
        student.setId(studentId);
        student.setRole("student");
        student.setHasSeenEnding(false);

        when(userRepository.findById(studentId)).thenReturn(Optional.of(student));

        controller.markEndingSeen(authFor(studentId));

        assertThat(student.getHasSeenEnding()).isTrue();
        verify(userRepository).save(student);
        verify(studentStatsService).applyReward(studentId, 0);
    }

    @Test
    void markEndingSeen_isIdempotentWhenCalledAgain() {
        Long studentId = 1L;
        User student = new User();
        student.setId(studentId);
        student.setRole("student");
        student.setHasSeenEnding(true);

        when(userRepository.findById(studentId)).thenReturn(Optional.of(student));

        controller.markEndingSeen(authFor(studentId));

        assertThat(student.getHasSeenEnding()).isTrue();
        verify(studentStatsService).applyReward(studentId, 0);
    }

    @Test
    void markEndingSeen_neverTouchesOtherStudentsStats() {
        Long studentId = 1L;
        User student = new User();
        student.setId(studentId);
        student.setRole("student");

        when(userRepository.findById(studentId)).thenReturn(Optional.of(student));

        controller.markEndingSeen(authFor(studentId));

        verify(studentStatsService, never()).applyReward(eq(2L), any(Integer.class));
    }
}
