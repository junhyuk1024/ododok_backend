package com.ododok.server.domain.book.service;

import com.ododok.server.domain.book.dto.BookRecommendResponseDto;
import com.ododok.server.domain.book.dto.ModelResultDto;
import com.ododok.server.domain.book.entity.Book;
import com.ododok.server.domain.book.repository.BookRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookService {

    private final BookRepository bookRepository;

    /**
     * AI 모델 결과(ISBN, Score, TotalPage)를 받아 DB의 메타데이터와 조인
     */
    public List<BookRecommendResponseDto> getRecommendedBooksByModel(List<ModelResultDto> modelResults) {
        List<String> isbns = modelResults.stream()
                .map(ModelResultDto::getIsbn)
                .collect(Collectors.toList());

        List<Book> books = bookRepository.findByIsbnIn(isbns);

        Map<String, Book> bookMap = books.stream()
                .collect(Collectors.toMap(Book::getIsbn, book -> book, (b1, b2) -> b1));

        return modelResults.stream()
                .map(result -> {
                    Book book = bookMap.get(result.getIsbn());
                    if (book == null) return null;
                    return new BookRecommendResponseDto(book, result.getScore(), result.getTotalPage());
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    // AI 모델 연동 전 백엔드 단독 테스트용 (Mock 데이터 반환)
    public List<BookRecommendResponseDto> getMockRecommendedBooks(int duration, double wpm) {
        List<Book> allBooks = bookRepository.findAll();
        if (allBooks.isEmpty()) return List.of();

        int targetPage = (int) (duration * wpm);

        // 상위 3권만 가상 매핑
        return allBooks.stream().limit(3).map(book ->
                new BookRecommendResponseDto(book, 0.92, targetPage)
        ).collect(Collectors.toList());
    }
}