package com.ododok.server.domain.transit.controller;

import com.ododok.server.domain.transit.dto.TransitDurationResponseDto;
import com.ododok.server.domain.transit.service.TransitService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/transit")
@RequiredArgsConstructor
public class TransitController {

    private final TransitService transitService;

    /**
     * 🌟 [API 1] 출발역과 도착역 이름으로 지하철 소요 시간(분)만 조회
     * GET /api/v1/transit/duration?start=강남역&end=안양역
     */
    @GetMapping("/duration")
    public TransitDurationResponseDto getTransitDuration(
            @RequestParam String start,
            @RequestParam String end) {

        int duration = transitService.getTransitDurationMinutesByName(start, end);
        return new TransitDurationResponseDto(start, end, duration);
    }
}