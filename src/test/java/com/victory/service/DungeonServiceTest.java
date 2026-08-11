package com.victory.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.victory.dto.BattleResultResponse;
import com.victory.dto.StudentStatsResponse;
import com.victory.entity.Dungeon;
import com.victory.entity.User;
import com.victory.repository.DungeonRecordRepository;
import com.victory.repository.DungeonRepository;
import com.victory.repository.ReadingRecordRepository;
import com.victory.repository.UserRepository;

/*
 * 던전 클리어 보상 리셋 정책 회귀 방지 테스트.
 *
 * 핵심 버그: rewardStatResetValue가 없는 던전(고급/최종 단계)을 클리어했을 때
 * nullSafe()가 null을 0으로 바꿔 넘기는 바람에 applyReward(studentId, 0)이
 * 호출되어 학생 능력치가 전부 0으로 초기화되던 문제. 고급 클리어는 능력치를
 * 절대 건드리지 않고 엔딩만 트리거해야 한다.
 */
@ExtendWith(MockitoExtension.class)
class DungeonServiceTest {

    private static final Long STUDENT_ID = 1L;

    @Mock private DungeonRepository dungeonRepository;
    @Mock private DungeonRecordRepository dungeonRecordRepository;
    @Mock private ReadingRecordRepository readingRecordRepository;
    @Mock private UserRepository userRepository;
    @Mock private StudentStatsService studentStatsService;
    @Mock private DemoAccountService demoAccountService;
    @Mock private StudentEndingService studentEndingService;

    private DungeonService service;

    @BeforeEach
    void setUp() {
        service = new DungeonService(
            dungeonRepository, dungeonRecordRepository, readingRecordRepository,
            userRepository, studentStatsService, demoAccountService, studentEndingService);

        User student = new User();
        student.setId(STUDENT_ID);
        student.setRole("student");

        when(demoAccountService.isDemoAccount(STUDENT_ID)).thenReturn(false);
        when(userRepository.findById(STUDENT_ID)).thenReturn(Optional.of(student));
        when(dungeonRecordRepository.countByStudent_IdAndDungeon_IdAndPlayedAtAfter(
            eq(STUDENT_ID), any(), any(LocalDateTime.class))).thenReturn(0L);
        org.mockito.Mockito.lenient().when(studentStatsService.getStats(STUDENT_ID)).thenReturn(
            new StudentStatsResponse(10, 10, 10, 10, false));
        org.mockito.Mockito.lenient().when(studentEndingService.hasEnded(STUDENT_ID)).thenReturn(false);
    }

    private Dungeon dungeonWithReward(Long id, Integer rewardStatResetValue, Dungeon prerequisite) {
        Dungeon dungeon = new Dungeon();
        dungeon.setId(id);
        dungeon.setName("테스트 던전 " + id);
        dungeon.setRewardStatResetValue(rewardStatResetValue);
        dungeon.setPrerequisiteDungeon(prerequisite);
        return dungeon;
    }

    @Test
    void 중급_클리어는_기존값과_무관하게_보상값으로_능력치를_리셋한다() {
        Dungeon beginner = dungeonWithReward(1L, 10, null);
        Dungeon intermediate = dungeonWithReward(2L, 15, beginner);

        when(dungeonRepository.findById(2L)).thenReturn(Optional.of(intermediate));
        when(dungeonRepository.findAll()).thenReturn(List.of(beginner, intermediate));

        BattleResultResponse response = service.submitBattleResult(STUDENT_ID, 2L, "victory");

        verify(studentStatsService, times(1)).applyReward(STUDENT_ID, 15);
        assertThat(response.isRewardApplied()).isTrue();
    }

    @Test
    void 고급_최종단계_클리어는_능력치를_리셋하지_않고_엔딩만_켠다() {
        Dungeon beginner = dungeonWithReward(1L, 10, null);
        Dungeon intermediate = dungeonWithReward(2L, 15, beginner);
        Dungeon advanced = dungeonWithReward(3L, null, intermediate);

        when(dungeonRepository.findById(3L)).thenReturn(Optional.of(advanced));
        when(dungeonRepository.findAll()).thenReturn(List.of(beginner, intermediate, advanced));

        BattleResultResponse response = service.submitBattleResult(STUDENT_ID, 3L, "victory");

        verify(studentStatsService, never()).applyReward(eq(STUDENT_ID), anyInt());
        assertThat(response.isShowEnding()).isTrue();
    }

    @Test
    void 패배는_능력치를_건드리지_않는다() {
        Dungeon beginner = dungeonWithReward(1L, 10, null);

        when(dungeonRepository.findById(1L)).thenReturn(Optional.of(beginner));

        BattleResultResponse response = service.submitBattleResult(STUDENT_ID, 1L, "defeat");

        verify(studentStatsService, never()).applyReward(eq(STUDENT_ID), anyInt());
        assertThat(response.isRewardApplied()).isFalse();
        assertThat(response.getUpdatedStats()).isNull();
    }

    @Test
    void 엔딩을_이미_본_학생은_초급을_재클리어해도_능력치가_리셋되지_않는다() {
        Dungeon beginner = dungeonWithReward(1L, 10, null);

        when(dungeonRepository.findById(1L)).thenReturn(Optional.of(beginner));
        when(studentEndingService.hasEnded(STUDENT_ID)).thenReturn(true);

        BattleResultResponse response = service.submitBattleResult(STUDENT_ID, 1L, "victory");

        verify(studentStatsService, never()).applyReward(eq(STUDENT_ID), anyInt());
        assertThat(response.isRewardApplied()).isTrue();
    }

    @Test
    void 엔딩을_이미_본_학생이_고급을_다시_이겨도_엔딩을_다시_보여주지_않는다() {
        Dungeon beginner = dungeonWithReward(1L, 10, null);
        Dungeon intermediate = dungeonWithReward(2L, 15, beginner);
        Dungeon advanced = dungeonWithReward(3L, null, intermediate);

        when(dungeonRepository.findById(3L)).thenReturn(Optional.of(advanced));
        when(dungeonRepository.findAll()).thenReturn(List.of(beginner, intermediate, advanced));
        when(studentEndingService.hasEnded(STUDENT_ID)).thenReturn(true);

        BattleResultResponse response = service.submitBattleResult(STUDENT_ID, 3L, "victory");

        assertThat(response.isShowEnding()).isFalse();
    }
}
