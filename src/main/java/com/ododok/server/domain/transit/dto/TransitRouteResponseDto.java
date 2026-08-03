package com.ododok.server.domain.transit.dto;

import com.ododok.server.domain.recommend.dto.AiBookResponseDto;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class TransitRouteResponseDto {
    private String startStation;
    private String endStation;
    private int duration;

    // 🌟 파이썬 AI 추천 도서 리스트 추가
    private List<AiBookResponseDto> recommendedBooks;
}