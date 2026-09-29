package com.team5.bookexchange.controller;

import com.team5.bookexchange.entity.Book;
import com.team5.bookexchange.service.BookService;
import com.team5.bookexchange.service.ExchangeRequestService;
import com.team5.bookexchange.service.ImageService;
import com.team5.bookexchange.service.RedisService;
import com.team5.bookexchange.entity.Member;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@Controller
@RequestMapping("/books")
public class BookController {

    private final BookService bookService;
    private final ExchangeRequestService exchangeRequestService;
    private final ImageService imageService;
    private final RedisService redisService;

    public BookController(
            BookService bookService,
            ExchangeRequestService exchangeRequestService,
            ImageService imageService,
            RedisService redisService) {

        this.bookService = bookService;
        this.exchangeRequestService = exchangeRequestService;
        this.imageService = imageService;
        this.redisService = redisService;
    }

    // 도서 목록
    @GetMapping
    public String findAll(Model model) {

        var books = bookService.findAll();

        var viewCounts = new java.util.HashMap<Long, Long>();

        for (Book book : books) {
            viewCounts.put(
                    book.getId(),
                    redisService.getViewCount(book.getId())
            );
        }

        model.addAttribute("books", books);
        model.addAttribute("viewCounts", viewCounts);

        return "books";
    }

    // 도서 등록 화면
    @GetMapping("/new")
    public String createForm(HttpSession session) {

        if (session.getAttribute("loginMember") == null) {
            return "redirect:/login";
        }

        return "book-form";
    }

    // 도서 등록
    @PostMapping
    public String save(
            @RequestParam String title,
            @RequestParam String author,
            @RequestParam String description,
            @RequestParam(required = false) MultipartFile image,
            HttpSession session) {

        // 로그인하지 않았다면 저장하지 않고 로그인 화면으로 이동
        Member loginMember =
                (Member) session.getAttribute("loginMember");

        if (loginMember == null) {
            return "redirect:/login";
        }

        Book book = new Book();

        // 로그인한 회원을 게시글 작성자로 저장
        book.setOwnerId(loginMember.getId());

        book.setTitle(title);
        book.setAuthor(author);
        book.setDescription(description);
        book.setStatus("AVAILABLE");

        // 이미지 저장
        String imageName = imageService.save(image);

        // 저장된 이미지 파일명을 Book에 저장
        book.setImageName(imageName);

        // DB 저장
        bookService.save(book);

        return "redirect:/books";
    }

    // 도서 상세
    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {

        // 상세 조회 전에 Redis 캐시 존재 여부 확인
        boolean cacheHit = redisService.hasBookCache(id);

        // 실제 상세 조회
        Book book = bookService.findById(id);

        // 상세 페이지 접속이므로 조회수 +1
        Long viewCount = redisService.increaseViewCount(id);

        model.addAttribute("book", book);
        model.addAttribute("viewCount", viewCount);
        model.addAttribute("cacheHit", cacheHit);

        return "book-detail";
    }

    // 교환 요청
    @PostMapping("/{id}/exchange")
    @ResponseBody
    public ResponseEntity<String> exchange(
            @PathVariable Long id,
            HttpSession session) {

        Member loginMember =
                (Member) session.getAttribute("loginMember");

        if (loginMember == null) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body("LOGIN_REQUIRED");
        }

        Book book = bookService.findById(id);

        // 작성자 정보가 없는 기존 게시글은 요청 차단
        if (book.getOwnerId() == null) {
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body("작성자 정보가 없는 게시글입니다.");
        }

        // 자신의 게시글에는 교환 요청 불가
        if (book.getOwnerId().equals(loginMember.getId())) {
            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body("자신의 게시글에는 교환을 요청할 수 없습니다.");
        }

        // 교환 가능한 도서인지 확인
        if (!"AVAILABLE".equals(book.getStatus())) {
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body("현재 교환 요청이 가능한 도서가 아닙니다.");
        }

        exchangeRequestService.create(id);
        bookService.updateStatus(id, "REQUESTED");

        return ResponseEntity.ok("교환 요청이 완료되었습니다.");
    }

    @PostMapping("/{id}/cache/reset")
    @ResponseBody
    public String resetCache(@PathVariable Long id) {

        redisService.deleteBookCache(id);

        return "OK";
    }
}