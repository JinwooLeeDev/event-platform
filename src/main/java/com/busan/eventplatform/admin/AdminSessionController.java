package com.busan.eventplatform.admin;

import com.busan.eventplatform.event.event_session.EventSessionRepository;
import com.busan.eventplatform.event.event_session.EventSessionResponse;
import com.busan.eventplatform.event.event_session.EventSessionStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import java.util.Arrays;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class AdminSessionController {
    private final EventSessionRepository eventSessionRepository;

    @GetMapping("/admin/sessions")
    public String sessions(@RequestParam(required = false) EventSessionStatus status,
                           @RequestParam(defaultValue = "1") int page, Model model) {
        var result = eventSessionRepository.findForAdmin(status, PageRequest.of(Math.max(0, page - 1), 20));
        model.addAttribute("sessions", result.getContent().stream().map(session -> Map.of(
                "session", new EventSessionResponse.EventSessionDto(session), "event", session.getEvent())).toList());
        model.addAttribute("statusOptions", Arrays.stream(EventSessionStatus.values()).map(value -> Map.of(
                "value", value.name(), "label", statusLabel(value), "selected", value == status)).toList());
        model.addAttribute("statusValue", status == null ? "" : status.name());
        model.addAttribute("totalCount", result.getTotalElements());
        model.addAttribute("currentPage", result.getNumber() + 1);
        model.addAttribute("totalPages", Math.max(1, result.getTotalPages()));
        model.addAttribute("hasPrevious", result.hasPrevious());
        model.addAttribute("hasNext", result.hasNext());
        model.addAttribute("previousPage", result.getNumber());
        model.addAttribute("nextPage", result.getNumber() + 2);
        return AdminPageView.prepare(model, "회차 현황", "회차별 일정과 모집·정원 현황을 확인하세요.", "sessions");
    }

    private static String statusLabel(EventSessionStatus status) {
        return switch (status) {
            case SCHEDULED -> "진행 예정";
            case IN_PROGRESS -> "진행 중";
            case COMPLETED -> "종료";
            case CANCELLED -> "취소";
        };
    }
}
