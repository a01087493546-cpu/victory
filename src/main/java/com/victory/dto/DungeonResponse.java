package com.victory.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class DungeonResponse {

    private Long id;
    private String name;
    private String description;
    private String difficulty;
    private Integer requiredBooks;
    private Integer requiredStatAvg;
    private int bookCount;
    private double statAverage;
    private boolean cleared;
    private boolean eligible;
    private List<String> blockedReasons;
    private int attemptsLeftToday;
    private int rewardValue;
}
