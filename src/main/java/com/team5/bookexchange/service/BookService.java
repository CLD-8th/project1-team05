package com.team5.bookexchange.service;

import com.team5.bookexchange.dto.BookCache;
import com.team5.bookexchange.entity.Book;
import com.team5.bookexchange.repository.BookRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BookService {

    private final BookRepository bookRepository;
    private final RedisService redisService;

    public BookService(
            BookRepository bookRepository,
            RedisService redisService) {

        this.bookRepository = bookRepository;
        this.redisService = redisService;
    }

    // 전체 도서 조회
    public List<Book> findAll() {
        return bookRepository.findAll();
    }

    // 도서 저장
    public Book save(Book book) {
        return bookRepository.save(book);
    }

    // 상세 조회 - Redis Cache-Aside
    public Book findById(Long id) {

        // 1. Redis에서 먼저 조회
        BookCache cachedBook = redisService.getBookCache(id);

        // 2. Redis에 있으면 HIT
        if (cachedBook != null) {
            System.out.println("[Redis HIT] book:" + id);

            return cachedBook.toEntity();
        }

        // 3. Redis에 없으면 MISS → MySQL 조회
        System.out.println("[Redis MISS] book:" + id);

        Book book = bookRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("도서를 찾을 수 없습니다.")
                );

        // 4. MySQL에서 가져온 데이터를 Redis에 저장
        redisService.saveBookCache(new BookCache(book));

        return book;
    }

    // 도서 상태 변경
    public void updateStatus(Long id, String status) {

        Book book = bookRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("도서를 찾을 수 없습니다.")
                );

        book.setStatus(status);
        bookRepository.save(book);

        // DB 내용이 변경됐으므로 기존 Redis 캐시 삭제
        redisService.deleteBookCache(id);
    }
}