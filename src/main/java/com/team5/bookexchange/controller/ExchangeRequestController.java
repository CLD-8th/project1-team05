package com.team5.bookexchange.controller;

import com.team5.bookexchange.service.ExchangeRequestService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/exchanges")
public class ExchangeRequestController {

    private final ExchangeRequestService exchangeRequestService;

    public ExchangeRequestController(
            ExchangeRequestService exchangeRequestService) {

        this.exchangeRequestService = exchangeRequestService;
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
}