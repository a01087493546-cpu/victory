package com.victory.config;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.core.annotation.Order;

import com.victory.entity.ClassStudent;
import com.victory.entity.SchoolClass;
import com.victory.entity.StudentStats;
import com.victory.entity.User;
import com.victory.repository.ClassStudentRepository;
import com.victory.repository.SchoolClassRepository;
import com.victory.repository.StudentStatsRepository;
import com.victory.repository.UserRepository;

import lombok.RequiredArgsConstructor;

/** 심사용 계정과 가상 학급을 기존 행을 덮어쓰지 않고 멱등적으로 만든다. */
@Component
@Order(1)
@RequiredArgsConstructor
public class DemoDataInitializer implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(DemoDataInitializer.class);
    private static final String DEMO_CLASS_NAME = "생각이자라는우리반";
    private static final List<DemoStudent> STUDENTS = List.of(
        new DemoStudent("ss01", "ss01", "김초롱"),
        new DemoStudent("demo_student_02", "demo-only-02", "송민정"),
        new DemoStudent("demo_student_03", "demo-only-03", "박하민"),
        new DemoStudent("demo_student_04", "demo-only-04", "이진우"),
        new DemoStudent("demo_student_05", "demo-only-05", "김민지"),
        new DemoStudent("demo_student_06", "demo-only-06", "서희원"),
        new DemoStudent("demo_student_07", "demo-only-07", "김수진"),
        new DemoStudent("demo_student_08", "demo-only-08", "이혜원")
    );

    private final UserRepository userRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final ClassStudentRepository classStudentRepository;
    private final StudentStatsRepository studentStatsRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        User teacher = findOrCreateDemoUser("tt11", "tt11", "김문답", "teacher");
        if (teacher == null) return;

        SchoolClass schoolClass = schoolClassRepository.findByTeacherId(teacher.getId()).orElse(null);
        if (schoolClass == null) {
            schoolClass = new SchoolClass();
            schoolClass.setTeacher(teacher);
            schoolClass.setClassName(DEMO_CLASS_NAME);
            schoolClass.setGrade(4);
            schoolClass.setClassNumber(1);
            schoolClass = schoolClassRepository.save(schoolClass);
        } else if (!DEMO_CLASS_NAME.equals(schoolClass.getClassName())) {
            schoolClass.setClassName(DEMO_CLASS_NAME);
            schoolClass = schoolClassRepository.save(schoolClass);
        }

        for (int index = 0; index < STUDENTS.size(); index++) {
            DemoStudent source = STUDENTS.get(index);
            User student = findOrCreateDemoUser(
                source.loginId(), source.password(), source.name(), "student");
            if (student == null) continue;

            if (classStudentRepository.findByStudentId(student.getId()).isEmpty()) {
                ClassStudent membership = new ClassStudent();
                membership.setSchoolClass(schoolClass);
                membership.setStudent(student);
                membership.setStudentNumber(index + 1);
                classStudentRepository.save(membership);
            }
            ensureFixedStats(student, index);
        }
    }

    private User findOrCreateDemoUser(String loginId, String password, String name, String role) {
        User existing = userRepository.findByLoginId(loginId).orElse(null);
        if (existing != null) {
            boolean matchesExpectedIdentity = name.equals(existing.getName())
                && role.equalsIgnoreCase(existing.getRole())
                && passwordEncoder.matches(password, existing.getPassword());
            if (!matchesExpectedIdentity) {
                log.warn("심사 계정 아이디 {}가 다른 계정에 사용 중이어서 변경하지 않습니다.", loginId);
                return null;
            }
            if (!Boolean.TRUE.equals(existing.getDemoAccount())) {
                existing.setDemoAccount(true);
                return userRepository.save(existing);
            }
            return existing;
        }

        User created = new User();
        created.setLoginId(loginId);
        created.setPassword(passwordEncoder.encode(password));
        created.setName(name);
        created.setRole(role);
        created.setDemoAccount(true);
        if ("teacher".equals(role)) created.setSchool("문답초등학교");
        return userRepository.save(created);
    }

    private void ensureFixedStats(User student, int index) {
        if (studentStatsRepository.findByStudent_Id(student.getId()).isPresent()) return;
        int[][] values = {
            {72, 74, 76, 70}, {78, 77, 82, 74}, {61, 65, 64, 60}, {48, 55, 52, 46},
            {35, 42, 39, 34}, {69, 72, 75, 68}, {66, 63, 62, 73}, {58, 64, 57, 55}
        };
        StudentStats stats = new StudentStats();
        stats.setStudent(student);
        stats.setMagic(values[index][0]);
        stats.setStamina(values[index][1]);
        stats.setWisdom(values[index][2]);
        stats.setCourage(values[index][3]);
        studentStatsRepository.save(stats);
    }

    private record DemoStudent(String loginId, String password, String name) {}
}
