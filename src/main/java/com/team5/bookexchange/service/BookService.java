package com.team5.bookexchange.service;

import com.team5.bookexchange.entity.Book;
import com.team5.bookexchange.dto.BookListItem;
import com.team5.bookexchange.dto.BookListBlockCache;
import org.springframework.data.domain.PageImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.team5.bookexchange.repository.BookRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BookService {

    private static final Logger log = LoggerFactory.getLogger(BookService.class);
    private static final int PAGE_SIZE = 10;
    private static final int BLOCK_SIZE = 30;
    private final RedisService redisService;
    private final BookRepository bookRepository;

    public BookService(BookRepository bookRepository, RedisService redisService) {
        this.redisService = redisService;
        this.bookRepository = bookRepository;
    }

    public List<Book> findAll() {
        return bookRepository.findAll();
    }

    // 도서 저장
    public Book save(Book book) {
        Book saved = bookRepository.save(book);
        redisService.invalidateBookListCache();
        return saved;
    }

    // 도서 상세 조회
    // 상세 정보는 Redis 캐시를 사용하지 않고 DB에서 직접 조회
    public Book findById(Long id) {

        return bookRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("도서를 찾을 수 없습니다.")
                );
    }

    // 상태 변경
    public void updateStatus(Long id, String status) {

        Book book = bookRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("도서를 찾을 수 없습니다.")
                );

        book.setStatus(status);
        bookRepository.save(book);
        redisService.invalidateBookListCache();
    }

    // 30개 묶음을 캐시하고 요청한 페이지의 10개만 반환한다.
    public Page<BookListItem> findPage(int page) {
        if (page < 0) {
            throw new IllegalArgumentException("페이지 번호는 0 이상이어야 합니다.");
        }
        int block = page / 3;
        int offset = (page % 3) * PAGE_SIZE;
        var sort = Sort.by(Sort.Direction.DESC, "id");
        var pageRequest = PageRequest.of(page, PAGE_SIZE, sort);

        // 읽기 도중 변경이 발생해도 이전 결과는 이전 버전에만 저장된다.
        String version = redisService.getBookListVersion();
        BookListBlockCache cache = redisService.getBookListBlock(version, block);
        if (cache == null) {
            Page<Book> result = bookRepository.findAll(
                    PageRequest.of(block, BLOCK_SIZE, sort));
            cache = new BookListBlockCache(
                    result.getContent().stream().map(BookListItem::new).toList(),
                    result.getTotalElements());
            redisService.saveBookListBlock(version, block, cache);
            log.info("[목록 DB 조회] block={}, page={}", block, page + 1);
        } else {
            log.info("[목록 Redis HIT] block={}, page={}", block, page + 1);
        }

        int from = Math.min(offset, cache.getBooks().size());
        int to = Math.min(from + PAGE_SIZE, cache.getBooks().size());
        List<BookListItem> content = cache.getBooks().subList(from, to);
        return new PageImpl<>(content, pageRequest, cache.getTotalCount());
    }

    // 교환 요청
    public void requestExchange(Long bookId) {

        Book book = bookRepository.findById(bookId)
                .orElseThrow(() ->
                        new RuntimeException("도서를 찾을 수 없습니다.")
                );

        book.setStatus("REQUESTED");

        bookRepository.save(book);
        redisService.invalidateBookListCache();
    }

    // 작성자와 상태값이 REQUESTED인 건에 대한 조회 메서드
    public List<Book> findRequestedBooksByOwner(Long ownerId) {

        return bookRepository.findByOwnerIdAndStatus(
                ownerId,
                "REQUESTED"
        );
    }

    // 교환 신청 수락 메서드
    public void acceptExchange(Long bookId, Long ownerId) {

        Book book = bookRepository.findById(bookId)
                .orElseThrow(() ->
                        new RuntimeException("도서를 찾을 수 없습니다.")
                );

        // 현재 로그인 사용자가 실제 책 주인인지 확인
        if (!book.getOwnerId().equals(ownerId)) {
            throw new RuntimeException(
                    "교환 요청을 수락할 권한이 없습니다."
            );
        }

        // REQUESTED 상태에서만 수락 가능
        if (!"REQUESTED".equals(book.getStatus())) {
            throw new RuntimeException(
                    "교환 요청 상태인 도서만 수락할 수 있습니다."
            );
        }

        book.setStatus("EXCHANGED");

        bookRepository.save(book);
        redisService.invalidateBookListCache();
    }

    // 교환 신청 거절 메서드
    public void rejectExchange(Long bookId, Long ownerId) {

        Book book = bookRepository.findById(bookId)
                .orElseThrow(() ->
                        new RuntimeException("도서를 찾을 수 없습니다.")
                );

        // 책 주인만 거절 가능
        if (!book.getOwnerId().equals(ownerId)) {
            throw new RuntimeException(
                    "교환 요청을 거절할 권한이 없습니다."
            );
        }

        // 교환 요청 중인 책만 거절 가능
        if (!"REQUESTED".equals(book.getStatus())) {
            throw new RuntimeException(
                    "교환 요청 상태인 도서만 거절할 수 있습니다."
            );
        }

        // 다시 교환 가능한 상태로 변경
        book.setStatus("REJECTED");

        bookRepository.save(book);
        redisService.invalidateBookListCache();
    }
}