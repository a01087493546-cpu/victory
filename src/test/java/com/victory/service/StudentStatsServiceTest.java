package com.victory.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.victory.dto.ReadingFocusAddRequest;
import com.victory.dto.ReadingFocusResponse;
import com.victory.entity.StudentStats;
import com.victory.entity.User;
import com.victory.repository.StudentStatsRepository;
import com.victory.repository.UserRepository;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

/*
 * 독서 집중 시간 누적(getReadingFocus/addReadingFocusSeconds) 전용 테스트.
 * 기존 getStats/applyReward/getStatAverage는 이번 작업에서 손대지 않았으므로
 * 별도 테스트를 추가하지 않는다.
 */
class StudentStatsServiceTest {

    private static final Long STUDENT_A_ID = 10L;
    private static final Long STUDENT_B_ID = 20L;

    private final StudentStatsRepository studentStatsRepository = mock(StudentStatsRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final DemoAccountService demoAccountService = mock(DemoAccountService.class);
    private final StudentStatsService service =
        new StudentStatsService(studentStatsRepository, userRepository, demoAccountService);

    private User student(Long id) {
        User user = new User();
        user.setId(id);
        user.setRole("student");
        return user;
    }

    private StudentStats statsOf(User student, long totalSeconds) {
        StudentStats stats = new StudentStats();
        stats.setStudent(student);
        stats.setTotalReadingFocusSeconds(totalSeconds);
        return stats;
    }

    /* 1. 초기값 0: 아직 StudentStats 행이 없는 학생은 0으로 조회된다 */
    @Test
    void getReadingFocus_returnsZeroWhenNoRowExists() {
        when(studentStatsRepository.findByStudent_Id(STUDENT_A_ID)).thenReturn(Optional.empty());

        ReadingFocusResponse response = service.getReadingFocus(STUDENT_A_ID);

        assertThat(response.getTotalSeconds()).isZero();
    }

    /* 2. 600초 추가 -> 600 (행이 없던 학생은 User를 찾아 새로 만든 뒤 저장한다) */
    @Test
    void addReadingFocusSeconds_createsRowAndAddsWhenNoneExists() {
        User student = student(STUDENT_A_ID);
        when(studentStatsRepository.findByStudent_Id(STUDENT_A_ID)).thenReturn(Optional.empty());
        when(userRepository.findById(STUDENT_A_ID)).thenReturn(Optional.of(student));
        when(studentStatsRepository.save(any(StudentStats.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        ReadingFocusResponse response = service.addReadingFocusSeconds(STUDENT_A_ID, 600L);

        assertThat(response.getTotalSeconds()).isEqualTo(600L);
        verify(studentStatsRepository).save(any(StudentStats.class));
    }

    /* 3. 이미 600초가 쌓인 상태에서 300초를 더 추가하면 900초가 된다 */
    @Test
    void addReadingFocusSeconds_addsToExistingTotal() {
        User student = student(STUDENT_A_ID);
        StudentStats existing = statsOf(student, 600L);
        when(studentStatsRepository.findByStudent_Id(STUDENT_A_ID)).thenReturn(Optional.of(existing));
        when(studentStatsRepository.save(any(StudentStats.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        ReadingFocusResponse response = service.addReadingFocusSeconds(STUDENT_A_ID, 300L);

        assertThat(response.getTotalSeconds()).isEqualTo(900L);
    }

    /* 4. 학생 A/B 값 분리: 서로 다른 studentId는 서로 다른 저장 행을 조회해 완전히 분리된다 */
    @Test
    void getReadingFocus_isIsolatedPerStudent() {
        when(studentStatsRepository.findByStudent_Id(STUDENT_A_ID))
            .thenReturn(Optional.of(statsOf(student(STUDENT_A_ID), 900L)));
        when(studentStatsRepository.findByStudent_Id(STUDENT_B_ID))
            .thenReturn(Optional.of(statsOf(student(STUDENT_B_ID), 120L)));

        assertThat(service.getReadingFocus(STUDENT_A_ID).getTotalSeconds()).isEqualTo(900L);
        assertThat(service.getReadingFocus(STUDENT_B_ID).getTotalSeconds()).isEqualTo(120L);
    }

    /*
     * 5. 연습읽기 10분 + 개별읽기 20분처럼, 어느 화면(context)에서 왔는지
     * 구분하지 않고 같은 학생의 값에 계속 더해져 하나로 합산된다.
     */
    @Test
    void addReadingFocusSeconds_accumulatesAcrossPracticeAndIndividualContexts() {
        User student = student(STUDENT_A_ID);
        StudentStats existing = statsOf(student, 0L);
        when(studentStatsRepository.findByStudent_Id(STUDENT_A_ID)).thenReturn(Optional.of(existing));
        when(studentStatsRepository.save(any(StudentStats.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        service.addReadingFocusSeconds(STUDENT_A_ID, 600L);
        ReadingFocusResponse response = service.addReadingFocusSeconds(STUDENT_A_ID, 1200L);

        assertThat(response.getTotalSeconds()).isEqualTo(1800L);
    }

    /* 6. 심사계정은 DB에 반영하지 않고 현재 값만 그대로 돌려준다(저장 호출 자체가 없음) */
    @Test
    void addReadingFocusSeconds_doesNotPersistForDemoAccount() {
        when(demoAccountService.isDemoAccount(STUDENT_A_ID)).thenReturn(true);
        when(studentStatsRepository.findByStudent_Id(STUDENT_A_ID))
            .thenReturn(Optional.of(statsOf(student(STUDENT_A_ID), 500L)));

        ReadingFocusResponse response = service.addReadingFocusSeconds(STUDENT_A_ID, 600L);

        assertThat(response.getTotalSeconds()).isEqualTo(500L);
        verify(studentStatsRepository, never()).save(any(StudentStats.class));
    }

    /* 7. 일반계정은 정상적으로 DB에 저장된다 (2/3/5번 테스트가 곧 일반계정 정상 동작 확인) */

    /* 8. 음수 add 차단: 컨트롤러 진입 전 @Valid(@Min(0))에서 이미 거부된다 */
    @Test
    void readingFocusAddRequest_rejectsNegativeAddSeconds() throws Exception {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            Validator validator = factory.getValidator();

            assertThat(validator.validate(buildAddRequest(-1L))).isNotEmpty();
            assertThat(validator.validate(buildAddRequest(0L))).isEmpty();
            assertThat(validator.validate(buildAddRequest(null))).isNotEmpty();
        }
    }

    /* 9. 0초 처리: 0을 더해도 정상적으로(변화 없이) 저장/응답된다 */
    @Test
    void addReadingFocusSeconds_zeroSecondsKeepsTotalUnchanged() {
        User student = student(STUDENT_A_ID);
        StudentStats existing = statsOf(student, 300L);
        when(studentStatsRepository.findByStudent_Id(STUDENT_A_ID)).thenReturn(Optional.of(existing));
        when(studentStatsRepository.save(any(StudentStats.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        ReadingFocusResponse response = service.addReadingFocusSeconds(STUDENT_A_ID, 0L);

        assertThat(response.getTotalSeconds()).isEqualTo(300L);
    }

    private ReadingFocusAddRequest buildAddRequest(Long addSeconds) throws Exception {
        ReadingFocusAddRequest request = new ReadingFocusAddRequest();
        java.lang.reflect.Field field = ReadingFocusAddRequest.class.getDeclaredField("addSeconds");
        field.setAccessible(true);
        field.set(request, addSeconds);
        return request;
    }
}
