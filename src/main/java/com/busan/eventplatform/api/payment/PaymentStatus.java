package com.busan.eventplatform.api.payment;

public enum PaymentStatus {
    READY("결제 대기") ,
    PAID("결제 완료") ,
    CANCELED("결제 취소");

    private final String displayName;

    private PaymentStatus(String displayName) {this.displayName = displayName;}

    public String getDisplayName() {return this.displayName;}
}
