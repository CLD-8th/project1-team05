package com.team5.bookexchange.controller;

import com.team5.bookexchange.entity.Member;
import com.team5.bookexchange.service.MemberService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class AuthController {

    private final MemberService memberService;

    public AuthController(MemberService memberService) {
        this.memberService = memberService;
    }

    // 회원가입 화면
    @GetMapping("/signup")
    public String signupForm() {
        return "signup";
    }

    // 회원가입 처리
    @PostMapping("/signup")
    public String signup(
            @RequestParam String username,
            @RequestParam String password,
            @RequestParam String name,
            Model model) {

        try {
            memberService.signup(username, password, name);
            return "redirect:/login";

        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("username", username);
            model.addAttribute("name", name);

            return "signup";
        }
    }

    // 로그인 화면
    @GetMapping("/login")
    public String loginForm() {
        return "login";
    }

    // 로그인 처리
    @PostMapping("/login")
    public String login(
            @RequestParam String username,
            @RequestParam String password,
            HttpSession session,
            Model model) {

        try {
            Member member = memberService.login(username, password);

            session.setAttribute("loginMember", member);

            return "redirect:/";

        } catch (IllegalArgumentException e) {

            model.addAttribute("error", e.getMessage());

            return "login";
        }
    }

    // 로그아웃
    @GetMapping("/logout")
    public String logout(HttpSession session) {

        session.invalidate();

        return "redirect:/";
    }
}