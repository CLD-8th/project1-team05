package com.team5.bookexchange.controller;

import com.team5.bookexchange.entity.Member;
import com.team5.bookexchange.repository.BookRepository;
import com.team5.bookexchange.repository.ExchangeRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import com.team5.bookexchange.entity.Book;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class MyPageController {

    private final BookRepository bookRepository;
    private final ExchangeRepository exchangeRepository;

    public MyPageController(
            BookRepository bookRepository,
            ExchangeRepository exchangeRepository) {

        this.bookRepository = bookRepository;
        this.exchangeRepository = exchangeRepository;
    }

    @GetMapping("/mypage")
    public String myPage(HttpSession session, Model model) {

        // 로그인할 때 세션에 저장한 회원 정보 가져오기
        Member loginMember =
                (Member) session.getAttribute("loginMember");

        // 로그인하지 않았다면 로그인 화면으로 이동
        if (loginMember == null) {
            return "redirect:/login";
        }

        Long memberId = loginMember.getId();

        // 회원 정보
        model.addAttribute("member", loginMember);

        // 내가 등록한 책
        model.addAttribute(
                "myBooks",
                bookRepository.findByOwnerId(memberId)
        );

        // 내가 보낸 교환 요청
        model.addAttribute(
                "sentExchanges",
                exchangeRepository.findSentExchanges(memberId)
        );

        // 내가 받은 교환 요청
        model.addAttribute(
                "receivedExchanges",
                exchangeRepository.findReceivedExchanges(memberId)
        );

        return "mypage";
    }

    @PostMapping("/mypage/exchanges/{bookId}/status")
    public String updateExchangeStatus(
            @PathVariable Long bookId,
            @RequestParam String status,
            HttpSession session) {

        Member loginMember =
                (Member) session.getAttribute("loginMember");

        if (loginMember == null) {
            return "redirect:/login";
        }

        Book book = bookRepository.findById(bookId)
                .orElseThrow();

        // 내가 등록한 책인지 확인
        if (!book.getOwnerId().equals(loginMember.getId())) {
            return "redirect:/mypage";
        }

        // 허용할 상태만 변경
        if ("EXCHANGED".equals(status)
                || "REJECTED".equals(status)) {

            book.setStatus(status);
            bookRepository.save(book);
        }

        return "redirect:/mypage";
    }
}