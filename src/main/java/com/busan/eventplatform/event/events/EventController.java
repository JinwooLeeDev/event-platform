
package com.busan.eventplatform.event.events;

import com.busan.eventplatform.event.event_session.EventSessionService;
import com.busan.eventplatform.global.util.Define;
import com.busan.eventplatform.login.companylogin.SessionCompanyDto;
import com.busan.eventplatform.login.personallogin.SessionUserDto;
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
    private final EventSessionService eventSessionService;

    // 행사 목록 조회
    // GET - http://localhost:8080/events/list
    @GetMapping("/events/list")
    public String list(
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @RequestParam(name = "keyword", defaultValue = "") String keyword,
            Model model, HttpSession session) {

        SessionUserDto sessionUserDto =
                (SessionUserDto) session.getAttribute(Define.SESSION_USER);

        Long userId = (sessionUserDto == null) ? null : sessionUserDto.getId();

        EventResponse.PageDto pageDto =
                eventService.findAllPage(page, size, keyword, userId);

        model.addAttribute("eventPage", pageDto);
        model.addAttribute("keyword", keyword);
        model.addAttribute("isSearch", !keyword.isBlank());

        return "event/list";
    }

    // 행사 상세 조회
    // GET - http://localhost:8080/events/1
    @GetMapping("/events/{id}")
    public String detail(@PathVariable Long id, Model model, HttpSession session) {
        SessionCompanyDto sessionCompanyDto =
                (SessionCompanyDto) session.getAttribute(Define.SESSION_COMPANY);

        Long companyId = (sessionCompanyDto == null) ? null : sessionCompanyDto.getId();
        SessionUserDto sessionUserDto =
                (SessionUserDto) session.getAttribute(Define.SESSION_USER);
        Long userId = (sessionUserDto == null) ? null : sessionUserDto.getId();

        EventResponse.DetailDto detailDto =
                eventService.findById(id, companyId, userId);
        model.addAttribute("eventSessions", eventSessionService.findByEventId(id));
        model.addAttribute("event", detailDto);

        return "event/detail";
    }

    // 행사 등록 화면
    // GET - http://localhost:8080/events/save
    @GetMapping("/events/save")
    public String saveForm() {

        return "event/save-form";
    }

    // 행사 등록
    // POST - http://localhost:8080/events/save
    @PostMapping("/events/save")
    public String save(EventRequest.SaveDto saveDto, HttpSession session) {
        saveDto.validate();
        SessionCompanyDto sessionCompanyDto =
                (SessionCompanyDto) session.getAttribute(Define.SESSION_COMPANY);
        eventService.save(saveDto, sessionCompanyDto.getId());

        return "redirect:/events";
    }

    // 기업이 등록한 행사 전체 조회
    // GET - http://localhost:8080/events
    @GetMapping("/events")
    public String findAllByCompanyId(HttpSession session, Model model) {
        SessionCompanyDto sessionCompanyDto =
                (SessionCompanyDto) session.getAttribute(Define.SESSION_COMPANY);
        EventResponse.ListDto response =
                eventService.findAllByCompanyId(sessionCompanyDto.getId());
        model.addAttribute("eventList", response);

        return "company/event-list";
    }

    // 기업이 보완 요청받은 행사 조회
    // GET - http://localhost:8080/events/feedback
    @GetMapping("/events/feedback")
    public String findAllFeedback(HttpSession session, Model model) {
        SessionCompanyDto sessionCompanyDto =
                (SessionCompanyDto) session.getAttribute(Define.SESSION_COMPANY);
        model.addAttribute("eventList",
                eventService.findAllFeedback(sessionCompanyDto.getId()));

        return "company/event-feedback-list";
    }

    // 행사 수정 화면
    // GET - http://localhost:8080/events/update/1
    @GetMapping("/events/update/{id}")
    public String updateForm(@PathVariable Long id, HttpSession session, Model model) {
        SessionCompanyDto sessionCompanyDto =
                (SessionCompanyDto) session.getAttribute(Define.SESSION_COMPANY);
        EventResponse.UpdateFormDto updateFormDto = eventService.update(id, sessionCompanyDto.getId());
        model.addAttribute("event", updateFormDto);

        return "event/update-form";
    }

    // 행사 수정
    // POST - http://localhost:8080/events/update/1
    @PostMapping("/events/update/{id}")
    public String update(@PathVariable Long id, EventRequest.UpdateDto updateDto, HttpSession session) {
        updateDto.validate();
        SessionCompanyDto sessionCompanyDto =
                (SessionCompanyDto) session.getAttribute(Define.SESSION_COMPANY);
        eventService.update(id, sessionCompanyDto.getId(), updateDto);

        return "redirect:/events";
    }

    // 보완 요청된 행사 재제출
    // POST - http://localhost:8080/events/resubmit/1
    @PostMapping("/events/resubmit/{id}")
    public String resubmit(@PathVariable Long id, HttpSession session) {
        SessionCompanyDto sessionCompanyDto =
                (SessionCompanyDto) session.getAttribute(Define.SESSION_COMPANY);
        eventService.resubmit(id, sessionCompanyDto.getId());

        return "redirect:/events";
    }

    // 기업 행사 삭제
    // POST - http://localhost:8080/events/delete/1
    @PostMapping("/events/delete/{id}")
    public String delete(@PathVariable Long id, HttpSession session) {
        SessionCompanyDto sessionCompanyDto =
                (SessionCompanyDto) session.getAttribute(Define.SESSION_COMPANY);

        eventService.delete(id, sessionCompanyDto.getId());

        return "redirect:/events";
    }

    // 관리자 행사 승인
    // POST - http://localhost:8080/events/approve/1
    @PostMapping("/events/approve/{id}")
    public String approve(@PathVariable Long id) {
        eventService.approve(id);

        return "redirect:/events";
    }

    // 관리자 보완 요청
    // POST - http://localhost:8080/events/feedback/1
    @PostMapping("/events/feedback/{id}")
    public String requestFeedback(
            @PathVariable Long id,
            @RequestParam String feedbackMessage) {
        eventService.requestFeedback(id, feedbackMessage);

        return "redirect:/events";
    }

    // 관리자 행사 취소
    // POST - http://localhost:8080/events/cancel/1
    @PostMapping("/events/cancel/{id}")
    public String cancel(@PathVariable Long id, EventRequest.CancelDto cancelDto) {
        cancelDto.validate();
        eventService.cancel(id, cancelDto);

        return "redirect:/events";
    }
}
