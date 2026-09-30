package com.team5.bookexchange.controller;

import com.team5.bookexchange.entity.Book;
import com.team5.bookexchange.service.BookService;
import com.team5.bookexchange.service.ExchangeService;
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
    private final ImageService imageService;
    private final RedisService redisService;
    private final ExchangeService exchangeService;

    public BookController(
            BookService bookService,
            ImageService imageService,
            RedisService redisService,
            ExchangeService exchangeService) {

        this.bookService = bookService;
        this.imageService = imageService;
        this.redisService = redisService;
        this.exchangeService = exchangeService;
    }

    // 도서 목록
    @GetMapping
    public String findAll(@RequestParam(defaultValue = "1") int page, Model model) {

        page = Math.max(page, 1);
        var books = bookService.findPage(page - 1);

        var viewCounts = new java.util.HashMap<Long, Long>();

        for (Book book : books) {
            viewCounts.put(
                    book.getId(),
                    redisService.getViewCount(book.getId())
            );
        }

        model.addAttribute("books", books.getContent());
        model.addAttribute("bookPage", books);
        model.addAttribute("viewCounts", viewCounts);

        int currentPage = books.getNumber() + 1;
        int endPage = Math.min(books.getTotalPages(), currentPage + 2);
        int startPage = Math.max(1, Math.min(currentPage - 2, endPage - 4));
        model.addAttribute("startPage", startPage);
        model.addAttribute("endPage", endPage);

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

        // 도서 정보는 MySQL에서 조회
        Book book = bookService.findById(id);

        // 조회수는 Redis에서 관리
        Long viewCount = redisService.increaseViewCount(id);

        model.addAttribute("book", book);
        model.addAttribute("viewCount", viewCount);

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
        if (!"AVAILABLE".equals(book.getStatus()) && !"REJECTED".equals(book.getStatus())) {
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body("현재 교환 요청이 가능한 도서가 아닙니다.");
        }

        bookService.findById(id);

        exchangeService.create(
                book.getId(),
                loginMember.getId(),
                book.getOwnerId()
        );

        bookService.requestExchange(id);

        return ResponseEntity.ok("교환 요청이 완료되었습니다.");
    }

}