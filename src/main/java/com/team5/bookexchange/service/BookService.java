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
}
