package com.team5.bookexchange.repository;

import com.team5.bookexchange.dto.MyPageExchangeDto;
import com.team5.bookexchange.entity.Exchange;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ExchangeRepository extends JpaRepository<Exchange, Long> {

    // 내가 보낸 교환 요청
    @Query("""
            SELECT new com.team5.bookexchange.dto.MyPageExchangeDto(
                e.id,
                b.id,
                b.title,
                b.status
            )
            FROM Exchange e
            JOIN Book b ON e.bookId = b.id
            WHERE e.requesterId = :memberId
            """)
    List<MyPageExchangeDto> findSentExchanges(
            @Param("memberId") Long memberId
    );


    // 내가 받은 교환 요청
    @Query("""
            SELECT new com.team5.bookexchange.dto.MyPageExchangeDto(
                e.id,
                b.id,
                b.title,
                b.status
            )
            FROM Exchange e
            JOIN Book b ON e.bookId = b.id
            WHERE e.receiverId = :memberId
            """)
    List<MyPageExchangeDto> findReceivedExchanges(
            @Param("memberId") Long memberId
    );
}