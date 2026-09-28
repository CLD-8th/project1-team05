package com.team5.bookexchange.dto;

import com.team5.bookexchange.entity.Book;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class BookCache {

    private Long id;
    private String title;
    private String author;
    private String description;
    private String status;
    private String imageName;

    public BookCache(Book book) {
        this.id = book.getId();
        this.title = book.getTitle();
        this.author = book.getAuthor();
        this.description = book.getDescription();
        this.status = book.getStatus();
        this.imageName = book.getImageName();
    }

    public Book toEntity() {
        Book book = new Book();

        book.setId(id);
        book.setTitle(title);
        book.setAuthor(author);
        book.setDescription(description);
        book.setStatus(status);
        book.setImageName(imageName);

        return book;
    }
}