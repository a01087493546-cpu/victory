package com.victory.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.victory.exception.DuplicateLoginIdException;
import com.victory.service.TeacherRegistrationService;

/*
 * /api/teachers/register 학생 일괄 생성 500(Internal Server Error) 버그의
 * 회귀 방지 테스트. checkDuplicateLoginId가 던지는 예외를 이 컨트롤러가
 * 잡아 409/400으로 변환하는지 확인한다(수정 전에는 핸들러가 없어 uncaught
 * 예외로 전파되어 Spring 기본 500 응답이 나갔다).
 */
class TeacherRegistrationControllerTest {

    private final TeacherRegistrationService teacherRegistrationService =
            mock(TeacherRegistrationService.class);
    private final TeacherRegistrationController controller =
            new TeacherRegistrationController(teacherRegistrationService);

    @Test
    void handleDuplicateLoginId_returns409WithMessage() {
        DuplicateLoginIdException ex =
                new DuplicateLoginIdException("이미 사용 중인 학생 아이디가 있습니다: sss01, sss02");

        ResponseEntity<java.util.Map<String, String>> response =
                controller.handleDuplicateLoginId(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).containsEntry(
                "message", "이미 사용 중인 학생 아이디가 있습니다: sss01, sss02");
    }

    @Test
    void handleInvalidRequest_returns400WithMessage() {
        IllegalArgumentException ex =
                new IllegalArgumentException("요청에 중복된 학생 아이디가 있습니다: sss01");

        ResponseEntity<java.util.Map<String, String>> response =
                controller.handleInvalidRequest(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry(
                "message", "요청에 중복된 학생 아이디가 있습니다: sss01");
    }
}
