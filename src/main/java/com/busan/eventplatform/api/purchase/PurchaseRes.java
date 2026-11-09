package com.busan.eventplatform.api.purchase;

import lombok.Builder;
import lombok.Data;

public class PurchaseRes {
    @Data
    public static class PurchasePointDto {
        private Long id; // 현재 이벤트회차 id.
        private int currentPoint; // 현재 보유 포인트.
        private int eventPoint; // 행사 결제 포인트.(참여에 필요한 포인트)
        private int predictedPoint;  // 결제 후 예상 포인트.


        @Builder
        public PurchasePointDto(Long id , int currentPoint , int eventPoint) {
            this.id = id;
            this.currentPoint = currentPoint;
            this.eventPoint = eventPoint;
            this.predictedPoint = currentPoint - eventPoint;
        }
    }
}
