package com.ododok.server.domain.recommend.controller;

import com.ododok.server.domain.recommend.dto.AiBookResponseDto;
import com.ododok.server.domain.recommend.dto.BookRecommendRequestDto;
import com.ododok.server.domain.transit.service.TransitService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/books")
@RequiredArgsConstructor
public class BookRecommendController {

    private final TransitService transitService;

    /**
     * 🌟 [API 2] 이동시간 및 프론트에서 선택한 장르를 받아 맞춤 도서 목록을 추천
     * POST /api/v1/books/recommend
     */
    @PostMapping("/recommend")
    public List<AiBookResponseDto> getBookRecommendations(@RequestBody BookRecommendRequestDto requestDto) {
        return transitService.getBookRecommendations(
                requestDto.getDuration(),
                requestDto.getUserCpm(),        // null이면 서비스에서 기본값 950 적용
                requestDto.getPreferredGenres() // 프론트 버튼 입력값
        );
    }
}