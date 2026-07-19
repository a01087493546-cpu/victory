package com.victory.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.victory.dto.ClassReadingBookRequest;
import com.victory.dto.ClassReadingBookResponse;
import com.victory.dto.ClassReadingProgressRequest;
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

    /*
     * 학급의 읽기 진행(전체 쪽수/누적 읽은 쪽수)만 수정한다.
     * 책 제목·지은이·표지는 그대로 유지하고 건드리지 않는다.
     * 아직 등록된 책이 없는 학급이면 404를 반환한다(진행 수정은 등록 이후에만 의미가 있다).
     */
    @Transactional
    public ClassReadingBookResponse updateReadingProgress(
            Long classId,
            ClassReadingProgressRequest request) {

        ClassReadingBook book = classReadingBookRepository
            .findBySchoolClassId(classId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "등록된 온책읽기 책 정보가 없습니다. classId=" + classId
            ));

        if (request.getCurrentPage() > request.getTotalPages()) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "누적 읽은 쪽수는 전체 쪽수를 넘을 수 없습니다."
            );
        }

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
