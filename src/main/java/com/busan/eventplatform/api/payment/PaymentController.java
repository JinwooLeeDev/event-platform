package com.busan.eventplatform.api.payment;

import com.busan.eventplatform.login.LoginIdentity;
import com.busan.eventplatform.login.LoginSession;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

@Controller     @RequiredArgsConstructor        @Slf4j
public class PaymentController {
    private final PaymentPortOneService paymentPortOneService;
    private final PaymentService paymentService;

    @GetMapping("/charge")
    public String charge(Model model , HttpSession session) {
        // loginIdentity에 "IDENTITY" 키에 들어간 loginType(User인지 Company인지)를 보고 구분 가능.
        LoginIdentity loginIdentity = LoginSession.requireIdentity(session);

        int currentPoint = paymentService.currentPoint(loginIdentity);

        model.addAttribute("myPoint" , currentPoint);

        return "point/charge-point";
    }

    @PostMapping("/api/payment/prepare")
    public ResponseEntity<?> prepare(@RequestBody PaymentReq.PrepareDto prepareDto , HttpSession session) {
        prepareDto.validate();

        LoginIdentity loginIdentity = LoginSession.requireIdentity(session);

        PaymentRes.PrepareDto body = paymentPortOneService.prepare(loginIdentity , prepareDto.getAmount());

        return ResponseEntity.ok(body);
    }

    @PostMapping("/api/payment/complete")
    public ResponseEntity<?> complete(@RequestBody PaymentReq.CompleteDto completeDto , HttpSession session) {
        completeDto.validate();

        LoginIdentity loginIdentity = LoginSession.requireIdentity(session);

        PaymentRes.CompleteDto result = paymentPortOneService.complete(loginIdentity , completeDto.getPaymentId());
        int currentPoint = paymentService.currentPoint(loginIdentity);

        // 포인트 충전시 화면에 실시간으로 충전된 금액까지 포함하여 보여주기위해 "currentPoint"로 전달.
        return ResponseEntity.ok(Map.of("payment" , result , "currentPoint" , currentPoint));
    }

}
