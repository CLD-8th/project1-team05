package com.team5.bookexchange.dto;

import com.team5.bookexchange.entity.Book;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

// 목록 화면과 목록 캐시에서 사용하는 도서 한 건.
@Getter
@Setter
@NoArgsConstructor
public class BookListItem {
    private Long id;
    private String title;
    private String author;
    private String status;
    private String imageName;

    public BookListItem(Book book) {
        this.id = book.getId();
        this.title = book.getTitle();
        this.author = book.getAuthor();
        this.status = book.getStatus();
        this.imageName = book.getImageName();
    }
}
