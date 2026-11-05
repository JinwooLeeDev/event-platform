package com.busan.eventplatform.api.payment;

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

@Controller     @RequiredArgsConstructor        @Slf4j
public class PaymentController {
    private final PaymentPortOneService paymentPortOneService;

    @GetMapping("/charge")
    public String charge(Model model , HttpSession session) {
        // TODO 추후 sessionUser , sessionCompany 세션 구현되면 변경.
//        User user;  // 포인트 충전할때 user인지 company인지 구분 필요.
//        Company company;
//
//        user = (User)session.getAttribute("sessionUser");
//        if(user == null) company = (Company) session.getAttribute("sessionCompany");
//        else throw new Exception403("로그인 먼저 해주세요");
//            paymentService.
//        model.addAttribute("myPoint");

        return "point/charge-point";
    }

    @PostMapping("/api/payment/prepare")
    public ResponseEntity<?> prepare(@RequestBody PaymentReq.PrepareDto prepareDto , HttpSession session) {
        prepareDto.validate();
//        User user = (User) session.getAttribute("sessionUser"); // TODO 추후 sessionUser 구현되면 재설계.

        // TODO 1L : 추후 session 생성시 user.getId()로 변경.
        PaymentRes.PrepareDto body = paymentPortOneService.prepare(1L , prepareDto.getAmount());

        return ResponseEntity.ok(body);
    }

    @PostMapping("/api/payment/complete")
    public ResponseEntity<?> complete(@RequestBody PaymentReq.CompleteDto completeDto , HttpSession session) {
        completeDto.validate();
//        User user = (User) session.getAttribute("sessionUser"); // TODO 추후 sessionUser 구현되면 재설계. user 검증코드도 필요.

        // TODO 1L : 추후 session 생성시 user.getId()로 변경.
        return ResponseEntity.ok(paymentPortOneService.complete(1L , completeDto.getPaymentId()));
    }


}
