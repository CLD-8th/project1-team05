package com.team5.bookexchange.service;

import com.team5.bookexchange.entity.Book;
import com.team5.bookexchange.entity.ExchangeRequest;
import com.team5.bookexchange.repository.BookRepository;
import com.team5.bookexchange.repository.ExchangeRequestRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ExchangeRequestService {

    private final ExchangeRequestRepository exchangeRequestRepository;
    private final BookRepository bookRepository;

    public ExchangeRequestService(
            ExchangeRequestRepository exchangeRequestRepository, BookRepository bookRepository) {

        this.exchangeRequestRepository = exchangeRequestRepository;
        this.bookRepository = bookRepository;
    }

    // 교환 요청 생성
    public ExchangeRequest create(Long bookId, Long requesterId) {

        ExchangeRequest request = new ExchangeRequest();

        request.setBookId(bookId);
        request.setStatus("PENDING");
        request.setRequesterId(requesterId);

        return exchangeRequestRepository.save(request);
    }

    // 교환 요청 수락
    public ExchangeRequest accept(Long requestId) {

        ExchangeRequest request = exchangeRequestRepository.findById(requestId)
                .orElseThrow(() ->
                        new RuntimeException("교환 요청을 찾을 수 없습니다.")
                        );
        if (!"PENDING".equals(request.getStatus())) {
            throw new RuntimeException("대기 중인 교환 요청만 수락할 수 있습니다.");
        }

        request.setStatus("ACCEPTED");

        return exchangeRequestRepository.save(request);
    }

    // 전체 교환 요청 조회
    public List<ExchangeRequest> findAll() {

        return exchangeRequestRepository.findAll();
    }

    //내가 보낸 교환 요청
    public List<ExchangeRequest> findSent(Long userId) {
        return exchangeRequestRepository.findByRequesterIdOrderByIdDesc(userId);
    }
    //받은 교환 요청
    public List<ExchangeRequest> findReceived(Long userId) {
        List<Long> bookIds = bookRepository.findAllByOwnerId(userId).stream()
                .map(Book::getId).toList();

        return exchangeRequestRepository.findByBookIdInOrderByIdDesc(bookIds);
    }
}
