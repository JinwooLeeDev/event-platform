package com.busan.eventplatform.event.events;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Slf4j
@Controller
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    // 행사 목록 조회
    // GEt - http://localhost:8080/events/list
    @GetMapping("/events/list")
    public String list(
        @RequestParam(name = "page", defaultValue = "1") int page,
        @RequestParam(name = "size", defaultValue = "10") int size,
        @RequestParam(name = "keyword", defaultValue = "") String keyword,
        Model model) {

        EventResponse.PageDto pageDto = eventService.findAllPage(page, size, keyword);
        model.addAttribute("eventPage", pageDto);
        model.addAttribute("keyword", keyword);
        model.addAttribute("isSearch", !keyword.isBlank());

        return "event/list";
    }

    // 행사 상세 조회
    // GEt - http://localhost:8080/evets/{1}
    @PostMapping("/event/{id}")
    public String detail(@PathVariable Long id, Model model, HttpSession session) {

    }




}
