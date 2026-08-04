package com.victory.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.victory.dto.StudentRegisterRequest;
import com.victory.dto.TeacherRegisterRequest;
import com.victory.dto.TeacherRegisterResponse;
import com.victory.entity.ClassStudent;
import com.victory.entity.SchoolClass;
import com.victory.entity.User;
import com.victory.exception.DuplicateLoginIdException;
import com.victory.repository.ClassStudentRepository;
import com.victory.repository.SchoolClassRepository;
import com.victory.repository.UserRepository;

/*
 * /api/teachers/register 학생 일괄 생성 시 500(Internal Server Error)이 발생하던
 * 문제의 원인(checkDuplicateLoginId가 던지는 DuplicateLoginIdException을
 * TeacherRegistrationController가 처리하지 않아 uncaught 예외로 전파됨)을
 * 재현/회귀 방지하기 위한 테스트.
 */
@ExtendWith(MockitoExtension.class)
class TeacherRegistrationServiceTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Mock private UserRepository userRepository;
    @Mock private SchoolClassRepository schoolClassRepository;
    @Mock private ClassStudentRepository classStudentRepository;

    private TeacherRegistrationService service;

    @BeforeEach
    void setUp() {
        service = new TeacherRegistrationService(
                userRepository, schoolClassRepository, classStudentRepository);
    }

    private TeacherRegisterRequest request(List<StudentRegisterRequest> students) throws Exception {
        String json = "{"
                + "\"loginId\":\"teacher01\","
                + "\"password\":\"a1234\","
                + "\"name\":\"김교사\","
                + "\"school\":\"승리초등학교\","
                + "\"className\":\"1반\","
                + "\"grade\":1,"
                + "\"classNumber\":1,"
                + "\"students\": []"
                + "}";
        TeacherRegisterRequest req = MAPPER.readValue(json, TeacherRegisterRequest.class);
        java.lang.reflect.Field field = TeacherRegisterRequest.class.getDeclaredField("students");
        field.setAccessible(true);
        field.set(req, students);
        return req;
    }

    private StudentRegisterRequest student(String loginId, int studentNumber) throws Exception {
        String json = "{"
                + "\"loginId\":\"" + loginId + "\","
                + "\"password\":\"a1234\","
                + "\"name\":\"학생" + studentNumber + "\","
                + "\"studentNumber\":" + studentNumber
                + "}";
        return MAPPER.readValue(json, StudentRegisterRequest.class);
    }

    /* 정상 케이스: 중복이 전혀 없으면 교사·학급·학생 5명이 모두 저장되고 role/암호화가 올바르다 */
    @Test
    void registerTeacherWithClass_success_savesTeacherClassAndAllStudents() throws Exception {
        List<StudentRegisterRequest> students = List.of(
                student("sss01", 1), student("sss02", 2), student("sss03", 3),
                student("sss04", 4), student("sss05", 5));
        TeacherRegisterRequest req = request(students);

        when(userRepository.findByLoginId(anyString())).thenReturn(Optional.empty());

        User savedTeacher = new User();
        savedTeacher.setId(1L);
        savedTeacher.setLoginId("teacher01");
        savedTeacher.setName("김교사");
        savedTeacher.setSchool("승리초등학교");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            if ("teacher".equals(u.getRole())) {
                return savedTeacher;
            }
            User saved = new User();
            saved.setId((long) (10 + u.getLoginId().hashCode() % 1000));
            saved.setLoginId(u.getLoginId());
            saved.setName(u.getName());
            saved.setRole(u.getRole());
            saved.setPassword(u.getPassword());
            return saved;
        });

        SchoolClass savedClass = new SchoolClass();
        savedClass.setId(100L);
        savedClass.setClassName("1반");
        savedClass.setGrade(1);
        savedClass.setClassNumber(1);
        when(schoolClassRepository.save(any(SchoolClass.class))).thenReturn(savedClass);
        when(classStudentRepository.save(any(ClassStudent.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        TeacherRegisterResponse response = service.registerTeacherWithClass(req);

        assertThat(response.getStudents()).hasSize(5);
        verify(classStudentRepository, org.mockito.Mockito.times(5)).save(any(ClassStudent.class));

        // 비밀번호가 평문으로 저장되지 않고 BCrypt로 암호화되었는지 확인
        org.mockito.ArgumentCaptor<User> userCaptor = org.mockito.ArgumentCaptor.forClass(User.class);
        verify(userRepository, org.mockito.Mockito.atLeastOnce()).save(userCaptor.capture());
        for (User saved : userCaptor.getAllValues()) {
            assertThat(saved.getPassword()).isNotEqualTo("a1234");
            assertThat(saved.getPassword()).startsWith("$2");
        }
        assertThat(userCaptor.getAllValues())
                .filteredOn(u -> "student".equals(u.getRole()))
                .hasSize(5);
    }

    /* 교사 아이디가 이미 존재하면 DuplicateLoginIdException(409로 매핑됨)이 발생하고, 아무것도 저장되지 않는다 */
    @Test
    void registerTeacherWithClass_duplicateTeacherLoginId_throwsAndSavesNothing() throws Exception {
        TeacherRegisterRequest req = request(List.of(student("sss01", 1)));
        when(userRepository.findByLoginId("teacher01")).thenReturn(Optional.of(new User()));

        assertThatThrownBy(() -> service.registerTeacherWithClass(req))
                .isInstanceOf(DuplicateLoginIdException.class)
                .hasMessageContaining("교사");

        verify(userRepository, never()).save(any(User.class));
        verify(schoolClassRepository, never()).save(any(SchoolClass.class));
        verify(classStudentRepository, never()).save(any(ClassStudent.class));
    }

    /* 학생 아이디 하나가 이미 DB에 존재하면(이번 버그의 재현 시나리오) 409로 매핑되는
     * DuplicateLoginIdException이 발생하고, 어떤 계정도 저장되지 않는다 */
    @Test
    void registerTeacherWithClass_oneStudentLoginIdAlreadyExists_throwsAndSavesNothing() throws Exception {
        List<StudentRegisterRequest> students = List.of(
                student("sss01", 1), student("sss02", 2), student("s01", 3),
                student("sss04", 4), student("sss05", 5));
        TeacherRegisterRequest req = request(students);

        when(userRepository.findByLoginId("teacher01")).thenReturn(Optional.empty());
        when(userRepository.findByLoginId("sss01")).thenReturn(Optional.empty());
        when(userRepository.findByLoginId("sss02")).thenReturn(Optional.empty());
        when(userRepository.findByLoginId("s01")).thenReturn(Optional.of(new User()));
        when(userRepository.findByLoginId("sss04")).thenReturn(Optional.empty());
        when(userRepository.findByLoginId("sss05")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.registerTeacherWithClass(req))
                .isInstanceOf(DuplicateLoginIdException.class)
                .hasMessageContaining("s01");

        verify(userRepository, never()).save(any(User.class));
        verify(schoolClassRepository, never()).save(any(SchoolClass.class));
        verify(classStudentRepository, never()).save(any(ClassStudent.class));
    }

    /* 학생 아이디가 여러 개 중복이면 메시지에 전부 포함된다 */
    @Test
    void registerTeacherWithClass_multipleStudentLoginIdsAlreadyExist_allListedInMessage() throws Exception {
        List<StudentRegisterRequest> students = List.of(
                student("sss01", 1), student("sss02", 2), student("sss03", 3));
        TeacherRegisterRequest req = request(students);

        when(userRepository.findByLoginId("teacher01")).thenReturn(Optional.empty());
        when(userRepository.findByLoginId("sss01")).thenReturn(Optional.of(new User()));
        when(userRepository.findByLoginId("sss02")).thenReturn(Optional.empty());
        when(userRepository.findByLoginId("sss03")).thenReturn(Optional.of(new User()));

        assertThatThrownBy(() -> service.registerTeacherWithClass(req))
                .isInstanceOf(DuplicateLoginIdException.class)
                .hasMessageContaining("sss01")
                .hasMessageContaining("sss03");
    }

    /* 같은 요청 안에서 학생 아이디가 중복되면(프론트 검증을 우회한 경우) 400으로 매핑되는
     * IllegalArgumentException이 발생하고 DB 저장은 시도되지 않는다 */
    @Test
    void registerTeacherWithClass_duplicateLoginIdWithinRequest_throwsIllegalArgumentException() throws Exception {
        List<StudentRegisterRequest> students = List.of(
                student("sss01", 1), student("sss01", 2));
        TeacherRegisterRequest req = request(students);

        when(userRepository.findByLoginId("teacher01")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.registerTeacherWithClass(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("sss01");

        verify(userRepository, never()).save(any(User.class));
        verify(classStudentRepository, never()).save(any(ClassStudent.class));
    }

    /* 같은 요청 안에서 학생 번호가 중복되면 400으로 매핑되는 IllegalArgumentException이 발생한다 */
    @Test
    void registerTeacherWithClass_duplicateStudentNumberWithinRequest_throwsIllegalArgumentException() throws Exception {
        List<StudentRegisterRequest> students = List.of(
                student("sss01", 1), student("sss02", 1));
        TeacherRegisterRequest req = request(students);

        when(userRepository.findByLoginId("teacher01")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.registerTeacherWithClass(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("1");

        verify(userRepository, never()).save(any(User.class));
        verify(classStudentRepository, never()).save(any(ClassStudent.class));
    }
}
