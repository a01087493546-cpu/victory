package com.victory.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.victory.dto.ClassReadingBookRequest;
import com.victory.dto.ClassReadingBookResponse;
import com.victory.service.ClassReadingBookService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/classes")
@RequiredArgsConstructor
public class ClassReadingBookController {

    private final ClassReadingBookService classReadingBookService;

    @GetMapping("/{classId}/reading-range")
    public ResponseEntity<ClassReadingBookResponse> getReadingRange(
            @PathVariable("classId") Long classId
    ) {
        ClassReadingBookResponse response =
                classReadingBookService.getReadingBook(classId);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{classId}/reading-range")
    public ResponseEntity<ClassReadingBookResponse> saveReadingRange(
            @PathVariable("classId") Long classId,
            @Valid @RequestBody ClassReadingBookRequest request
    ) {
        ClassReadingBookResponse response =
                classReadingBookService.saveReadingBook(classId, request);

        return ResponseEntity.ok(response);
    }
}
