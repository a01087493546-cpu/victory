package com.victory.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.victory.dto.StudentRegisterRequest;
import com.victory.dto.TeacherClassResponse;
import com.victory.dto.TeacherRegisterRequest;
import com.victory.dto.TeacherRegisterResponse;
import com.victory.entity.ClassStudent;
import com.victory.entity.SchoolClass;
import com.victory.entity.User;
import com.victory.exception.DuplicateLoginIdException;
import com.victory.repository.ClassStudentRepository;
import com.victory.repository.SchoolClassRepository;
import com.victory.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TeacherRegistrationService {

    private final UserRepository userRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final ClassStudentRepository classStudentRepository;

    private final BCryptPasswordEncoder passwordEncoder =
            new BCryptPasswordEncoder();

    @Transactional
    public TeacherRegisterResponse registerTeacherWithClass(
            TeacherRegisterRequest request
    ) {
        checkDuplicateLoginId(request);

        // 1. 교사 계정 저장
        User teacher = new User();
        teacher.setLoginId(request.getLoginId());
        teacher.setPassword(passwordEncoder.encode(request.getPassword()));
        teacher.setName(request.getName());
        teacher.setRole("teacher");
        teacher.setSchool(request.getSchool());

        User savedTeacher = userRepository.save(teacher);

        // 2. 학급 저장
        SchoolClass schoolClass = new SchoolClass();
        schoolClass.setTeacher(savedTeacher);
        schoolClass.setClassName(request.getClassName());
        schoolClass.setGrade(request.getGrade());
        schoolClass.setClassNumber(request.getClassNumber());

        SchoolClass savedClass = schoolClassRepository.save(schoolClass);

        // 3. 학생 계정과 학급 소속 저장
        List<TeacherRegisterResponse.StudentResponse> studentResponses =
                new ArrayList<>();

        for (StudentRegisterRequest studentRequest : request.getStudents()) {

            User student = new User();
            student.setLoginId(studentRequest.getLoginId());
            student.setPassword(
                    passwordEncoder.encode(studentRequest.getPassword())
            );
            student.setName(studentRequest.getName());
            student.setRole("student");

            User savedStudent = userRepository.save(student);

            ClassStudent classStudent = new ClassStudent();
            classStudent.setSchoolClass(savedClass);
            classStudent.setStudent(savedStudent);
            classStudent.setStudentNumber(
                    studentRequest.getStudentNumber()
            );

            classStudentRepository.save(classStudent);

            studentResponses.add(
                    new TeacherRegisterResponse.StudentResponse(
                            savedStudent.getId(),
                            savedStudent.getLoginId(),
                            savedStudent.getName(),
                            studentRequest.getStudentNumber()
                    )
            );
        }

        // 4. 등록 결과 반환
        return new TeacherRegisterResponse(
                savedTeacher.getId(),
                savedClass.getId(),
                savedTeacher.getName(),
                savedTeacher.getSchool(),
                savedClass.getClassName(),
                savedClass.getGrade(),
                savedClass.getClassNumber(),
                studentResponses
        );
    }
@Transactional(readOnly = true)
public TeacherClassResponse getTeacherClass(Long teacherId) {

    SchoolClass schoolClass = schoolClassRepository
            .findByTeacherId(teacherId)
            .orElseThrow(() ->
                    new IllegalArgumentException(
                            "해당 교사의 학급 정보를 찾을 수 없습니다."
                    )
            );

    User teacher = schoolClass.getTeacher();

    return new TeacherClassResponse(
            teacher.getId(),
            teacher.getName(),
            teacher.getSchool(),
            schoolClass.getId(),
            schoolClass.getClassName(),
            schoolClass.getGrade(),
            schoolClass.getClassNumber()
    );
}
    private void checkDuplicateLoginId(
            TeacherRegisterRequest request
    ) {
        if (userRepository.findByLoginId(request.getLoginId()).isPresent()) {
            throw new DuplicateLoginIdException(
                    "이미 사용 중인 교사 아이디입니다."
            );
        }

        for (StudentRegisterRequest student : request.getStudents()) {
            if (userRepository.findByLoginId(student.getLoginId()).isPresent()) {
                throw new DuplicateLoginIdException(
                        "이미 사용 중인 학생 아이디가 있습니다: "
                                + student.getLoginId()
                );
            }
        }
    }
}