package com.busan.eventplatform.api.purchase;

import com.busan.eventplatform.event.EventSession;
import com.busan.eventplatform.event.events.Event;
import com.busan.eventplatform.global.util.MyDateUtil;
import com.busan.eventplatform.user.Registration;
import com.busan.eventplatform.user.RegistrationStatus;
import lombok.Builder;
import lombok.Data;

import java.sql.Timestamp;

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

    @Data
    public static class PurchaseListDto {
        private String category;
        private String title;
        private String location;
        private String sessionName;
        private String statusMessage;
        private String cancelUrl;
        private String detailUrl;
        private int price;
        private Timestamp eventDate;
        private Timestamp purchaseDate;
        private RegistrationStatus status;
        private boolean canCancel;

        public PurchaseListDto(Purchase purchase , Registration registration){
            EventSession session = purchase.getEventSession();
            Event event = session.getEvent();

            this.category = session.getEvent().getCategory().getName();
            this.title = event.getTitle();
            this.eventDate = Timestamp.from(session.getStartsAt());
            this.sessionName = session.getName();
            this.location = event.getAddress();
            this.price = purchase.getPrice();
            this.purchaseDate = purchase.getPurchasedAt();

            if(registration != null) {
                this.status = registration.getStatus();

                this.statusMessage = switch(status) {
                    case HOLD -> "신청 처리중";
                    case CONFIRMED -> "신청 확정";
                    case WAITING -> "신청 대기";
                    case PENDING_APPROVAL -> "승인 대기";
                    case CANCELLED -> "신청 취소";
                };
                this.canCancel = selectCancel();

                // TODO cancelURL ,detailUrl은 추후.
            }
        }

        // 취소 가능 여부 판단.
        public boolean selectCancel() {
            return  status == RegistrationStatus.CONFIRMED ||
                    status == RegistrationStatus.PENDING_APPROVAL ||
                    status == RegistrationStatus.WAITING;

        }

    }
}