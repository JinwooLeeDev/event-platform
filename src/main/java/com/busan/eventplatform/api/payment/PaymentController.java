package com.busan.eventplatform.api.payment;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PaymentController {

    @GetMapping("/payment")
    public String payment() {
        return "point/payment";
    }

    @GetMapping("/charge")
    public String charge() {
        return "point/charge-point";
    }
}
