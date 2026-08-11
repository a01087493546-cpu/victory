package com.victory.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.server.ResponseStatusException;

import com.victory.dto.StudentStatsResponse;
import com.victory.service.StudentStatsService;

class StudentStatsControllerTest {

    private final StudentStatsService studentStatsService = mock(StudentStatsService.class);
    private final StudentStatsController controller = new StudentStatsController(studentStatsService);

    /* 인증이 없으면(비로그인) 403으로 차단된다 */
    @Test
    void getStats_throwsWhenNoAuthentication() {
        assertThatThrownBy(() -> controller.getStats(1L, null))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("403");
    }

    /* JWT의 studentId와 URL의 studentId가 다르면(다른 학생 능력치 조회 시도) 403으로 차단된다 */
    @Test
    void getStats_throwsWhenStudentIdDoesNotMatchAuthentication() {
        Authentication authentication = new UsernamePasswordAuthenticationToken(
            1L, null, List.of(new SimpleGrantedAuthority("student")));

        assertThatThrownBy(() -> controller.getStats(2L, authentication))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("403");
    }

    /* 본인 studentId로 조회하면 StudentStatsService가 돌려주는 값을 그대로 응답한다 */
    @Test
    void getStats_returnsServiceResultForSelf() {
        Long studentId = 1L;
        Authentication authentication = new UsernamePasswordAuthenticationToken(
            studentId, null, List.of(new SimpleGrantedAuthority("student")));

        StudentStatsResponse expected = new StudentStatsResponse(8, 8, 8, 8, false);
        when(studentStatsService.getStats(studentId)).thenReturn(expected);

        StudentStatsResponse result = controller.getStats(studentId, authentication).getBody();

        assertThat(result).isEqualTo(expected);
        verify(studentStatsService).getStats(eq(studentId));
    }

    /* 학생 간 능력치 격리: 학생 A의 JWT로는 학생 B의 studentId를 조회할 수 없다 */
    @Test
    void getStats_doesNotAllowCrossStudentAccess() {
        Authentication studentAAuth = new UsernamePasswordAuthenticationToken(
            10L, null, List.of(new SimpleGrantedAuthority("student")));

        assertThatThrownBy(() -> controller.getStats(20L, studentAAuth))
            .isInstanceOf(ResponseStatusException.class);
    }
}
