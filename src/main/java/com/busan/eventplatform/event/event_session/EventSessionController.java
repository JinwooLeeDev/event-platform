package com.busan.eventplatform.event.event_session;

import com.busan.eventplatform.event.EventSession;
import com.busan.eventplatform.global.util.Define;
import com.busan.eventplatform.login.companylogin.SessionCompanyDto;
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
@RequiredArgsConstructor
@Controller
public class EventSessionController {

    private final EventSessionService eventSessionService;

    // 회차 등록
    // POST - http://localhost:8080/events/sessions/save/{eventId}
    @PostMapping("/events/sessions/save/{eventId}")
    public String save(@PathVariable Long eventId, EventSessionRequest.SaveDto saveDto, HttpSession session) {
        saveDto.validate();
        SessionCompanyDto sessionCompanyDto =
                (SessionCompanyDto) session.getAttribute(Define.SESSION_COMPANY);
        eventSessionService.save(eventId, sessionCompanyDto.getId(), saveDto);

        return "redirect:/events";
    }

    // 회차 단건 조회
    // GET - http://localhost:8080/event-sessions/{id}
    @GetMapping("/event-sessions/{id}")
    public String detail(@PathVariable Long id, Model model) {
        EventSessionResponse.DetailDto detailDto = eventSessionService.findById(id);
        model.addAttribute("eventSession", detailDto);

        return "event_Session/detail";
    }

    // 행사별 회차 목록 조회
    // GET - http://localhost:8080/events/sessions/list/{eventId}
    @GetMapping("/events/sessions/list/{eventId}")
    public String list(@PathVariable Long eventId, Model model) {
        model.addAttribute("eventSession", eventSessionService.findByEventId(eventId));

        return "event_session/list";
    }

    // 행사별 상태별 회차 조회
    // GET - http://localhost:8080/events/sessions/status/{eventId}
    @GetMapping("/events/sessions/status/{eventId}")
    public String listByStatus(@PathVariable Long eventId, @RequestParam EventSessionStatus status, Model model) {
        model.addAttribute("eventSessions", eventSessionService.findAllByEventIdAndStatus(eventId, status));
        model.addAttribute("status", status);

        return "event_session/list";
    }

    // 회차 수정
    // POST - http://localhost:8080/event-session/update/{id}
    @PostMapping("/event-session/update/{id}")
    public String update(@PathVariable Long id, EventSessionRequest.UpdateDto updateDto, HttpSession session) {
        updateDto.validate();
        SessionCompanyDto sessionCompanyDto = (SessionCompanyDto) session.getAttribute(Define.SESSION_COMPANY);
        eventSessionService.update(id, updateDto, sessionCompanyDto.getId());

        return "redirect:/event-sessions/" + id;
    }

    // 회차 삭제
    // POST - http://localhost:8080/event-sessions/delete/{id}
    @PostMapping("/event-sessions/delete/{id}")
    public String delete(@PathVariable Long id, HttpSession session) {
        SessionCompanyDto sessionCompanyDto = (SessionCompanyDto) session.getAttribute(Define.SESSION_COMPANY);
        eventSessionService.delete(id, sessionCompanyDto.getId());

        return "redirect:/events";
    }

}
