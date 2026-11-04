package com.busan.eventplatform.api.payment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

public class PaymentRes {
    // 채널키 , payment_id , store_id
    @Data
    public static class PrepareDto {
        private String channelKey;
        private String paymentId;
        private String storeId;
        private Integer amount;

        @Builder
        public PrepareDto(String channelKey, String paymentId, String storeId, Integer amount) {
            this.channelKey = channelKey;
            this.paymentId = paymentId;
            this.storeId = storeId;
            this.amount = amount;
        }
    }

    @Data
    @AllArgsConstructor
    public static class CompleteDto {
        private int amount;
    }

    @Data
    public static class PortOnePayment {
        private String paymentId;
        private String status;
        private Amount amount;

        @Data
        public static class Amount{
            private Integer total; // Service계층에서 null인지 검사하기 위해 Integer 설정.
        }

    }
}
