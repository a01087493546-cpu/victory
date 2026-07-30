package com.victory.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class IndividualFinishCandidatesResponse {

    private List<IndividualQaRecordItem> before;
    private List<IndividualQaRecordItem> during;
    private List<IndividualQaRecordItem> after;
}
