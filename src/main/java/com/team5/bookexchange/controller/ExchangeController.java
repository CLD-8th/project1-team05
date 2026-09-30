package com.team5.bookexchange.controller;

import com.team5.bookexchange.entity.Member;
import com.team5.bookexchange.service.BookService;
import com.team5.bookexchange.service.ExchangeService;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/exchanges")
public class ExchangeController {

    private final ExchangeService exchangeService;
    private final BookService bookService;

    public ExchangeController(
            ExchangeService exchangeService,
            BookService bookService) {

        this.exchangeService = exchangeService;
        this.bookService = bookService;
    }

    private static final Logger exchangeLog =
            LoggerFactory.getLogger("EXCHANGE_LOG");

    // 내가 받은 교환 요청 목록
    @GetMapping
    public String findAll(
            HttpSession session,
            Model model) {

        Member loginMember =
                (Member) session.getAttribute("loginMember");

        if (loginMember == null) {
            return "redirect:/login";
        }

        model.addAttribute(
                "exchanges",
                exchangeService.findReceivedExchanges(
                        loginMember.getId()
                )
        );

        return "exchanges";
    }

    @PostMapping("/{bookId}/accept")
    public String accept(
            @PathVariable Long bookId,
            HttpSession session) {

        Member loginMember =
                (Member) session.getAttribute("loginMember");

        if (loginMember == null) {
            return "redirect:/login";
        }

        bookService.acceptExchange(
                bookId,
                loginMember.getId()
        );

        exchangeLog.info(
                "[ACCEPT] receiverId={} bookId={}",
                loginMember.getId(),
                bookId
        );

        return "redirect:/exchanges";
    }

    @PostMapping("/{bookId}/reject")
    public String reject(
            @PathVariable Long bookId,
            HttpSession session) {

        Member loginMember =
                (Member) session.getAttribute("loginMember");

        if (loginMember == null) {
            return "redirect:/login";
        }

        bookService.rejectExchange(
                bookId,
                loginMember.getId()
        );

        exchangeLog.info(
                "[REJECT] receiverId={} bookId={}",
                loginMember.getId(),
                bookId
        );

        return "redirect:/exchanges";
    }
}