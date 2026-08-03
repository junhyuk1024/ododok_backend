package com.ododok.server.domain.book.repository;

import com.ododok.server.domain.book.entity.Book;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BookRepository extends JpaRepository<Book, Long> {

    // ISBN 목록으로 DB에서 책 정보 조회
    List<Book> findByIsbnIn(List<String> isbns);
}