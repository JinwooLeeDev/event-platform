package com.busan.eventplatform.user.attendance;

import com.busan.eventplatform.login.LoginIdentity;
import com.busan.eventplatform.login.LoginSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttribute;

/**
 * ★ [f/42] 출석 화면 (F04)
 * - GET  /attendance/qr/{sessionId}      주최 기업: 사이트가 만든 출석 QR 표시
 * - GET  /attendance/check/{sessionId}   참가자: QR 인식 후 확인 화면
 * - POST /attendance/check/{sessionId}   참가자: 출석 저장 → 같은 화면에 결과
 */
@Controller
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService attendanceService;

    @GetMapping("/attendance/qr/{sessionId}")
    public String qr(@PathVariable Long sessionId,
                     @SessionAttribute(name = LoginSession.IDENTITY, required = false) LoginIdentity identity,
                     Model model) {
        model.addAttribute("qr", attendanceService.getQrPage(sessionId, identity));   // 주최 기업이 아니면 403
        return "attendance/qr";
    }

    @GetMapping("/attendance/check/{sessionId}")
    public String checkPage(@PathVariable Long sessionId,
                            @RequestParam(defaultValue = "") String code,
                            @SessionAttribute(name = LoginSession.IDENTITY, required = false) LoginIdentity identity,
                            Model model) {
        model.addAttribute("page", attendanceService.getCheckPage(sessionId, code, identity));
        return "attendance/check";
    }

    @PostMapping("/attendance/check/{sessionId}")
    public String check(@PathVariable Long sessionId,
                        @RequestParam(defaultValue = "") String code,
                        @SessionAttribute(name = LoginSession.IDENTITY, required = false) LoginIdentity identity,
                        Model model) {
        model.addAttribute("page", attendanceService.check(sessionId, code, identity));
        return "attendance/check";
    }
}