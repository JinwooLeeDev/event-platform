package com.busan.eventplatform.event.event_cancel;

import com.busan.eventplatform.global.util.Define;
import com.busan.eventplatform.login.companylogin.SessionCompanyDto;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@RequiredArgsConstructor
@Controller
public class EventCancelController {

    private final EventCancelService eventCancelService;

    // GET - http://localhost:8080/event-cancels/save/1
    @GetMapping("/event-cancels/save/{sessionId}")
    public String form(@PathVariable Long sessionId, HttpSession session, Model model) {

        model.addAttribute("sessionId", sessionId);

        return "event_cancel/save-form";
    }

    // 회차 취소 신청
    // POST - http://localhost:8080/event-cancels/save/1
    @PostMapping("/event-cancels/save/{sessionId}")
    public String save(@PathVariable Long sessionId,
                       EventCancelRequest.SaveDto saveDto,
                       HttpSession session) {

        SessionCompanyDto sessionCompanyDto
                = (SessionCompanyDto) session.getAttribute(Define.SESSION_COMPANY);

        // 취소 신청서 저장
        eventCancelService.save(
                sessionCompanyDto.getId(),
                sessionId,
                saveDto.getReason()
        );

        return "redirect:/events";
    }
}
