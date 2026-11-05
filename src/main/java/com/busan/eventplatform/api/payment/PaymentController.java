package com.busan.eventplatform.api.payment;

import com.busan.eventplatform.global.util.Define;
import com.busan.eventplatform.login.AuthController;
import com.busan.eventplatform.login.SessionUserDto;
import com.busan.eventplatform.login.UserRepository;
import com.busan.eventplatform.user.User;
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
        // TODO 추후 sessionUser , sessionCompany 세션 구현되면 변경.

        SessionUserDto sessionUserDto = (SessionUserDto) session.getAttribute(AuthController.SESSION_USER);
        User user = paymentService.sessionUser(sessionUserDto.getId());
        model.addAttribute("myPoint" , user.getPoint());

        return "point/charge-point";
    }

    @PostMapping("/api/payment/prepare")
    public ResponseEntity<?> prepare(@RequestBody PaymentReq.PrepareDto prepareDto , HttpSession session) {
        prepareDto.validate();

        // TODO User , Company를 나누는 분기점 필요

        SessionUserDto sessionUserDto = (SessionUserDto) session.getAttribute(AuthController.SESSION_USER);

        PaymentRes.PrepareDto body = paymentPortOneService.prepare(sessionUserDto.getId() , prepareDto.getAmount());

        return ResponseEntity.ok(body);
    }

    @PostMapping("/api/payment/complete")
    public ResponseEntity<?> complete(@RequestBody PaymentReq.CompleteDto completeDto , HttpSession session) {
        completeDto.validate();
        SessionUserDto sessionUserDto = (SessionUserDto) session.getAttribute(AuthController.SESSION_USER);
//        User user = (User) session.getAttribute("sessionUser"); // TODO 추후 sessionUser 구현되면 재설계. user 검증코드도 필요.

        PaymentRes.CompleteDto result = paymentPortOneService.complete(sessionUserDto.getId() , completeDto.getPaymentId());
        int currentPoint = paymentService.currentPoint(sessionUserDto.getId());

        // 포인트 충전시 화면에 실시간으로 충전된 금액까지 포함하여 보여주기위해 "currentPoint"로 전달.
        return ResponseEntity.ok(Map.of("payment" , result , "currentPoint" , currentPoint));
    }

}
