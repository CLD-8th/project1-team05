package com.team5.bookexchange.repository;

import com.team5.bookexchange.entity.ExchangeRequest;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExchangeRequestRepository
        extends JpaRepository<ExchangeRequest, Long> {
}