// 프론트엔드 응답용 DTO (BookRecommendResponseDto.java)
package com.ododok.server.domain.book.dto;

import com.ododok.server.domain.book.entity.Book;
import lombok.Getter;

@Getter
public class BookRecommendResponseDto {
    private final Long id;
    private final String title;
    private final String author;
    private final String isbn;
    private final Integer totalPage;
    private final Integer matchPercentage;

    public BookRecommendResponseDto(Book book, Double score, Integer totalPage) {
        this.id = book.getId();
        this.title = book.getTitle();
        this.author = book.getAuthor();
        this.isbn = book.getIsbn();
        this.totalPage = totalPage;
        this.matchPercentage = score != null ? (int) Math.round(score * 100) : null;
    }
}