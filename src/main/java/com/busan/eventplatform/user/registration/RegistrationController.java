package com.busan.eventplatform.user.registration;

import com.busan.eventplatform.login.LoginIdentity;
import com.busan.eventplatform.login.LoginSession;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

/**
 * 참가 신청 화면용 컨트롤러 (form 제출 → 처리 → 화면 이동).
 * Fetch(JSON) 호출은 RegistrationApiController 를 사용한다.
 */
@Controller
@RequiredArgsConstructor
public class RegistrationController {

    private final RegistrationService registrationService;

    // 신청 내역(/myhistory) 화면의 '참가 취소' form → 취소 후 신청 내역으로 이동
    @PostMapping("/registrations/cancel/{registrationId}")
    public String cancel(@PathVariable Long registrationId, HttpSession session) {
        LoginIdentity loginIdentity = LoginSession.requireIdentity(session); // 미로그인 → Exception401 → 에러 페이지
        registrationService.cancel(registrationId, loginIdentity);
        return "redirect:/myhistory";
    }

}
