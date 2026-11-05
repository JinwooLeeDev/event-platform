package com.busan.eventplatform.api.payment;

import com.busan.eventplatform.global.error.exception.Exception400;
import lombok.Data;

public class PaymentReq {
    @Data
    public static class PrepareDto {
        private int amount;

        public void validate() {
            if(amount <= 0) throw new Exception400("충전금액은 0포인트 이상이어야합니다");
        }
    }

    @Data
    public static class CompleteDto {
        private String paymentId;
        public void validate() {
//            if(paymentId == 0) throw new Exception400("충전금액은 0포인트 이상이어야합니다");
        }
    }
}
