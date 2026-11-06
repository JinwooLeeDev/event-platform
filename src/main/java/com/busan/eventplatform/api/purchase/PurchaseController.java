package com.busan.eventplatform.api.purchase;

import com.busan.eventplatform.login.LoginIdentity;
import com.busan.eventplatform.login.LoginSession;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller     @RequiredArgsConstructor
public class PurchaseController {

    // TODO 이벤트컬럼 작성되면 이벤트 컬럼에 맞게 mustache 설정 + model + session
    @GetMapping("/purchase")
    public String purchaseView(Model model , HttpSession session){
        LoginIdentity loginIdentity = LoginSession.requireIdentity(session);


        return "point/purchase";
    }
}
 