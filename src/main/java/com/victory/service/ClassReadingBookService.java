package com.victory.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.victory.dto.ClassReadingBookRequest;
import com.victory.dto.ClassReadingBookResponse;
import com.victory.entity.ClassReadingBook;
import com.victory.entity.SchoolClass;
import com.victory.repository.ClassReadingBookRepository;
import com.victory.repository.SchoolClassRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClassReadingBookService {

    private final ClassReadingBookRepository classReadingBookRepository;
    private final SchoolClassRepository schoolClassRepository;

    /*
     * 학급의 온책읽기 책 정보를 조회한다.
     */
    public ClassReadingBookResponse getReadingBook(Long classId) {

        ClassReadingBook book = classReadingBookRepository
            .findBySchoolClassId(classId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "등록된 온책읽기 책 정보가 없습니다. classId=" + classId
            ));

        return ClassReadingBookResponse.from(book);
    }

    /*
     * 학급의 온책읽기 책 정보를 저장한다.
     * 이미 저장된 행이 있으면 새로 추가하지 않고 그 행을 덮어쓴다(학급당 1행 유지).
     */
    @Transactional
    public ClassReadingBookResponse saveReadingBook(
            Long classId,
            ClassReadingBookRequest request) {

        SchoolClass schoolClass = findSchoolClass(classId);

        ClassReadingBook book = classReadingBookRepository
            .findBySchoolClassId(classId)
            .orElseGet(() -> createReadingBook(schoolClass));

        book.setBookTitle(request.getBookTitle());
        book.setAuthor(request.getAuthor());
        book.setCoverImage(request.getCoverImage());
        book.setTotalPages(request.getTotalPages());
        book.setCurrentPage(request.getCurrentPage());

        ClassReadingBook savedBook =
            classReadingBookRepository.save(book);

        return ClassReadingBookResponse.from(savedBook);
    }

    private SchoolClass findSchoolClass(Long classId) {

        return schoolClassRepository.findById(classId)
            .orElseThrow(() -> new EntityNotFoundException(
                "학급을 찾을 수 없습니다. classId=" + classId
            ));
    }

    private ClassReadingBook createReadingBook(SchoolClass schoolClass) {

        ClassReadingBook book = new ClassReadingBook();
        book.setSchoolClass(schoolClass);

        return book;
    }
}
