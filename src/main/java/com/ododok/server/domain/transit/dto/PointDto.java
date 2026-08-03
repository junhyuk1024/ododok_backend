package com.ododok.server.domain.transit.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PointDto {
    private double longitude; // X
    private double latitude;  // Y
}