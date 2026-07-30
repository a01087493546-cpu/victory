package com.victory.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;

/*
 * 개별읽기 책 등록 요청(POST /api/students/me/individual-reading/books).
 * 학생 ID는 이 DTO에 없다 - 컨트롤러가 JWT(Authentication)에서만 가져온다.
 */
@Getter
@NoArgsConstructor
public class IndividualBookRegisterRequest {

    @NotBlank
    private String title;

    private String author;

    private String bookType;

    /*
     * individual-before-reading.html에는 아직 표지 입력 UI가 없어 항상
     * null로 온다. Book 엔티티 컬럼을 최대한 재사용하기 위해 필드만 미리
     * 열어 둔다(추후 화면이 생기면 그대로 연결 가능).
     */
    private String coverImage;

    /*
     * 전체 쪽수는 읽기 중 화면에서 나중에 입력할 수 있으므로 선택값이다.
     */
    @Positive
    private Integer totalPages;
}
