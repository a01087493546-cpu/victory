package com.victory.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class IndividualBookChatPostRequest {

    /*
     * 이 글이 어느 개별읽기 진행 중인 책(readingRecord)에 대한 것인지.
     * bookTitle은 화면 표시용 자유 입력일 뿐이라 이 값으로 되짚어 추측하지
     * 않는다 - 서버는 오직 이 readingRecordId만 신뢰하고, 본인 소유·진행
     * 중 상태를 직접 검증한다(IndividualBookChatService 참고).
     */
    private Long readingRecordId;

    private String bookTitle;
    private String title;
    private String scene;
    private String optionA;
    private String optionB;
}
