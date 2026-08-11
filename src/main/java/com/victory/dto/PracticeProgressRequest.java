package com.victory.dto;

import java.util.Map;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PracticeProgressRequest {

    private Boolean bookSelected;
    private Boolean beforeDone;
    private Boolean classReadDone;
    private Boolean afterDone;

    private Map<String, Boolean> duringTypeProgress;
    private Map<String, Boolean> afterTypeProgress;
}