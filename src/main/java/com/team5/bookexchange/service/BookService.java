package com.team5.bookexchange.service;

import com.team5.bookexchange.dto.BookCache;
import com.team5.bookexchange.dto.LatestBookIdsCache;
import com.team5.bookexchange.entity.Book;
import com.team5.bookexchange.repository.BookRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class BookService {
    private final BookRepository bookRepository;
    private final RedisService redisService;

    public BookService(BookRepository bookRepository, RedisService redisService) {
        this.bookRepository = bookRepository;
        this.redisService = redisService;
    }

    public List<Book> findAll() {
        return bookRepository.findAll();
    }

    // 저장 후 최신 30개 상세 캐시 갱신
    public Book save(Book book) {
        Book savedBook = bookRepository.save(book);
        redisService.deleteBookCache(savedBook.getId());
        rebuildLatestBooksCache();
        return savedBook;
    }

    // 최신 30개뿐 아니라 실제 조회한 오래된 글도 공용 상세 캐시에 저장한다.
    public Book findById(Long id) {
        LatestBookIdsCache latestCache = getOrCreateLatestBooksCache();
        BookCache cachedBook = redisService.getBookCache(id);
        if (cachedBook != null) {
            System.out.println("[Redis HIT] book:" + id);
            return cachedBook.toEntity();
        }

        System.out.println("[Redis MISS / DB 조회] book:" + id);
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("도서를 찾을 수 없습니다."));

        boolean latest = latestCache.getBookIds().contains(id);
        redisService.saveBookCache(new BookCache(book), latest);
        return book;
    }

    // 상태 변경 시 해당 상세 캐시만 삭제하고 다음 조회에서 다시 저장한다.
    public void updateStatus(Long id, String status) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("도서를 찾을 수 없습니다."));
        book.setStatus(status);
        bookRepository.save(book);
        redisService.deleteBookCache(id);
    }

    // 목록 캐시 없이 기존 DB 페이징 유지
    public Page<Book> findPage(int page) {
        PageRequest pageRequest = PageRequest.of(
                page, 10, Sort.by(Sort.Direction.DESC, "id"));
        return bookRepository.findAll(pageRequest);
    }

    public List<Book> findLatest30() {
        var pageable = PageRequest.of(0, 30, Sort.by(Sort.Direction.DESC, "id"));
        return bookRepository.findAll(pageable).getContent();
    }

    private LatestBookIdsCache rebuildLatestBooksCache() {
        LatestBookIdsCache previousCache = redisService.getLatestBookIdsCache();
        List<Book> latestBooks = findLatest30();
        List<Long> bookIds = new ArrayList<>();

        for (Book book : latestBooks) {
            redisService.saveBookCache(new BookCache(book), true);
            bookIds.add(book.getId());
        }

        // 밀려난 글도 재조회에 활용하도록 삭제 대신 30분 TTL을 부여한다.
        if (previousCache != null) {
            for (Long previousId : previousCache.getBookIds()) {
                if (!bookIds.contains(previousId)) {
                    redisService.expireBookCache(previousId);
                }
            }
        }

        LatestBookIdsCache cache = new LatestBookIdsCache(bookIds);
        redisService.saveLatestBookIdsCache(cache);
        return cache;
    }

    private LatestBookIdsCache getOrCreateLatestBooksCache() {
        LatestBookIdsCache cache = redisService.getLatestBookIdsCache();
        if (cache == null) {
            return rebuildLatestBooksCache();
        }
        return cache;
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
