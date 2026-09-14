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

import com.victory.dto.ReadingFocusAddRequest;
import com.victory.dto.ReadingFocusResponse;
import com.victory.service.StudentStatsService;

class ReadingFocusControllerTest {

    private final StudentStatsService studentStatsService = mock(StudentStatsService.class);
    private final ReadingFocusController controller = new ReadingFocusController(studentStatsService);

    /* 인증이 없으면(비로그인) 403으로 차단된다 - 기존 StudentStatsController와 같은 정책 */
    @Test
    void getReadingFocus_throwsWhenNoAuthentication() {
        assertThatThrownBy(() -> controller.getReadingFocus(1L, null))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("403");
    }

    /* JWT의 studentId와 URL의 studentId가 다르면(다른 학생 값 조회 시도) 403으로 차단된다 */
    @Test
    void getReadingFocus_throwsWhenStudentIdDoesNotMatchAuthentication() {
        Authentication authentication = new UsernamePasswordAuthenticationToken(
            1L, null, List.of(new SimpleGrantedAuthority("student")));

        assertThatThrownBy(() -> controller.getReadingFocus(2L, authentication))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("403");
    }

    /* 본인 studentId로 조회하면 StudentStatsService가 돌려주는 값을 그대로 응답한다 */
    @Test
    void getReadingFocus_returnsServiceResultForSelf() {
        Long studentId = 1L;
        Authentication authentication = new UsernamePasswordAuthenticationToken(
            studentId, null, List.of(new SimpleGrantedAuthority("student")));

        ReadingFocusResponse expected = ReadingFocusResponse.of(900L);
        when(studentStatsService.getReadingFocus(studentId)).thenReturn(expected);

        ReadingFocusResponse result = controller.getReadingFocus(studentId, authentication).getBody();

        assertThat(result).isEqualTo(expected);
        verify(studentStatsService).getReadingFocus(eq(studentId));
    }

    /* 다른 학생 studentId로는 추가 요청도 403으로 차단된다(자기 값만 저장 가능) */
    @Test
    void addReadingFocus_throwsWhenStudentIdDoesNotMatchAuthentication() {
        Authentication authentication = new UsernamePasswordAuthenticationToken(
            1L, null, List.of(new SimpleGrantedAuthority("student")));
        ReadingFocusAddRequest request = new ReadingFocusAddRequest();

        assertThatThrownBy(() -> controller.addReadingFocus(2L, request, authentication))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("403");
    }

    /* 본인 studentId로 추가하면 서비스 결과(최신 누적값)를 그대로 응답한다 */
    @Test
    void addReadingFocus_returnsUpdatedTotalForSelf() throws Exception {
        Long studentId = 1L;
        Authentication authentication = new UsernamePasswordAuthenticationToken(
            studentId, null, List.of(new SimpleGrantedAuthority("student")));

        ReadingFocusAddRequest request = new ReadingFocusAddRequest();
        java.lang.reflect.Field field = ReadingFocusAddRequest.class.getDeclaredField("addSeconds");
        field.setAccessible(true);
        field.set(request, 600L);

        when(studentStatsService.addReadingFocusSeconds(studentId, 600L))
            .thenReturn(ReadingFocusResponse.of(600L));

        ReadingFocusResponse result = controller.addReadingFocus(studentId, request, authentication).getBody();

        assertThat(result.getTotalSeconds()).isEqualTo(600L);
        verify(studentStatsService).addReadingFocusSeconds(eq(studentId), eq(600L));
    }
}
