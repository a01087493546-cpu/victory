package com.victory.service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.victory.entity.ClassReadingBook;
import com.victory.entity.ClassStudent;
import com.victory.entity.SchoolClass;
import com.victory.repository.ClassReadingBookRepository;
import com.victory.repository.ClassStudentRepository;
import com.victory.repository.SchoolClassRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/*
 * 연습읽기 성취도 그래프용 "그날 자정까지 누적된 값" 일별 스냅샷을 매일
 * Asia/Seoul 자정에 저장한다.
 *
 * 서버가 자정에 꺼져 있었던 경우(빠진 날짜 보정): 이 스케줄러는 앱이 뜰 때도
 * 한 번 같은 로직을 실행해서, "오늘" 스냅샷이 아직 없으면 지금 시점의 값으로
 * 채운다. 다만 실제로 놓친 과거 날짜(예: 이틀 전)의 "그날 자정 시점 값"은
 * responses/practice_progress 등 기존 데이터에 그 시점 스냅샷을 그대로
 * 재현할 수 있는 이력이 남아있지 않아(현재 값으로 덮어써지는 구조라 과거
 * 시점의 누적 쪽수/진행 상태를 그대로 복원할 수 없음) 임의로 지어내지
 * 않는다 - 놓친 과거 날짜는 그래프에 빈 구간으로 남고, "오늘" 스냅샷만
 * 최소한으로 보정해서 채운다. saveSnapshotIfAbsent 자체가 이미 있는
 * 날짜는 절대 덮어쓰지 않으므로(그리고 DB UNIQUE 제약이 최후 방어선),
 * 스케줄러가 여러 번/여러 인스턴스에서 겹쳐 실행돼도 안전하다.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PracticeAchievementSnapshotScheduler {

    private static final ZoneId ZONE_SEOUL = ZoneId.of("Asia/Seoul");

    private final PracticeAchievementService practiceAchievementService;
    private final ClassStudentRepository classStudentRepository;
    private final ClassReadingBookRepository classReadingBookRepository;
    private final SchoolClassRepository schoolClassRepository;

    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
    public void runDailySnapshot() {
        snapshotAllStudents(LocalDate.now(ZONE_SEOUL));
    }

    /*
     * 서버 재시작(자정에 꺼져 있었던 경우 포함) 직후 "오늘" 스냅샷이 없으면
     * 채워 넣는다. saveSnapshotIfAbsent가 idempotent라 정상적으로 자정에
     * 실행됐던 날은 여기서 그냥 아무 일도 하지 않는다.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void catchUpMissingTodaySnapshotOnStartup() {
        snapshotAllStudents(LocalDate.now(ZONE_SEOUL));
    }

    private void snapshotAllStudents(LocalDate snapshotDate) {
        List<SchoolClass> classes = schoolClassRepository.findAll();

        for (SchoolClass schoolClass : classes) {
            Optional<ClassReadingBook> classReadingBook =
                classReadingBookRepository.findBySchoolClassId(schoolClass.getId());

            if (classReadingBook.isEmpty()) {
                continue;
            }

            ClassReadingBook book = classReadingBook.get();
            List<ClassStudent> roster =
                classStudentRepository.findBySchoolClassId(schoolClass.getId());

            for (ClassStudent classStudent : roster) {
                Long studentId = classStudent.getStudent().getId();

                try {
                    practiceAchievementService.saveSnapshotIfAbsent(
                        studentId, book.getId(), book, snapshotDate);
                } catch (Exception e) {
                    log.error(
                        "연습읽기 성취도 스냅샷 저장 실패 studentId={} classReadingBookId={} date={}",
                        studentId, book.getId(), snapshotDate, e
                    );
                }
            }
        }
    }
}
