package com.team5.bookexchange.repository;

import com.team5.bookexchange.entity.ExchangeRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExchangeRequestRepository
        extends JpaRepository<ExchangeRequest, Long> {
    List<ExchangeRequest> findByRequesterIdOrderByIdDesc(Long requesterId);

    List<ExchangeRequest> findByBookIdInOrderByIdDesc(List<Long> bookIds);

}
