package com.team5.bookexchange.controller;

import com.team5.bookexchange.entity.ExchangeRequest;
import com.team5.bookexchange.entity.Member;
import com.team5.bookexchange.repository.BookRepository;
import com.team5.bookexchange.service.BookService;
import com.team5.bookexchange.service.ExchangeRequestService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/exchanges")
public class ExchangeRequestController {

    private final ExchangeRequestService exchangeRequestService;
    private final BookService bookService;
    private final BookRepository bookRepository;

    public ExchangeRequestController(
            ExchangeRequestService exchangeRequestService,
            BookService bookService, BookRepository bookRepository) {

        this.exchangeRequestService = exchangeRequestService;
        this.bookService = bookService;
        this.bookRepository = bookRepository;
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
    // 내가 보낸 교환 요청,
    @GetMapping("/sent")
    public String findSent(Model model, HttpSession session) {
        Member loginMember = (Member) session.getAttribute("loginMember");
        if (loginMember == null) return "redirect:/login";

        model.addAttribute(
                "exchangeRequests",
                exchangeRequestService.findSent(loginMember.getId())
        );
        return "exchanges";
    }
    // 받은 교환 요청
    @GetMapping("/received")
    public String findReceived(Model model, HttpSession session) {
        Member loginMember = (Member) session.getAttribute("loginMember");
        if (loginMember == null) return "redirect:/login";

        model.addAttribute(
                "exchangeRequests",
                exchangeRequestService.findReceived(loginMember.getId())
        );

        return "exchanges";
    }
}