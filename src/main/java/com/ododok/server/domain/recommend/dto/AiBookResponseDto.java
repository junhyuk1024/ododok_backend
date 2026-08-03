package com.ododok.server.domain.recommend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class AiBookResponseDto {

    private String isbn;
    private String title; // 🌟 파이썬에서 들어온 title 추가

    @JsonProperty("estimated_time_minutes")
    private double estimatedTimeMinutes;
    private int score;
}