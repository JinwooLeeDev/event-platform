package com.busan.eventplatform.event.eventCancel;

import com.busan.eventplatform.global.i18n.LocaleMessages;

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

    private final com.busan.eventplatform.event.eventCancel.EventCancelService eventCancelService;

    // GET - http://localhost:8080/event-cancels/save/1
    @GetMapping("/event-cancels/save/{sessionId}")
    public String form(@PathVariable Long sessionId, HttpSession session, Model model) {
        model.addAllAttributes(eventCancelService.context(companyId(session), sessionId));
        model.addAttribute("sessionId", sessionId);

        return "event_cancel/save-form";
    }

    // 회차 취소 신청
    // POST - http://localhost:8080/event-cancels/save/1
    @PostMapping("/event-cancels/save/{sessionId}")
    public String save(@PathVariable Long sessionId,
                       EventCancelRequest.SaveDto saveDto,
                       HttpSession session,
                       org.springframework.web.servlet.mvc.support.RedirectAttributes redirect) {

        Long companyId = companyId(session);

        // 취소 신청서 저장
        eventCancelService.save(
                companyId,
                sessionId,
                saveDto.getReason()
        );

        redirect.addFlashAttribute("successMessage", LocaleMessages.display("취소 신청이 접수되었습니다. 운영자 승인 후 회차가 취소됩니다."));
        return "redirect:/event-sessions/" + sessionId;
    }

    public static Long companyId(HttpSession session) {
        var identity = com.busan.eventplatform.login.LoginSession.requireIdentity(session);
        if (identity.getLoginType() != com.busan.eventplatform.login.LoginType.COMPANY) {
            throw new com.busan.eventplatform.global.error.exception.Exception403("기업 계정으로 이용해주세요.");
        }
        Object value = session.getAttribute(Define.SESSION_COMPANY);
        if (!(value instanceof SessionCompanyDto company) || !java.util.Objects.equals(identity.getId(), company.getId())) {
            throw new com.busan.eventplatform.global.error.exception.Exception401("기업 계정으로 다시 로그인해주세요.");
        }
        return company.getId();
    }
}
