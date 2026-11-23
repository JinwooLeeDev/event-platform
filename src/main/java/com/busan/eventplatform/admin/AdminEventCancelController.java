package com.busan.eventplatform.admin;

import com.busan.eventplatform.global.i18n.LocaleMessages;
import com.busan.eventplatform.event.eventCancel.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/event-cancels")
public class AdminEventCancelController {
    private final EventCancelService eventCancelService;

    @GetMapping
    public String list(@RequestParam(defaultValue = "PENDING") EventCancelStatus status, Model model) {
        model.addAttribute("cancelRequests", eventCancelService.reviewRequests(status));
        model.addAttribute("cancelStatusOptions", java.util.Arrays.stream(EventCancelStatus.values())
                .map(s -> java.util.Map.of("value", s.name(), "label", EventCancelService.statusLabel(s), "selected", s == status)).toList());
        return AdminPageView.prepare(model, LocaleMessages.display("회차 취소 신청"), LocaleMessages.display("기업이 제출한 취소 사유와 회차 정보를 확인하고 승인 또는 거절하세요."), "cancels");
    }

    @PostMapping("/{id}/approve")
    public String approve(@PathVariable Long id, RedirectAttributes redirect) {
        eventCancelService.approve(id);
        redirect.addFlashAttribute("reviewSuccess", LocaleMessages.display("취소 신청을 승인했습니다."));
        return "redirect:/admin/event-cancels";
    }

    @PostMapping("/{id}/reject")
    public String reject(@PathVariable Long id, RedirectAttributes redirect) {
        eventCancelService.reject(id);
        redirect.addFlashAttribute("reviewSuccess", LocaleMessages.display("취소 신청을 거절했습니다. 회차는 기존 상태를 유지합니다."));
        return "redirect:/admin/event-cancels";
    }
}
