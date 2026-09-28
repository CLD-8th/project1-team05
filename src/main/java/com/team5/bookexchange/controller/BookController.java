package com.team5.bookexchange.controller;

import com.team5.bookexchange.entity.Book;
import com.team5.bookexchange.service.BookService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/books")
public class BookController {

    private final BookService bookService;

    public BookController(BookService bookService) {
        this.bookService = bookService;
    }

    @GetMapping
    public List<Book> findAll() {
        return bookService.findAll();
    }

    @PostMapping
    public Book save(@RequestBody Book book) {
        return bookService.save(book);
    }
}