package com.team5.bookexchange.controller;

import com.team5.bookexchange.entity.Book;
import com.team5.bookexchange.service.BookService;
import com.team5.bookexchange.service.RedisService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class HomeController {

    private final BookService bookService;
    private final RedisService redisService;

    public HomeController(
            BookService bookService,
            RedisService redisService) {

        this.bookService = bookService;
        this.redisService = redisService;
    }

    @GetMapping("/")
    public String home(Model model) {

        // DB에 등록된 모든 도서 조회
        List<Book> books = bookService.findAll();

        // 전체 등록 도서 수
        long totalCount = books.size();

        // 교환 가능한 도서 수
        long availableCount = books.stream()
                .filter(book -> "AVAILABLE".equals(book.getStatus()))
                .count();

        // 교환 요청 중인 도서 수
        long requestedCount = books.stream()
                .filter(book -> "REQUESTED".equals(book.getStatus()))
                .count();

        // 최근 등록된 도서 3권
        List<Book> recentBooks = books.stream()
                .sorted((a, b) -> b.getId().compareTo(a.getId()))
                .limit(3)
                .toList();

        // 최근 도서 조회수 Redis에서 가져오기
        Map<Long, Long> viewCounts = new HashMap<>();

        for (Book book : recentBooks) {
            viewCounts.put(
                    book.getId(),
                    redisService.getViewCount(book.getId())
            );
        }

        model.addAttribute("totalCount", totalCount);
        model.addAttribute("availableCount", availableCount);
        model.addAttribute("requestedCount", requestedCount);
        model.addAttribute("recentBooks", recentBooks);
        model.addAttribute("viewCounts", viewCounts);

        return "index";
    }
}