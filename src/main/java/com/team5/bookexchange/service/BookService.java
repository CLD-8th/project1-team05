package com.team5.bookexchange.service;

import com.team5.bookexchange.entity.Book;
import com.team5.bookexchange.repository.BookRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BookService {

    private final BookRepository bookRepository;

    public BookService(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    // 전체 도서 조회
    public List<Book> findAll() {
        return bookRepository.findAll();
    }

    // 도서 저장
    public Book save(Book book) {
        return bookRepository.save(book);
    }

    // 상세 조회
    public Book findById(Long id) {
        return bookRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("도서를 찾을 수 없습니다."));
    }

    // 도서 상태 변경
    public void updateStatus(Long id, String status) {

        Book book = findById(id);

        book.setStatus(status);

        bookRepository.save(book);
    }

}