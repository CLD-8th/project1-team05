package com.team5.bookexchange.controller;

import com.team5.bookexchange.entity.Book;
import com.team5.bookexchange.service.BookService;
import com.team5.bookexchange.service.ExchangeRequestService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/books")
public class BookController {

    private final BookService bookService;
    private final ExchangeRequestService exchangeRequestService;

    public BookController(
            BookService bookService,
            ExchangeRequestService exchangeRequestService) {

        this.bookService = bookService;
        this.exchangeRequestService = exchangeRequestService;
    }

    // 도서 목록
    @GetMapping
    public String findAll(Model model) {

        model.addAttribute("books", bookService.findAll());

        return "books";
    }

    // 도서 등록 화면
    @GetMapping("/new")
    public String createForm() {

        return "book-form";
    }

    // 도서 등록
    @PostMapping
    public String save(
            @RequestParam String title,
            @RequestParam String author,
            @RequestParam String description) {

        Book book = new Book();

        book.setTitle(title);
        book.setAuthor(author);
        book.setDescription(description);
        book.setStatus("AVAILABLE");

        bookService.save(book);

        return "redirect:/books";
    }

    // 도서 상세
    @GetMapping("/{id}")
    public String findById(
            @PathVariable Long id,
            Model model) {

        Book book = bookService.findById(id);

        model.addAttribute("book", book);

        return "book-detail";
    }

    // 교환 요청
    @PostMapping("/{id}/exchange")
    @ResponseBody
    public String exchange(@PathVariable Long id) {

        // 도서 존재 확인
        bookService.findById(id);

        // 교환 요청 생성
        exchangeRequestService.create(id);

        // 도서 상태 변경
        bookService.updateStatus(id, "REQUESTED");

        return "교환 요청이 완료되었습니다.";
    }
}