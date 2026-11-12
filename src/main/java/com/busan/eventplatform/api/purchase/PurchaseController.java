package com.busan.eventplatform.api.purchase;

import com.busan.eventplatform.login.LoginIdentity;
import com.busan.eventplatform.login.LoginSession;
import com.busan.eventplatform.user.registration.RegistrationPurchaseService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@Controller     @RequiredArgsConstructor
public class PurchaseController {
    private final PurchaseService purchaseService;
    private final RegistrationPurchaseService registrationPurchaseService;

    // TODO 이벤트컬럼 작성되면 이벤트 컬럼에 맞게 mustache 설정("event/{id}/purchase") + model + session
    @GetMapping("/event/purchase/{id}")
    public String purchaseView(@PathVariable(name = "id") Long eventId , Model model , HttpSession session){
        LoginIdentity loginIdentity = LoginSession.requireIdentity(session);

        PurchaseRes.PurchasePointDto purchasePointDto = purchaseService.calcPoint(loginIdentity , eventId);
        model.addAttribute("viewPoint" , purchasePointDto);

        return "point/purchase";
    }

    /**
     * [결제하기] 버튼 — 참가 신청 + 포인트 결제를 한 번에 처리
     * 검증 → 포인트 결제 → 신청 저장을 하나의 트랜잭션으로 처리 (실패 시 전체 롤백)
     * ※ 여기서 purchaseService 를 직접 부르면 결제만 되고 신청이 생기지 않으므로 부르지 않는다
     */
    @PostMapping("/event/purchase/{id}")
    public String purchaseAction(@PathVariable(name = "id") Long eventId , HttpSession session) {
        LoginIdentity loginIdentity = LoginSession.requireIdentity(session);

       //[도현] 변경, 사유 위와 같음
        //[주의] eventId 는 이름과 달리 회차(EventSession) id 다 — applyWithPayment 의 sessionId 로 전달
        registrationPurchaseService.applyWithPayment(eventId, loginIdentity);

        return "redirect:/myhistory"; // TODO -[의논 필요] 신청 결과(확정/대기)는 내 신청 내역에서 확인
    }

}
