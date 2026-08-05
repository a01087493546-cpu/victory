package com.victory.service;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.victory.dto.TeacherManagedAccountsResponse;
import com.victory.dto.TeacherManagedAccountsResponse.AccountItem;
import com.victory.dto.TemporaryPasswordResponse;
import com.victory.entity.ClassStudent;
import com.victory.entity.SchoolClass;
import com.victory.entity.User;
import com.victory.repository.ClassStudentRepository;
import com.victory.repository.SchoolClassRepository;
import com.victory.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TeacherAccountManagementService {

    private static final String LETTERS = "abcdefghjkmnpqrstuvwxyz";
    private static final String DIGITS = "23456789";
    private static final String PASSWORD_CHARS = LETTERS + DIGITS;
    private static final int TEMPORARY_PASSWORD_LENGTH = 10;

    private final UserRepository userRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final ClassStudentRepository classStudentRepository;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional(readOnly = true)
    public TeacherManagedAccountsResponse getManagedAccounts(Long teacherId) {
        User teacher = requireTeacher(teacherId);
        SchoolClass schoolClass = schoolClassRepository.findByTeacherId(teacherId).orElse(null);

        AccountItem teacherItem = toAccountItem(teacher, schoolClass, null, true);
        List<AccountItem> students = new ArrayList<>();

        if (schoolClass != null) {
            List<ClassStudent> roster = new ArrayList<>(
                    classStudentRepository.findBySchoolClassId(schoolClass.getId()));
            roster.sort(Comparator.comparing(
                    ClassStudent::getStudentNumber,
                    Comparator.nullsLast(Integer::compareTo)));

            for (ClassStudent membership : roster) {
                students.add(toAccountItem(
                        membership.getStudent(), schoolClass,
                        membership.getStudentNumber(), false));
            }
        }

        return new TeacherManagedAccountsResponse(teacherItem, students);
    }

    @Transactional
    public TemporaryPasswordResponse resetStudentPassword(Long teacherId, Long studentId) {
        requireTeacher(teacherId);

        User student = userRepository.findById(studentId)
                .filter(user -> "student".equals(user.getRole()))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "학생 계정을 찾을 수 없습니다."));

        SchoolClass teacherClass = schoolClassRepository.findByTeacherId(teacherId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN, "관리할 수 있는 학급이 없습니다."));

        ClassStudent membership = classStudentRepository.findByStudentId(studentId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN, "담당 학급의 학생 계정만 관리할 수 있습니다."));

        if (!teacherClass.getId().equals(membership.getSchoolClass().getId())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "담당 학급의 학생 계정만 관리할 수 있습니다.");
        }

        String temporaryPassword = generateTemporaryPassword();
        student.setPassword(passwordEncoder.encode(temporaryPassword));
        userRepository.save(student);

        return new TemporaryPasswordResponse(
                student.getId(), student.getLoginId(), student.getName(), temporaryPassword);
    }

    private User requireTeacher(Long teacherId) {
        return userRepository.findById(teacherId)
                .filter(user -> "teacher".equals(user.getRole()))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN, "교사 계정만 계정을 관리할 수 있습니다."));
    }

    private AccountItem toAccountItem(
            User user, SchoolClass schoolClass, Integer studentNumber, boolean self) {
        return new AccountItem(
                user.getId(),
                user.getName(),
                user.getLoginId(),
                user.getRole(),
                user.getSchool(),
                schoolClass == null ? null : schoolClass.getClassName(),
                schoolClass == null ? null : schoolClass.getGrade(),
                schoolClass == null ? null : schoolClass.getClassNumber(),
                studentNumber,
                user.getCreatedAt(),
                self);
    }

    private String generateTemporaryPassword() {
        char[] password = new char[TEMPORARY_PASSWORD_LENGTH];
        password[0] = randomChar(LETTERS);
        password[1] = randomChar(DIGITS);

        for (int index = 2; index < password.length; index++) {
            password[index] = randomChar(PASSWORD_CHARS);
        }

        for (int index = password.length - 1; index > 0; index--) {
            int swapIndex = secureRandom.nextInt(index + 1);
            char value = password[index];
            password[index] = password[swapIndex];
            password[swapIndex] = value;
        }

        return new String(password);
    }

    private char randomChar(String characters) {
        return characters.charAt(secureRandom.nextInt(characters.length()));
    }
}
