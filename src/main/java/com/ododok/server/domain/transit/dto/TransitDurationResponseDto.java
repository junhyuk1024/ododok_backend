package com.ododok.server.domain.transit.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class TransitDurationResponseDto {
    private String startStation;
    private String endStation;
    private int duration; // 지하철 소요시간 (분)
}