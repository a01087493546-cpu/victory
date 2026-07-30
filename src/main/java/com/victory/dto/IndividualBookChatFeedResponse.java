package com.victory.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;

/*
 * 책수다방 글 목록 응답. hasClass가 false면 이 학생이 어떤 학급에도
 * 속해 있지 않다는 뜻이다(테스트 계정 등) - 이 경우 posts는 본인이 쓴
 * 글만 담고, 프론트는 "학급 정보가 없다"는 안내를 별도로 보여줘야 한다.
 */
@Getter
@AllArgsConstructor
public class IndividualBookChatFeedResponse {

    private boolean hasClass;
    private List<IndividualBookChatPostResponse> posts;
}
