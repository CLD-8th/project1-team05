package com.team5.bookexchange.controller;

import com.team5.bookexchange.entity.ExchangeRequest;
import com.team5.bookexchange.service.BookService;
import com.team5.bookexchange.service.ExchangeRequestService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/exchanges")
public class ExchangeRequestController {

    private final ExchangeRequestService exchangeRequestService;
    private final BookService bookService;

    public ExchangeRequestController(
            ExchangeRequestService exchangeRequestService,
            BookService bookService) {

        this.exchangeRequestService = exchangeRequestService;
        this.bookService = bookService;
    }

    // 교환 요청 목록
    @GetMapping
    public String findAll(Model model) {

        model.addAttribute(
                "exchangeRequests",
                exchangeRequestService.findAll()
        );

        return "exchanges";
    }

    @PostMapping("/{id}/accept")
    @ResponseBody
    public String accept(@PathVariable Long id) {
        ExchangeRequest request = exchangeRequestService.accept(id);

        bookService.updateStatus(
                request.getBookId(),
                "EXCHANGED"
        );

        return "교환이 완료되었습니다.";
    }
}