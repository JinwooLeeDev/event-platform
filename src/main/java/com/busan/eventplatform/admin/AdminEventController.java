package com.busan.eventplatform.admin;

import com.busan.eventplatform.event.events.*;
import com.busan.eventplatform.event.event_session.EventSessionService;
import com.busan.eventplatform.global.error.exception.Exception404;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/events")
public class AdminEventController {
    private final EventRepository eventRepository;
    private final EventService eventService;
    private final EventSessionService eventSessionService;

    @GetMapping
    public String list(Model model) {
        var events = eventRepository.findReviewTargets(EventStatus.PENDING_REVIEW);
        model.addAttribute("eventListPage", true);
        model.addAttribute("reviewEvents", events);
        model.addAttribute("eventReviewCount", events.size());
        return AdminPageView.prepare(model, "행사 심사", "기업이 신청한 행사 정보를 확인하고 승인 또는 보완을 요청하세요.", "events");
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        var event = eventRepository.findByDetail(id).orElseThrow(() -> new Exception404("행사를 찾을 수 없습니다."));
        model.addAttribute("eventDetailPage", true);
        model.addAttribute("reviewEvent", event);
        model.addAttribute("reviewable", event.getStatus() == EventStatus.PENDING_REVIEW);
        model.addAttribute("reviewSessions", eventSessionService.findByEventId(id));
        return AdminPageView.prepare(model, "행사 상세 심사", "제출된 행사와 회차 정보를 확인하세요.", "events");
    }

    @PostMapping("/{id}/approve")
    public String approve(@PathVariable Long id, RedirectAttributes redirect) {
        eventService.approve(id);
        redirect.addFlashAttribute("reviewSuccess", "행사를 승인했습니다. 기업 결제 대기 상태로 변경되었습니다.");
        return "redirect:/admin/events";
    }

    @PostMapping("/{id}/feedback")
    public String feedback(@PathVariable Long id, @RequestParam String feedbackMessage, RedirectAttributes redirect) {
        eventService.requestFeedback(id, feedbackMessage);
        redirect.addFlashAttribute("reviewSuccess", "기업에 보완을 요청했습니다.");
        return "redirect:/admin/events";
    }
}
