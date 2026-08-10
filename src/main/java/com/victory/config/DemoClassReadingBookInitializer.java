package com.victory.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.victory.entity.ClassReadingBook;
import com.victory.entity.SchoolClass;
import com.victory.entity.User;
import com.victory.repository.ClassReadingBookRepository;
import com.victory.repository.SchoolClassRepository;
import com.victory.repository.UserRepository;

import lombok.RequiredArgsConstructor;

/*
 * 심사 교사 계정(tt11)의 학급에 연습읽기(온책읽기) 책을 멱등적으로 등록한다.
 * class_reading_books는 class_id UNIQUE라 학급당 1행만 존재하므로,
 * 이미 행이 있으면 내용을 덮어쓰지 않고 그대로 재사용한다(교사가 화면에서 책을
 * 직접 바꿔도 서버 재시작 때마다 되돌리지 않기 위함).
 */
@Component
@Order(3)
@RequiredArgsConstructor
public class DemoClassReadingBookInitializer implements ApplicationRunner {

    private static final String BOOK_TITLE = "나만의 보물 찾기";
    private static final String BOOK_AUTHOR = "김하늘 글 · 이지후 그림 · 푸른숲 어린이";
    private static final String COVER_IMAGE = "../assets/images/demo/demo_class_book_cover.png";
    private static final int TOTAL_PAGES = 52;

    private final UserRepository userRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final ClassReadingBookRepository classReadingBookRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        User teacher = userRepository.findByLoginId("tt11").orElse(null);
        if (teacher == null || !Boolean.TRUE.equals(teacher.getDemoAccount())) return;

        SchoolClass schoolClass = schoolClassRepository.findByTeacherId(teacher.getId()).orElse(null);
        if (schoolClass == null) return;

        if (classReadingBookRepository.findBySchoolClassId(schoolClass.getId()).isPresent()) return;

        ClassReadingBook book = new ClassReadingBook();
        book.setSchoolClass(schoolClass);
        book.setBookTitle(BOOK_TITLE);
        book.setAuthor(BOOK_AUTHOR);
        book.setCoverImage(COVER_IMAGE);
        book.setTotalPages(TOTAL_PAGES);
        book.setCurrentPage(0);
        classReadingBookRepository.save(book);
    }
}
