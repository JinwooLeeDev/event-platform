package com.busan.eventplatform.api.purchase;


import com.busan.eventplatform.global.error.exception.Exception403;
import com.busan.eventplatform.login.LoginIdentity;
import com.busan.eventplatform.login.LoginSession;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Slf4j
@Controller
@RequiredArgsConstructor
public class DepositController {
    private final DepositService depositService;

    @PostMapping("/event/deposit/{id}")
    public String payDeposit(@PathVariable(name = "id") Long eventId ,
                             @RequestParam(name = "sessionCount") int sessionCount ,
                             HttpSession session , RedirectAttributes redirectAttributes){
        LoginIdentity loginIdentity = LoginSession.requireIdentity(session);
        if(!loginIdentity.isCompany()) throw new Exception403("기업 세션만 이용가능합니다");

        depositService.payDeposit(loginIdentity.getId() , eventId , sessionCount);
        redirectAttributes.addFlashAttribute("successMessage" , "결제가 완료되었습니다. 해당 행사가 최종 확정됩니다.");

        return "redirect:/events";
    }
}
