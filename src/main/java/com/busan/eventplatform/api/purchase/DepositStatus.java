package com.busan.eventplatform.api.purchase;

public enum DepositStatus {
    READY("결제 대기") ,
    PAID("결제 완료") ,
    RETURN("보증금 반환 완료") ,
    REFUNDED("환불됨");

    private final String displayName;
    private DepositStatus(String displayName) {this.displayName = displayName;}

    public String getDisplayName() {return this.displayName;}
}
