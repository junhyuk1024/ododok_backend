package com.ododok.server.domain.book.controller;

import com.ododok.server.domain.book.dto.BookRecommendResponseDto;
import com.ododok.server.domain.book.service.BookService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/books")
@RequiredArgsConstructor
public class BookController {

    private final BookService bookService;

    @GetMapping("/recommend")
    public List<BookRecommendResponseDto> getRecommendedBooks(
            @RequestParam(name = "duration", defaultValue = "42") int duration,
            @RequestParam(name = "wpm", defaultValue = "1.2") double wpm) {

        // AI 연동 전까지 Mock 데이터로 응답 테스트
        return bookService.getMockRecommendedBooks(duration, wpm);
    }
}