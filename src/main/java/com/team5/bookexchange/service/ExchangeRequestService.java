package com.team5.bookexchange.service;

import com.team5.bookexchange.entity.ExchangeRequest;
import com.team5.bookexchange.repository.ExchangeRequestRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ExchangeRequestService {

    private final ExchangeRequestRepository exchangeRequestRepository;

    public ExchangeRequestService(
            ExchangeRequestRepository exchangeRequestRepository) {
        this.exchangeRequestRepository = exchangeRequestRepository;
    }

    // 교환 요청 생성
    public ExchangeRequest create(Long bookId) {
        ExchangeRequest request = new ExchangeRequest();

        request.setBookId(bookId);
        request.setStatus("PENDING");

        return exchangeRequestRepository.save(request);
    }

    // 전체 교환 요청 조회
    public List<ExchangeRequest> findAll() {
        return exchangeRequestRepository.findAll();
    }

    // 전체 교환 요청 수
    public long countAll() {
        return exchangeRequestRepository.count();
    }
}