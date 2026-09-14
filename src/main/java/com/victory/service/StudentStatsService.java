package com.victory.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.victory.dto.ReadingFocusResponse;
import com.victory.dto.StudentStatsResponse;
import com.victory.entity.StudentStats;
import com.victory.entity.User;
import com.victory.repository.StudentStatsRepository;
import com.victory.repository.UserRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudentStatsService {

    private final StudentStatsRepository studentStatsRepository;
    private final UserRepository userRepository;
    private final DemoAccountService demoAccountService;

    public StudentStatsResponse getStats(Long studentId) {
        User student = userRepository.findById(studentId)
            .orElseThrow(() -> new EntityNotFoundException(
                "학생을 찾을 수 없습니다. studentId=" + studentId));

        if (!"student".equalsIgnoreCase(student.getRole())) {
            throw new IllegalArgumentException("학생 계정만 능력치를 조회할 수 있습니다.");
        }

        boolean hasSeenEnding = Boolean.TRUE.equals(student.getHasSeenEnding());

        return studentStatsRepository.findByStudent_Id(studentId)
            .map(stats -> StudentStatsResponse.from(stats, hasSeenEnding))
            .orElseGet(() -> StudentStatsResponse.from(null, hasSeenEnding));
    }

    /*
     * 던전 승리 보상: magic/stamina/wisdom/courage 4개를 모두 value로 맞춘다.
     * 호출 측인 DungeonService가 dungeon.rewardStatResetValue를 그대로 넘긴다.
     */
    @Transactional
    public void applyReward(Long studentId, int value) {
        if (demoAccountService.isDemoAccount(studentId)) {
            return;
        }
        StudentStats stats = studentStatsRepository.findByStudent_Id(studentId)
            .orElseGet(() -> {
                User student = userRepository.findById(studentId)
                    .orElseThrow(() -> new EntityNotFoundException(
                        "학생을 찾을 수 없습니다. studentId=" + studentId));

                StudentStats created = new StudentStats();
                created.setStudent(student);
                return created;
            });

        stats.setMagic(value);
        stats.setStamina(value);
        stats.setWisdom(value);
        stats.setCourage(value);

        studentStatsRepository.save(stats);
    }

    public double getStatAverage(Long studentId) {
        return studentStatsRepository.findByStudent_Id(studentId)
            .map(stats -> (stats.getMagic() + stats.getStamina() + stats.getWisdom() + stats.getCourage()) / 4.0)
            .orElse(0.0);
    }

    /*
     * 독서 집중 시간 누적 초 조회. 연습읽기/개별읽기 구분 없이 학생당 하나의
     * 값으로 합산돼 있으므로(addReadingFocusSeconds 참고) 그대로 반환한다.
     * 아직 StudentStats 행이 없는 학생은 0으로 본다(getStats와 동일한 방식 -
     * 조회만으로 행을 새로 만들지 않는다).
     */
    public ReadingFocusResponse getReadingFocus(Long studentId) {
        long totalSeconds = studentStatsRepository.findByStudent_Id(studentId)
            .map(StudentStats::getTotalReadingFocusSeconds)
            .orElse(0L);

        return ReadingFocusResponse.of(totalSeconds);
    }

    /*
     * 독서 집중 시간 누적 초 추가. 심사계정은 브라우저 localStorage만
     * source of truth로 쓰기로 정책이 정해져 있어(reading-focus.html 참고)
     * 여기서 DB에 반영하지 않고 현재 누적값만 그대로 돌려준다 - 프런트도
     * 심사계정이면 이 API 자체를 호출하지 않지만, 혹시 호출되더라도 데모
     * 값이 실제 DB에 섞이지 않도록 서버에서도 한 번 더 막는다.
     */
    @Transactional
    public ReadingFocusResponse addReadingFocusSeconds(Long studentId, long addSeconds) {
        if (demoAccountService.isDemoAccount(studentId)) {
            return getReadingFocus(studentId);
        }

        StudentStats stats = studentStatsRepository.findByStudent_Id(studentId)
            .orElseGet(() -> {
                User student = userRepository.findById(studentId)
                    .orElseThrow(() -> new EntityNotFoundException(
                        "학생을 찾을 수 없습니다. studentId=" + studentId));

                StudentStats created = new StudentStats();
                created.setStudent(student);
                return created;
            });

        long currentSeconds = stats.getTotalReadingFocusSeconds() == null
            ? 0L
            : stats.getTotalReadingFocusSeconds();
        long updatedSeconds = currentSeconds + addSeconds;
        stats.setTotalReadingFocusSeconds(updatedSeconds);

        studentStatsRepository.save(stats);

        return ReadingFocusResponse.of(updatedSeconds);
    }
}
