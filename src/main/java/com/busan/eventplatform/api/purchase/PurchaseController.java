package com.busan.eventplatform.api.purchase;

import com.busan.eventplatform.global.error.exception.Exception403;
import com.busan.eventplatform.login.LoginIdentity;
import com.busan.eventplatform.login.LoginSession;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller     @RequiredArgsConstructor
public class PurchaseController {
    private final PurchaseService purchaseService;

    @GetMapping("/event/purchase/{id}")
    public String purchaseView(@PathVariable(name = "id") Long eventSessionId , Model model , HttpSession session){
        LoginIdentity loginIdentity = LoginSession.requireIdentity(session);
        if(!loginIdentity.isUser()) throw new Exception403("유저 전용 결제 페이지입니다");

        PurchaseRes.PurchasePointDto purchasePointDto = purchaseService.calcPointAndEvent(loginIdentity.getId() , eventSessionId);
        model.addAttribute("view" , purchasePointDto);

        return "point/purchase";
    }

    @PostMapping("/event/purchase/{id}")
    public String purchaseAction(@PathVariable(name = "id") Long eventSessionId , HttpSession session , RedirectAttributes redirect) {
        LoginIdentity loginIdentity = LoginSession.requireIdentity(session);
        if(!loginIdentity.isUser()) throw new Exception403("유저 전용 결제 페이지입니다");

        purchaseService.purchase(eventSessionId , loginIdentity.getId());

        redirect.addFlashAttribute("purchaseSuccessMsg",
                "결제완료 되었습니다. 결제 행사확인은 포인트관리 페이지에서 확인 가능합니다");

        return "redirect:/mypage"; // TODO 결제처리 완료후 어떤 페이지로 이동시킬지 추후 결정. 일단 myPage로.
    }

}
