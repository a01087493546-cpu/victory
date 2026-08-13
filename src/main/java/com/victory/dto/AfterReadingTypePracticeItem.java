package com.victory.dto;

import com.victory.entity.Response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AfterReadingTypePracticeItem {

    private String bookType;
    private String question1;
    private String answer1;
    private String question2;
    private String answer2;

    public static AfterReadingTypePracticeItem from(Response response) {
        var extraData = response.getExtraData();

        return new AfterReadingTypePracticeItem(
            extraData == null || extraData.get("bookType") == null
                ? null
                : extraData.get("bookType").toString(),
            extraData == null || extraData.get("question1") == null
                ? null
                : extraData.get("question1").toString(),
            extraData == null || extraData.get("answer1") == null
                ? null
                : extraData.get("answer1").toString(),
            extraData == null || extraData.get("question2") == null
                ? null
                : extraData.get("question2").toString(),
            extraData == null || extraData.get("answer2") == null
                ? null
                : extraData.get("answer2").toString()
        );
    }
}
