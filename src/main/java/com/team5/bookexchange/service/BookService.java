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

    public void requestExchange(Long bookId) {

        Book book = bookRepository.findById(bookId)
                .orElseThrow(() ->
                        new RuntimeException("도서를 찾을 수 없습니다.")
                        );

        book.setStatus("REQUESTED");

        bookRepository.save(book);

        // Book 정보가 변경 됐으므로 캐시 삭제
        redisService.deleteBookCache(bookId);
    }

    // 작성자와 상태값이 REQUESTED인 건에 대한 조회 메서드 추가
    public List<Book> findRequestedBooksByOwner(Long ownerId) {

        return bookRepository.findByOwnerIdAndStatus(ownerId, "REQUESTED");
    }

    // 교환 신청 수락 메서드
    public void acceptExchange(Long bookId, Long ownerId) {

        Book book = bookRepository.findById(bookId)
                .orElseThrow(() ->
                        new RuntimeException("도서를 찾을 수 없습니다.")
                );

        // 현재 로그인 사용자가 실제 책 주인인지 확인
        if (!book.getOwnerId().equals(ownerId)) {
            throw new RuntimeException("교환 요청을 수락할 권한이 없습니다.");
        }

        // REQUESTED 상태에서만 수락 가능
        if (!"REQUESTED".equals(book.getStatus())) {
            throw new RuntimeException("교환 요청 상태인 도서만 수락할 수 있습니다.");
        }

        book.setStatus("EXCHANGED");

        bookRepository.save(book);

        // 기존 Book 캐시 무효화
        redisService.deleteBookCache(bookId);
    }

    public void rejectExchange(Long bookId, Long ownerId) {

        Book book = bookRepository.findById(bookId)
                .orElseThrow(() ->
                            new RuntimeException("도서를 찾을 수 없습니다.")
                        );

        // 책 주인만 거절 가능
        if (!book.getOwnerId().equals(ownerId)) {
            throw new RuntimeException("교환 요청을 거절할 권한이 없습니다.");
        }

        // 교환 요청 중인 책만 거절 가능
        if (!"REQUESTED".equals(book.getStatus())) {
            throw new RuntimeException("교환 요청 상태인 도서만 거절할 수 있습니다.");
        }

        // 다시 교환 가능한 상태로 변경
        book.setStatus("REJECTED");

        bookRepository.save(book);

        // Redis에 이전 REQUESTED 상태가 남지 않도록 삭제
        redisService.deleteBookCache(bookId);
    }
}