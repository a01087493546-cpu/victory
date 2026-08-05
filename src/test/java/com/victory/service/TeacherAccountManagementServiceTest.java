package com.victory.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
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
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import com.victory.dto.TeacherManagedAccountsResponse;
import com.victory.dto.TemporaryPasswordResponse;
import com.victory.entity.ClassStudent;
import com.victory.entity.SchoolClass;
import com.victory.entity.User;
import com.victory.repository.ClassStudentRepository;
import com.victory.repository.SchoolClassRepository;
import com.victory.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class TeacherAccountManagementServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private SchoolClassRepository schoolClassRepository;
    @Mock private ClassStudentRepository classStudentRepository;

    private TeacherAccountManagementService service;
    private User teacher;
    private User student;
    private SchoolClass schoolClass;
    private ClassStudent membership;

    @BeforeEach
    void setUp() {
        service = new TeacherAccountManagementService(
                userRepository, schoolClassRepository, classStudentRepository);

        teacher = user(1L, "teacher01", "김교사", "teacher");
        teacher.setSchool("승리초등학교");
        student = user(2L, "s01", "김학생", "student");

        schoolClass = new SchoolClass();
        schoolClass.setId(10L);
        schoolClass.setTeacher(teacher);
        schoolClass.setClassName("생각반");
        schoolClass.setGrade(4);
        schoolClass.setClassNumber(1);

        membership = new ClassStudent();
        membership.setSchoolClass(schoolClass);
        membership.setStudent(student);
        membership.setStudentNumber(3);
    }

    @Test
    void getManagedAccounts_returnsOnlyTeacherClassRosterWithoutPassword() {
        when(userRepository.findById(teacher.getId())).thenReturn(Optional.of(teacher));
        when(schoolClassRepository.findByTeacherId(teacher.getId()))
                .thenReturn(Optional.of(schoolClass));
        when(classStudentRepository.findBySchoolClassId(schoolClass.getId()))
                .thenReturn(List.of(membership));

        TeacherManagedAccountsResponse response =
                service.getManagedAccounts(teacher.getId());

        assertThat(response.getTeacher().getLoginId()).isEqualTo("teacher01");
        assertThat(response.getTeacher().isSelf()).isTrue();
        assertThat(response.getStudents()).hasSize(1);
        assertThat(response.getStudents().get(0).getStudentNumber()).isEqualTo(3);
        assertThat(response.getStudents().get(0).getClassName()).isEqualTo("생각반");
        assertThat(TeacherManagedAccountsResponse.AccountItem.class.getDeclaredFields())
                .noneMatch(field -> field.getName().toLowerCase().contains("password"));
    }

    @Test
    void resetStudentPassword_hashesValueAndReturnsOneTimeTemporaryPassword() {
        when(userRepository.findById(teacher.getId())).thenReturn(Optional.of(teacher));
        when(userRepository.findById(student.getId())).thenReturn(Optional.of(student));
        when(schoolClassRepository.findByTeacherId(teacher.getId()))
                .thenReturn(Optional.of(schoolClass));
        when(classStudentRepository.findByStudentId(student.getId()))
                .thenReturn(Optional.of(membership));

        TemporaryPasswordResponse response =
                service.resetStudentPassword(teacher.getId(), student.getId());

        assertThat(response.getTemporaryPassword())
                .hasSize(10)
                .matches("(?=.*[a-z])(?=.*[0-9])[a-z0-9]+");
        assertThat(student.getPassword()).startsWith("$2");
        assertThat(student.getPassword()).isNotEqualTo(response.getTemporaryPassword());
        assertThat(new BCryptPasswordEncoder().matches(
                response.getTemporaryPassword(), student.getPassword())).isTrue();
        verify(userRepository).save(student);
    }

    @Test
    void resetStudentPassword_rejectsStudentFromAnotherTeachersClass() {
        SchoolClass otherClass = new SchoolClass();
        otherClass.setId(99L);
        membership.setSchoolClass(otherClass);

        when(userRepository.findById(teacher.getId())).thenReturn(Optional.of(teacher));
        when(userRepository.findById(student.getId())).thenReturn(Optional.of(student));
        when(schoolClassRepository.findByTeacherId(teacher.getId()))
                .thenReturn(Optional.of(schoolClass));
        when(classStudentRepository.findByStudentId(student.getId()))
                .thenReturn(Optional.of(membership));

        assertThatThrownBy(() -> service.resetStudentPassword(teacher.getId(), student.getId()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("403");
        verify(userRepository, never()).save(student);
    }

    @Test
    void resetStudentPassword_returnsNotFoundForMissingStudent() {
        when(userRepository.findById(teacher.getId())).thenReturn(Optional.of(teacher));
        when(userRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.resetStudentPassword(teacher.getId(), 404L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404");
    }

    private User user(Long id, String loginId, String name, String role) {
        User user = new User();
        user.setId(id);
        user.setLoginId(loginId);
        user.setName(name);
        user.setRole(role);
        user.setPassword("old-hash");
        user.setCreatedAt(LocalDateTime.of(2026, 8, 5, 10, 0));
        return user;
    }
}
