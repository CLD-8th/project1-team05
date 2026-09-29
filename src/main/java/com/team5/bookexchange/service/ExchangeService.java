package com.team5.bookexchange.service;

import com.team5.bookexchange.dto.MyPageExchangeDto;
import com.team5.bookexchange.entity.Exchange;
import com.team5.bookexchange.repository.ExchangeRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ExchangeService {

    private final ExchangeRepository exchangeRepository;

    public ExchangeService(ExchangeRepository exchangeRepository) {
        this.exchangeRepository = exchangeRepository;
    }

    public Exchange create(
            Long bookId,
            Long requesterId,
            Long receiverId
    ) {
        Exchange exchange = new Exchange();

        exchange.setBookId(bookId);
        exchange.setRequesterId(requesterId);
        exchange.setReceiverId(receiverId);

        return exchangeRepository.save(exchange);
    }

    // 내가 받은 교환 요청 조회
    public List<MyPageExchangeDto> findReceivedExchanges(Long memberId) {

        return exchangeRepository.findReceivedExchanges(memberId);
    }
}
