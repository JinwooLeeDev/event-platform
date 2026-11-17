package com.busan.eventplatform.api.purchase;

public enum PurchaseStatus {
    PURCHASED("결제처리 완료") ,
    REFUNDED("환불"); // 결제 -> 취소 -> 재신청 할때 필요한 구분점.

    private final String displayName;

    private PurchaseStatus(String displayName) {this.displayName = displayName;}
    public String getDisplayName() {return displayName;}
}
