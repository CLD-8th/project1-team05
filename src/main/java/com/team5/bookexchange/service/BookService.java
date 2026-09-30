package com.team5.bookexchange.service;

import com.team5.bookexchange.entity.Book;
import com.team5.bookexchange.repository.BookRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BookService {

    private final BookRepository bookRepository;

    public BookService(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    public List<Book> findAll() {
        return bookRepository.findAll();
    }

    // 도서 저장
    public Book save(Book book) {
        return bookRepository.save(book);
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
    }

    // 목록 캐시 없이 기존 DB 페이징 유지
    public Page<Book> findPage(int page) {

        PageRequest pageRequest = PageRequest.of(
                page,
                10,
                Sort.by(Sort.Direction.DESC, "id")
        );

        return bookRepository.findAll(pageRequest);
    }

    // 교환 요청
    public void requestExchange(Long bookId) {

        Book book = bookRepository.findById(bookId)
                .orElseThrow(() ->
                        new RuntimeException("도서를 찾을 수 없습니다.")
                );

        book.setStatus("REQUESTED");

        bookRepository.save(book);
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
    }
}