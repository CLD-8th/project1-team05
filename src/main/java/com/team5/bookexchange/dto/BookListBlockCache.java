package com.team5.bookexchange.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class BookListBlockCache {
    private List<BookListItem> books;
    private long totalCount;

    public BookListBlockCache(List<BookListItem> books, long totalCount) {
        this.books = books;
        this.totalCount = totalCount;
    }
}
