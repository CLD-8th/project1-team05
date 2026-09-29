package com.team5.bookexchange.dto;

import lombok.Getter;

@Getter
public class MyPageExchangeDto {

    private final Long exchangeId;
    private final Long bookId;
    private final String bookTitle;
    private final String status;

    public MyPageExchangeDto(
            Long exchangeId,
            Long bookId,
            String bookTitle,
            String status) {

        this.exchangeId = exchangeId;
        this.bookId = bookId;
        this.bookTitle = bookTitle;
        this.status = status;
    }
}