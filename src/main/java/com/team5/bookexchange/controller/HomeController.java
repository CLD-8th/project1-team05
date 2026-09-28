package com.team5.bookexchange.controller;

import com.team5.bookexchange.service.BookService;
import com.team5.bookexchange.service.ExchangeRequestService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    private final BookService bookService;
    private final ExchangeRequestService exchangeRequestService;

    public HomeController(
            BookService bookService,
            ExchangeRequestService exchangeRequestService) {
        this.bookService = bookService;
        this.exchangeRequestService = exchangeRequestService;
    }

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("bookCount", bookService.countAll());
        model.addAttribute("availableCount", bookService.countAvailable());
        model.addAttribute("exchangeCount", exchangeRequestService.countAll());
        model.addAttribute("recentBooks", bookService.findRecent());

        return "index";
    }
}