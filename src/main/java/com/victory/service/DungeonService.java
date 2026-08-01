package com.victory.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.victory.dto.BattleResultResponse;
import com.victory.dto.DungeonResponse;
import com.victory.dto.StudentStatsResponse;
import com.victory.entity.Dungeon;
import com.victory.entity.DungeonRecord;
import com.victory.entity.User;
import com.victory.repository.DungeonRecordRepository;
import com.victory.repository.DungeonRepository;
import com.victory.repository.ReadingRecordRepository;
import com.victory.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DungeonService {

    /*
     * IndividualReadingService와 동일하게 "오늘"은 서버 시스템 타임존이 아니라
     * Asia/Seoul 고정 기준으로 계산한다(오늘 자정 판정이 배포 서버 위치에 따라
     * 달라지지 않도록).
     */
    private static final ZoneId ZONE_SEOUL = ZoneId.of("Asia/Seoul");
    private static final int MAX_ATTEMPTS_PER_DAY = 3;
    private static final String RESULT_VICTORY = "victory";
    private static final List<String> VALID_RESULTS = List.of("victory", "defeat", "timeout");

    private final DungeonRepository dungeonRepository;
    private final DungeonRecordRepository dungeonRecordRepository;
    private final ReadingRecordRepository readingRecordRepository;
    private final UserRepository userRepository;
    private final StudentStatsService studentStatsService;

    public List<DungeonResponse> getDungeonsForStudent(Long studentId) {
        List<Dungeon> dungeons = dungeonRepository.findAll();

        long bookCount = readingRecordRepository.countByStudent_IdAndFinishedAtIsNotNull(studentId);
        double statAverage = studentStatsService.getStatAverage(studentId);
        LocalDateTime todayStart = LocalDate.now(ZONE_SEOUL).atStartOfDay();

        List<DungeonResponse> responses = new ArrayList<>();

        for (Dungeon dungeon : dungeons) {
            responses.add(buildDungeonResponse(studentId, dungeon, bookCount, statAverage, todayStart));
        }

        return responses;
    }

    private DungeonResponse buildDungeonResponse(
            Long studentId,
            Dungeon dungeon,
            long bookCount,
            double statAverage,
            LocalDateTime todayStart) {

        int requiredBooks = nullSafe(dungeon.getRequiredBooks());
        int requiredStatAvg = nullSafe(dungeon.getRequiredStatAvg());

        boolean cleared = dungeonRecordRepository
            .existsByStudent_IdAndDungeon_IdAndResult(studentId, dungeon.getId(), RESULT_VICTORY);

        Dungeon prerequisite = dungeon.getPrerequisiteDungeon();
        boolean prerequisiteCleared = prerequisite == null
            || dungeonRecordRepository.existsByStudent_IdAndDungeon_IdAndResult(
                studentId, prerequisite.getId(), RESULT_VICTORY);

        long attemptsToday = dungeonRecordRepository
            .countByStudent_IdAndDungeon_IdAndPlayedAtAfter(studentId, dungeon.getId(), todayStart);
        long attemptsLeftToday = Math.max(0, MAX_ATTEMPTS_PER_DAY - attemptsToday);

        boolean enoughBooks = bookCount >= requiredBooks;
        boolean enoughStats = statAverage >= requiredStatAvg;
        boolean eligible = enoughBooks && enoughStats && prerequisiteCleared && attemptsLeftToday > 0;

        List<String> blockedReasons = new ArrayList<>();

        if (!eligible) {
            if (!enoughBooks) {
                blockedReasons.add("책을 " + (requiredBooks - bookCount) + "권 더 읽어야 해요");
            }
            if (!enoughStats) {
                blockedReasons.add("능력치 평균이 부족해요");
            }
            if (!prerequisiteCleared) {
                blockedReasons.add("이전 던전을 먼저 클리어해야 해요");
            }
            if (attemptsLeftToday <= 0) {
                blockedReasons.add("오늘 도전 횟수를 다 썼어요");
            }
        }

        return new DungeonResponse(
            dungeon.getId(),
            dungeon.getName(),
            dungeon.getDescription(),
            dungeon.getDifficulty(),
            dungeon.getRequiredBooks(),
            dungeon.getRequiredStatAvg(),
            (int) bookCount,
            statAverage,
            cleared,
            eligible,
            blockedReasons,
            (int) attemptsLeftToday,
            nullSafe(dungeon.getRewardStatResetValue())
        );
    }

    @Transactional
    public BattleResultResponse submitBattleResult(Long studentId, Long dungeonId, String result) {
        if (!VALID_RESULTS.contains(result)) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "알 수 없는 전투 결과입니다. result=" + result);
        }

        LocalDateTime todayStart = LocalDate.now(ZONE_SEOUL).atStartOfDay();

        long attemptsToday = dungeonRecordRepository
            .countByStudent_IdAndDungeon_IdAndPlayedAtAfter(studentId, dungeonId, todayStart);

        if (attemptsToday >= MAX_ATTEMPTS_PER_DAY) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "오늘 이 던전은 더 도전할 수 없어요.");
        }

        User student = userRepository.findById(studentId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "학생 정보를 찾을 수 없습니다. studentId=" + studentId));

        Dungeon dungeon = dungeonRepository.findById(dungeonId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "던전 정보를 찾을 수 없습니다. dungeonId=" + dungeonId));

        DungeonRecord record = new DungeonRecord();
        record.setStudent(student);
        record.setDungeon(dungeon);
        record.setResult(result);
        dungeonRecordRepository.save(record);

        long attemptsLeftToday = Math.max(0, MAX_ATTEMPTS_PER_DAY - (attemptsToday + 1));

        boolean rewardApplied = false;
        boolean showEnding = false;
        StudentStatsResponse updatedStats = null;

        if (RESULT_VICTORY.equals(result)) {
            studentStatsService.applyReward(studentId, nullSafe(dungeon.getRewardStatResetValue()));
            rewardApplied = true;
            updatedStats = studentStatsService.getStats(studentId);
            showEnding = isLastStage(dungeon);
        }

        return new BattleResultResponse(result, rewardApplied, (int) attemptsLeftToday, showEnding, updatedStats);
    }

    /*
     * "마지막 단계 던전" = 다른 어떤 던전도 이 던전을 prerequisiteDungeon으로
     * 참조하지 않는 던전. 별도 카운트 쿼리를 추가하지 않고 findAll() 결과를
     * 그대로 재사용한다(getDungeonsForStudent와 동일하게 던전 개수가 적다는
     * 전제).
     */
    private boolean isLastStage(Dungeon dungeon) {
        return dungeonRepository.findAll().stream()
            .noneMatch(other -> other.getPrerequisiteDungeon() != null
                && other.getPrerequisiteDungeon().getId().equals(dungeon.getId()));
    }

    private int nullSafe(Integer value) {
        return value == null ? 0 : value;
    }
}
